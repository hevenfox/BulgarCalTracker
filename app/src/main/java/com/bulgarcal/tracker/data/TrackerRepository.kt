package com.bulgarcal.tracker.data

import com.bulgarcal.tracker.domain.CalculationEngine
import com.bulgarcal.tracker.domain.MealType
import com.bulgarcal.tracker.domain.SearchKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class TrackerRepository(private val db: AppDatabase) {

    companion object {
        /** Marks rows the user created themselves (bundled rows carry off/usda/curated). */
        const val USER_SOURCE = "user"

        /** A product pulled from Open Food Facts by a barcode scan. */
        const val SCANNED_SOURCE = "off"
    }

    val profile: Flow<UserProfileEntity?> = db.userProfileDao().observe()
    val weightEntries: Flow<List<WeightEntryEntity>> = db.weightEntryDao().all()

    fun mealLogs(day: LocalDate): Flow<List<MealLogEntity>> =
        db.mealLogDao().forDay(day.toEpochDay())

    fun dayTotals(day: LocalDate): Flow<DayTotalsRow> =
        db.mealLogDao().dayTotals(day.toEpochDay())

    fun mealCalories(day: LocalDate): Flow<Map<String, Int>> =
        db.mealLogDao().mealCalories(day.toEpochDay()).map { rows ->
            rows.associate { it.mealType to it.total }
        }

    fun foodsByCategory(category: String, query: String): Flow<List<FoodEntity>> =
        if (category == "Моите Рецепти") db.foodDao().recipes()
        else db.foodDao().byCategory(category, query)

    /** Search in either keyboard layout: "musaka" and "Мусака" both work. */
    fun searchFoods(query: String): Flow<List<FoodEntity>> {
        val trimmed = query.trim()
        return db.foodDao().search(SearchKey.ofQuery(trimmed), trimmed.lowercase())
    }

    fun favoriteFoods(): Flow<List<FoodEntity>> = db.foodDao().favorites()

    suspend fun getFood(id: Long) = db.foodDao().getById(id)

    suspend fun setFavorite(foodId: Long, favorite: Boolean) =
        db.foodDao().setFavorite(foodId, favorite)

    suspend fun saveProfile(entity: UserProfileEntity, logWeight: Boolean = true) {
        db.userProfileDao().upsert(entity)
        if (logWeight) {
            db.weightEntryDao().insert(
                WeightEntryEntity(
                    dateEpochDay = LocalDate.now().toEpochDay(),
                    weightKg = entity.weightKg
                )
            )
        }
    }

    suspend fun logFood(
        food: FoodEntity,
        mealType: MealType,
        grams: Float,
        date: LocalDate = LocalDate.now()
    ) {
        val factor = grams / food.servingGrams.coerceAtLeast(1f)
        db.mealLogDao().insert(
            MealLogEntity(
                foodId = food.id,
                foodName = food.name,
                mealType = mealType.name,
                dateEpochDay = date.toEpochDay(),
                grams = grams,
                calories = (food.calories * factor).toInt(),
                carbs = food.carbs * factor,
                protein = food.protein * factor,
                fats = food.fats * factor,
                saturatedFats = food.saturatedFats * factor,
                sugars = food.sugars * factor,
                fiber = food.fiber * factor,
                glycemicIndex = food.glycemicIndex,
                salt = food.salt * factor,
                monoFats = food.monoFats * factor,
                polyFats = food.polyFats * factor,
                transFats = food.transFats * factor,
                cholesterol = food.cholesterol * factor,
                potassium = food.potassium * factor,
                water = food.water * factor,
                glycemicLoad = food.glycemicLoad * factor
            )
        )
    }

    suspend fun updateLog(
        log: MealLogEntity,
        mealType: MealType,
        grams: Float,
        date: LocalDate
    ) {
        val food = db.foodDao().getById(log.foodId) ?: return
        val factor = grams / food.servingGrams.coerceAtLeast(1f)
        val updated = log.copy(
            mealType = mealType.name,
            dateEpochDay = date.toEpochDay(),
            grams = grams,
            calories = (food.calories * factor).toInt(),
            carbs = food.carbs * factor,
            protein = food.protein * factor,
            fats = food.fats * factor,
            saturatedFats = food.saturatedFats * factor,
            sugars = food.sugars * factor,
            fiber = food.fiber * factor,
            glycemicIndex = food.glycemicIndex,
            salt = food.salt * factor,
            monoFats = food.monoFats * factor,
            polyFats = food.polyFats * factor,
            transFats = food.transFats * factor,
            cholesterol = food.cholesterol * factor,
            potassium = food.potassium * factor,
            water = food.water * factor,
            glycemicLoad = food.glycemicLoad * factor
        )
        db.mealLogDao().update(updated)
    }

    suspend fun deleteLog(id: Long) = db.mealLogDao().delete(id)

    suspend fun saveCustomProduct(food: FoodEntity): FoodEntity =
        upsertWithBarcode(
            food.copy(
                isCustom = true,
                isRecipe = false,
                searchKey = SearchKey.of(food.name),
                source = USER_SOURCE
            )
        )

    /**
     * A product resolved from Open Food Facts. It is stored like any other food so
     * that it can be found by name later (no barcode needed), is answered offline on
     * the next scan, and survives a reseed of the bundled database (`isCustom`).
     */
    suspend fun saveScannedProduct(food: FoodEntity): FoodEntity =
        upsertWithBarcode(
            food.copy(
                isCustom = true,
                isRecipe = false,
                searchKey = SearchKey.of(food.name),
                source = food.source.ifBlank { SCANNED_SOURCE }
            )
        )

    suspend fun foodByBarcode(barcode: String): FoodEntity? =
        if (barcode.isBlank()) null else db.foodDao().byBarcode(barcode)

    /**
     * One barcode must never point at two foods, otherwise a scan would be a coin
     * flip. Re-saving a known barcode updates that row in place instead.
     */
    private suspend fun upsertWithBarcode(food: FoodEntity): FoodEntity {
        val existing = foodByBarcode(food.barcode)
        val toSave = if (existing == null) {
            food
        } else {
            food.copy(id = existing.id, isFavorite = existing.isFavorite || food.isFavorite)
        }
        val id = db.foodDao().insert(toSave)
        return toSave.copy(id = if (id > 0) id else toSave.id)
    }

    suspend fun saveRecipe(
        food: FoodEntity,
        ingredients: List<RecipeIngredientEntity>
    ): Long {
        val id = db.foodDao().insert(
            food.copy(
                isCustom = true,
                isRecipe = true,
                category = "Моите Рецепти",
                searchKey = SearchKey.of(food.name),
                source = USER_SOURCE
            )
        )
        db.recipeIngredientDao().insertAll(
            ingredients.map { it.copy(recipeFoodId = id) }
        )
        return id
    }

    suspend fun addWeight(weightKg: Float, date: LocalDate = LocalDate.now()) {
        db.weightEntryDao().insert(
            WeightEntryEntity(dateEpochDay = date.toEpochDay(), weightKg = weightKg)
        )
    }

    fun waterIntake(day: LocalDate): Flow<Int> =
        db.waterLogDao().totalForDay(day.toEpochDay())

    fun waterLogs(day: LocalDate): Flow<List<WaterLogEntity>> =
        db.waterLogDao().forDay(day.toEpochDay())

    suspend fun addWater(ml: Int, date: LocalDate = LocalDate.now()) {
        db.waterLogDao().insert(
            WaterLogEntity(
                dateEpochDay = date.toEpochDay(),
                milliliters = ml
            )
        )
    }

    suspend fun deleteWater(id: Long) = db.waterLogDao().delete(id)

    suspend fun removeLastWater(date: LocalDate) =
        db.waterLogDao().deleteLastForDay(date.toEpochDay())

    suspend fun dayTotalsInRange(start: LocalDate, end: LocalDate): List<DayCalorieRow> =
        db.mealLogDao().dayTotalsInRange(start.toEpochDay(), end.toEpochDay())

    suspend fun waterDailyInRange(start: LocalDate, end: LocalDate): List<WaterDayRow> =
        db.waterLogDao().dailyTotalsInRange(start.toEpochDay(), end.toEpochDay())

    suspend fun streakCalorieRows(through: LocalDate, lookbackDays: Long = 400): List<DayCalorieRow> {
        val start = through.minusDays(lookbackDays)
        return dayTotalsInRange(start, through)
    }

    /** Manual override from app_meta; null means "derive the goal from body weight". */
    suspend fun getWaterGoalOverrideMl(): Int? =
        db.appMetaDao().get("water_goal_ml")?.toIntOrNull()?.takeIf { it > 0 }

    suspend fun getDisplayName(): String =
        db.appMetaDao().get("display_name")?.takeIf { it.isNotBlank() } ?: "Потребител"

    suspend fun setDisplayName(name: String) {
        db.appMetaDao().set(AppMetaEntity(key = "display_name", value = name.trim()))
    }

    suspend fun getWaterIncrement(): Int {
        val raw = db.appMetaDao().get("water_increment_ml") ?: return 250
        return raw.toIntOrNull() ?: 250
    }

    suspend fun setWaterIncrement(ml: Int) {
        db.appMetaDao().set(AppMetaEntity(key = "water_increment_ml", value = ml.toString()))
    }

    suspend fun updateMacroTargets(
        calories: Int,
        carbsPct: Int,
        proteinPct: Int,
        fatPct: Int,
        overridden: Boolean
    ) {
        val current = db.userProfileDao().get() ?: return
        val macros = CalculationEngine.macroTargetsFromPercents(calories, carbsPct, proteinPct, fatPct)
        db.userProfileDao().upsert(
            current.copy(
                dailyCalories = calories,
                carbsPercent = carbsPct,
                proteinPercent = proteinPct,
                fatPercent = fatPct,
                carbsGrams = macros.carbsGrams,
                proteinGrams = macros.proteinGrams,
                fatGrams = macros.fatGrams,
                caloriesOverridden = overridden
            )
        )
    }
}

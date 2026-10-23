package com.bulgarcal.tracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT COUNT(*) FROM foods WHERE isCustom = 0")
    suspend fun bundledCount(): Int

    @Query("DELETE FROM foods WHERE isCustom = 0")
    suspend fun deleteBundled()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(foods: List<FoodEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(food: FoodEntity): Long

    @Query("SELECT * FROM foods WHERE category = :category AND name LIKE '%' || :query || '%' ORDER BY name")
    fun byCategory(category: String, query: String = ""): Flow<List<FoodEntity>>

    /**
     * [key] is the query transliterated to Latin (see SearchKey), [raw] is what the
     * user typed. Matching the key makes "musaka" find "Мусака" and keeps Cyrillic
     * input working in either case; the raw fallback still finds custom foods
     * created before the search key existed. Shortest names first, so an exact-ish
     * product beats a long one containing the same words.
     */
    @Query(
        "SELECT * FROM foods WHERE searchKey LIKE '%' || :key || '%' " +
            "OR LOWER(name) LIKE '%' || :raw || '%' " +
            "ORDER BY LENGTH(name), name LIMIT 100"
    )
    fun search(key: String, raw: String): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE searchKey = ''")
    suspend fun rowsWithoutSearchKey(): List<FoodEntity>

    @Update
    suspend fun updateAll(foods: List<FoodEntity>)

    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun getById(id: Long): FoodEntity?

    /** Barcode lookup for the scanner; the index on `barcode` serves this query. */
    @Query("SELECT * FROM foods WHERE barcode = :code LIMIT 1")
    suspend fun byBarcode(code: String): FoodEntity?

    @Query("SELECT * FROM foods WHERE isCustom = 1 AND isRecipe = 0 ORDER BY name")
    fun customProducts(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE isRecipe = 1 ORDER BY name")
    fun recipes(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE isFavorite = 1 ORDER BY name")
    fun favorites(): Flow<List<FoodEntity>>

    @Query("UPDATE foods SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Update
    suspend fun update(food: FoodEntity)
}

@Dao
interface MealLogDao {
    @Insert
    suspend fun insert(log: MealLogEntity): Long

    @Update
    suspend fun update(log: MealLogEntity)

    @Query("DELETE FROM meal_logs WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM meal_logs WHERE dateEpochDay = :day ORDER BY id DESC")
    fun forDay(day: Long): Flow<List<MealLogEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(calories),0) as calories,
               COALESCE(SUM(carbs),0) as carbs,
               COALESCE(SUM(protein),0) as protein,
               COALESCE(SUM(fats),0) as fats,
               COALESCE(SUM(saturatedFats),0) as saturatedFats,
               COALESCE(SUM(sugars),0) as sugars,
               COALESCE(SUM(fiber),0) as fiber,
               COALESCE(SUM(salt),0) as salt,
               COALESCE(SUM(monoFats),0) as monoFats,
               COALESCE(SUM(polyFats),0) as polyFats,
               COALESCE(SUM(transFats),0) as transFats,
               COALESCE(SUM(cholesterol),0) as cholesterol,
               COALESCE(SUM(potassium),0) as potassium,
               COALESCE(SUM(water),0) as water,
               COALESCE(SUM(glycemicLoad),0) as glycemicLoad,
               COALESCE(SUM(glycemicIndex * carbs),0) as glycemicIndexWeighted
        FROM meal_logs WHERE dateEpochDay = :day
        """
    )
    fun dayTotals(day: Long): Flow<DayTotalsRow>

    @Query(
        """
        SELECT mealType, COALESCE(SUM(calories),0) as total
        FROM meal_logs WHERE dateEpochDay = :day GROUP BY mealType
        """
    )
    fun mealCalories(day: Long): Flow<List<MealCalorieRow>>

    @Query(
        """
        SELECT dateEpochDay as dateEpochDay,
               COALESCE(SUM(calories),0) as calories,
               COALESCE(SUM(carbs),0) as carbs,
               COALESCE(SUM(protein),0) as protein,
               COALESCE(SUM(fats),0) as fats
        FROM meal_logs
        WHERE dateEpochDay BETWEEN :startDay AND :endDay
        GROUP BY dateEpochDay
        ORDER BY dateEpochDay ASC
        """
    )
    suspend fun dayTotalsInRange(startDay: Long, endDay: Long): List<DayCalorieRow>
}

data class DayTotalsRow(
    val calories: Int,
    val carbs: Float,
    val protein: Float,
    val fats: Float,
    val saturatedFats: Float,
    val sugars: Float,
    val fiber: Float,
    val salt: Float,
    val monoFats: Float = 0f,
    val polyFats: Float = 0f,
    val transFats: Float = 0f,
    val cholesterol: Float = 0f,
    val potassium: Float = 0f,
    val water: Float = 0f,
    val glycemicLoad: Float = 0f,
    /** SUM(GI * carbs); divided by the day's carbs for the average GI. */
    val glycemicIndexWeighted: Float = 0f
) {
    /** Carb-weighted average glycemic index for the day, 0 when unknown. */
    val averageGlycemicIndex: Float
        get() = if (carbs > 0f) glycemicIndexWeighted / carbs else 0f

    /** Net carbs: total carbs minus fiber. */
    val netCarbs: Float get() = (carbs - fiber).coerceAtLeast(0f)
}

data class MealCalorieRow(val mealType: String, val total: Int)

data class DayCalorieRow(
    val dateEpochDay: Long,
    val calories: Int,
    val carbs: Float,
    val protein: Float,
    val fats: Float
)

data class WaterDayRow(val dateEpochDay: Long, val totalMl: Int)

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun observe(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun get(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)
}

@Dao
interface WeightEntryDao {
    @Insert
    suspend fun insert(entry: WeightEntryEntity)

    @Query("SELECT * FROM weight_entries ORDER BY dateEpochDay ASC, id ASC")
    fun all(): Flow<List<WeightEntryEntity>>
}

@Dao
interface RecipeIngredientDao {
    @Insert
    suspend fun insertAll(items: List<RecipeIngredientEntity>)

    @Query("SELECT * FROM recipe_ingredients WHERE recipeFoodId = :recipeId")
    suspend fun forRecipe(recipeId: Long): List<RecipeIngredientEntity>
}

@Dao
interface AppMetaDao {
    @Query("SELECT value FROM app_meta WHERE key = :key")
    suspend fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(meta: AppMetaEntity)
}

@Dao
interface WaterLogDao {
    @Insert
    suspend fun insert(entry: WaterLogEntity)

    @Query("DELETE FROM water_logs WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM water_logs WHERE dateEpochDay = :day ORDER BY id ASC")
    fun forDay(day: Long): Flow<List<WaterLogEntity>>

    @Query("SELECT COALESCE(SUM(milliliters),0) FROM water_logs WHERE dateEpochDay = :day")
    fun totalForDay(day: Long): Flow<Int>

    @Query("DELETE FROM water_logs WHERE id = (SELECT MAX(id) FROM water_logs WHERE dateEpochDay = :day)")
    suspend fun deleteLastForDay(day: Long)

    @Query(
        """
        SELECT dateEpochDay as dateEpochDay, COALESCE(SUM(milliliters),0) as totalMl
        FROM water_logs
        WHERE dateEpochDay BETWEEN :startDay AND :endDay
        GROUP BY dateEpochDay
        ORDER BY dateEpochDay ASC
        """
    )
    suspend fun dailyTotalsInRange(startDay: Long, endDay: Long): List<WaterDayRow>
}

package com.bulgarcal.tracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bulgarcal.tracker.data.ActivityLevel
import com.bulgarcal.tracker.data.AppDatabase
import com.bulgarcal.tracker.data.BarcodeLookup
import com.bulgarcal.tracker.data.DayCalorieRow
import com.bulgarcal.tracker.data.DayTotalsRow
import com.bulgarcal.tracker.data.WaterDayRow
import com.bulgarcal.tracker.data.FoodEntity
import com.bulgarcal.tracker.data.FoodSeeder
import com.bulgarcal.tracker.data.GoalType
import com.bulgarcal.tracker.data.MealLogEntity
import com.bulgarcal.tracker.data.WaterLogEntity
import com.bulgarcal.tracker.data.PaceOption
import com.bulgarcal.tracker.data.RecipeIngredientEntity
import com.bulgarcal.tracker.data.ScanResult
import com.bulgarcal.tracker.data.Sex
import com.bulgarcal.tracker.data.TrackerRepository
import com.bulgarcal.tracker.data.UserProfileEntity
import com.bulgarcal.tracker.data.WeightEntryEntity
import com.bulgarcal.tracker.domain.CalculationEngine
import com.bulgarcal.tracker.domain.MealType
import com.bulgarcal.tracker.domain.ProgressAnalytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TrackerViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val repo = TrackerRepository(db)
    private val barcodeLookup = BarcodeLookup(repo)

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    val profile = repo.profile.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val mealLogs: StateFlow<List<MealLogEntity>> = _selectedDate
        .flatMapLatest { repo.mealLogs(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dayTotals: StateFlow<DayTotalsRow> = _selectedDate
        .flatMapLatest { repo.dayTotals(it) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            DayTotalsRow(0, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        )

    val mealCalories: StateFlow<Map<String, Int>> = _selectedDate
        .flatMapLatest { repo.mealCalories(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val waterLogs: StateFlow<List<WaterLogEntity>> = _selectedDate
        .flatMapLatest { repo.waterLogs(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _waterIncrement = MutableStateFlow(250)
    val waterIncrement: StateFlow<Int> = _waterIncrement.asStateFlow()

    private val _pendingMeal = MutableStateFlow<MealType?>(null)
    val pendingMeal: StateFlow<MealType?> = _pendingMeal.asStateFlow()

    /** False until the bundled food database finished loading on first launch. */
    private val _foodDbReady = MutableStateFlow(false)
    val foodDbReady: StateFlow<Boolean> = _foodDbReady.asStateFlow()

    val weightEntries: StateFlow<List<WeightEntryEntity>> =
        repo.weightEntries.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteFoods: StateFlow<List<FoodEntity>> =
        repo.favoriteFoods().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _waterGoalOverrideMl = MutableStateFlow<Int?>(null)

    /** Manual override (app_meta) if set, otherwise 35 ml/kg of the latest weight, default 2300. */
    val waterGoalMl: StateFlow<Int> =
        combine(profile, weightEntries, _waterGoalOverrideMl) { p, entries, manualGoal ->
            val kg = entries.lastOrNull()?.weightKg ?: p?.weightKg
            manualGoal ?: kg?.let { CalculationEngine.waterGoalMl(it) } ?: 2300
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2300)

    private val _loggingStreak = MutableStateFlow(0)
    val loggingStreak: StateFlow<Int> = _loggingStreak.asStateFlow()

    private val _displayName = MutableStateFlow("Потребител")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    init {
        viewModelScope.launch {
            FoodSeeder.seedIfNeeded(app, db)
            _foodDbReady.value = true
        }
        viewModelScope.launch {
            _waterIncrement.value = repo.getWaterIncrement()
            refreshMetaAndStreak()
        }
    }

    private fun refreshMetaAndStreak() {
        viewModelScope.launch {
            _waterGoalOverrideMl.value = repo.getWaterGoalOverrideMl()
            _displayName.value = repo.getDisplayName()
            val rows = repo.streakCalorieRows(LocalDate.now())
            _loggingStreak.value = ProgressAnalytics.loggingStreak(rows, LocalDate.now())
        }
    }

    fun setDate(date: LocalDate) { _selectedDate.value = date }
    fun shiftDate(days: Long) { _selectedDate.value = _selectedDate.value.plusDays(days) }

    fun completeOnboarding(
        goal: GoalType,
        sex: Sex,
        birthDate: LocalDate,
        heightCm: Float,
        weightKg: Float,
        targetKg: Float,
        activity: ActivityLevel,
        pace: PaceOption
    ) {
        val age = Period.between(birthDate, LocalDate.now()).years.coerceAtLeast(1)
        val bmr = CalculationEngine.calculateBmr(sex, weightKg, heightCm, age)
        val tdee = CalculationEngine.calculateTdee(bmr, activity)
        val calories = CalculationEngine.dailyCalorieTarget(tdee, goal, pace)
        val macros = CalculationEngine.defaultMacroTargets(calories)
        val entity = UserProfileEntity(
            onboardingComplete = true,
            goal = goal.name,
            sex = sex.name,
            birthDateEpochDay = birthDate.toEpochDay(),
            heightCm = heightCm,
            weightKg = weightKg,
            targetWeightKg = targetKg,
            activityLevel = activity.name,
            pace = pace.name,
            dailyCalories = macros.calories,
            carbsPercent = macros.carbsPercent,
            proteinPercent = macros.proteinPercent,
            fatPercent = macros.fatPercent,
            carbsGrams = macros.carbsGrams,
            proteinGrams = macros.proteinGrams,
            fatGrams = macros.fatGrams
        )
        viewModelScope.launch { repo.saveProfile(entity) }
    }

    fun updateProfile(
        sex: Sex,
        heightCm: Float,
        weightKg: Float,
        targetKg: Float,
        activity: ActivityLevel,
        goal: GoalType,
        pace: PaceOption,
        birthDate: LocalDate
    ) {
        viewModelScope.launch {
            val current = db.userProfileDao().get() ?: return@launch
            val age = Period.between(birthDate, LocalDate.now()).years.coerceAtLeast(1)
            val bmr = CalculationEngine.calculateBmr(sex, weightKg, heightCm, age)
            val tdee = CalculationEngine.calculateTdee(bmr, activity)
            val calories = if (current.caloriesOverridden) {
                current.dailyCalories
            } else {
                CalculationEngine.dailyCalorieTarget(tdee, goal, pace)
            }
            val macros = if (current.caloriesOverridden) {
                CalculationEngine.macroTargetsFromPercents(
                    calories, current.carbsPercent, current.proteinPercent, current.fatPercent
                )
            } else {
                CalculationEngine.defaultMacroTargets(calories)
            }
            repo.saveProfile(
                current.copy(
                    sex = sex.name,
                    birthDateEpochDay = birthDate.toEpochDay(),
                    heightCm = heightCm,
                    weightKg = weightKg,
                    targetWeightKg = targetKg,
                    activityLevel = activity.name,
                    pace = pace.name,
                    goal = goal.name,
                    dailyCalories = macros.calories,
                    carbsPercent = macros.carbsPercent,
                    proteinPercent = macros.proteinPercent,
                    fatPercent = macros.fatPercent,
                    carbsGrams = macros.carbsGrams,
                    proteinGrams = macros.proteinGrams,
                    fatGrams = macros.fatGrams
                ),
                // only add a chart point when the weight actually changed
                logWeight = weightKg != current.weightKg
            )
        }
    }

    fun logFood(food: FoodEntity, meal: MealType, grams: Float, date: LocalDate? = null) {
        viewModelScope.launch {
            repo.logFood(food, meal, grams, date ?: _selectedDate.value)
            refreshMetaAndStreak()
        }
    }

    fun setFavorite(foodId: Long, favorite: Boolean) {
        viewModelScope.launch { repo.setFavorite(foodId, favorite) }
    }

    fun deleteLog(id: Long) {
        viewModelScope.launch {
            repo.deleteLog(id)
            refreshMetaAndStreak()
        }
    }

    fun updateLog(log: MealLogEntity, grams: Float, meal: MealType, date: LocalDate) {
        viewModelScope.launch {
            repo.updateLog(log, meal, grams, date)
            refreshMetaAndStreak()
        }
    }

    fun addQuickWater() {
        viewModelScope.launch {
            repo.addWater(_waterIncrement.value, _selectedDate.value)
        }
    }

    fun removeLastWater() {
        viewModelScope.launch { repo.removeLastWater(_selectedDate.value) }
    }

    fun deleteWater(id: Long) {
        viewModelScope.launch { repo.deleteWater(id) }
    }

    fun addWeight(weightKg: Float, date: LocalDate = _selectedDate.value) {
        viewModelScope.launch { repo.addWeight(weightKg, date) }
    }

    fun changeWaterIncrement(ml: Int) {
        viewModelScope.launch {
            _waterIncrement.value = ml
            repo.setWaterIncrement(ml)
        }
    }

    suspend fun calorieRowsInRange(start: LocalDate, end: LocalDate): List<DayCalorieRow> =
        repo.dayTotalsInRange(start, end)

    suspend fun waterRowsInRange(start: LocalDate, end: LocalDate): List<WaterDayRow> =
        repo.waterDailyInRange(start, end)

    fun setPendingMeal(meal: MealType?) {
        _pendingMeal.value = meal
    }

    suspend fun getFood(id: Long): FoodEntity? = repo.getFood(id)

    fun saveCustomProduct(food: FoodEntity) {
        viewModelScope.launch { repo.saveCustomProduct(food) }
    }

    /**
     * A barcode scan: the local database first, then Open Food Facts. Suspending so
     * the scanner can show the lookup in progress; safe to cancel on back.
     */
    suspend fun scanBarcode(raw: String): ScanResult = barcodeLookup.resolve(raw)

    /** Saves a product created from an unknown barcode and returns it with its id. */
    suspend fun saveProduct(food: FoodEntity): FoodEntity = repo.saveCustomProduct(food)

    fun saveRecipe(food: FoodEntity, ingredients: List<RecipeIngredientEntity>) {
        viewModelScope.launch { repo.saveRecipe(food, ingredients) }
    }

    fun setDisplayName(name: String) {
        viewModelScope.launch {
            repo.setDisplayName(name)
            _displayName.value = repo.getDisplayName()
        }
    }

    fun updateMacros(calories: Int, c: Int, p: Int, f: Int) {
        viewModelScope.launch { repo.updateMacroTargets(calories, c, p, f, overridden = true) }
    }

    fun previewCalories(
        goal: GoalType,
        sex: Sex,
        birthDate: LocalDate,
        heightCm: Float,
        weightKg: Float,
        activity: ActivityLevel,
        pace: PaceOption
    ): Int {
        val age = Period.between(birthDate, LocalDate.now()).years.coerceAtLeast(1)
        val bmr = CalculationEngine.calculateBmr(sex, weightKg, heightCm, age)
        val tdee = CalculationEngine.calculateTdee(bmr, activity)
        return CalculationEngine.dailyCalorieTarget(tdee, goal, pace)
    }

    fun previewWeeks(goal: GoalType, current: Float, target: Float, pace: PaceOption): Int? =
        CalculationEngine.weeksToGoal(current, target, goal, pace)

    companion object {
        val dateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("bg"))
        val monthFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("bg"))
    }
}

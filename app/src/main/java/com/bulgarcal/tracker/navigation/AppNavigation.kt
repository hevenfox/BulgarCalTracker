package com.bulgarcal.tracker.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import com.bulgarcal.tracker.TrackerViewModel
import com.bulgarcal.tracker.data.ActivityLevel
import com.bulgarcal.tracker.data.AppDatabase
import com.bulgarcal.tracker.data.FoodEntity
import com.bulgarcal.tracker.data.GoalType
import com.bulgarcal.tracker.data.Sex
import com.bulgarcal.tracker.domain.CalculationEngine
import com.bulgarcal.tracker.domain.MealType
import com.bulgarcal.tracker.domain.SearchKey
import com.bulgarcal.tracker.domain.ProgressSubTab
import com.bulgarcal.tracker.ui.details.MacroEditScreen
import com.bulgarcal.tracker.ui.details.MicroDetailsScreen
import com.bulgarcal.tracker.ui.diary.DiaryScreen
import com.bulgarcal.tracker.ui.food.CategoryFoodsScreen
import com.bulgarcal.tracker.ui.food.CreationSheet
import com.bulgarcal.tracker.ui.food.FavoriteFoodsScreen
import com.bulgarcal.tracker.ui.food.FoodSearchScreen
import com.bulgarcal.tracker.ui.food.MyRecipesScreen
import com.bulgarcal.tracker.ui.food.NewProductScreen
import com.bulgarcal.tracker.ui.food.NewRecipeScreen
import com.bulgarcal.tracker.ui.scan.ScannerScreen
import com.bulgarcal.tracker.ui.home.HomeScreen
import com.bulgarcal.tracker.ui.more.CalendarScreen
import com.bulgarcal.tracker.ui.more.FeedbackScreen
import com.bulgarcal.tracker.ui.more.InfoScreen
import com.bulgarcal.tracker.ui.more.MoreScreen
import com.bulgarcal.tracker.ui.more.ProfileEditScreen
import com.bulgarcal.tracker.ui.more.ProfileReadOnlyScreen
import com.bulgarcal.tracker.ui.more.RemindersScreen
import com.bulgarcal.tracker.ui.more.YourNumbersScreen
import com.bulgarcal.tracker.ui.food.LogFoodSheet
import com.bulgarcal.tracker.data.DayTotalsRow
import com.bulgarcal.tracker.data.MealLogEntity
import kotlinx.coroutines.flow.combine
import com.bulgarcal.tracker.reminders.ReminderPreferences
import com.bulgarcal.tracker.reminders.ReminderSettings
import com.bulgarcal.tracker.ui.components.MainBottomBar
import com.bulgarcal.tracker.ui.onboarding.GoalSelectionScreen
import com.bulgarcal.tracker.ui.onboarding.IntakeScreen
import com.bulgarcal.tracker.ui.onboarding.ProfileScreen
import com.bulgarcal.tracker.ui.onboarding.rememberOnboardingDraft
import com.bulgarcal.tracker.ui.progress.ProgressScreen
import com.bulgarcal.tracker.ui.theme.PitchBlack
import com.bulgarcal.tracker.ui.theme.ThemeMode
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.Period

object Routes {
    const val ONBOARD_GOAL = "onboard_goal"
    const val ONBOARD_PROFILE = "onboard_profile"
    const val ONBOARD_INTAKE = "onboard_intake"
    const val MAIN = "main"
    const val FOOD_SEARCH = "food_search"
    const val FOOD_CATEGORY = "food_category/{category}"
    const val MY_RECIPES = "my_recipes"
    const val FAVORITES = "favorites"
    const val SCANNER = "scanner"
    const val NEW_PRODUCT = "new_product"
    const val NEW_RECIPE = "new_recipe"
    const val DETAILS = "details"
    const val MACRO_EDIT = "macro_edit"
    const val REMINDERS = "reminders"
    const val PROFILE_EDIT = "profile_edit"
    const val PROFILE_VIEW = "profile_view"
    const val YOUR_NUMBERS = "your_numbers"
    const val CALENDAR = "calendar"
    const val INFO = "info"
    const val FEEDBACK = "feedback"
}

@Composable
fun BulgarCalApp(
    vm: TrackerViewModel = viewModel(),
    openFoodSearch: Boolean = false,
    onFoodSearchConsumed: () -> Unit = {},
    onRequestNotificationPermission: (onResult: (Boolean) -> Unit) -> Unit = {},
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    val nav = rememberNavController()
    val profile by vm.profile.collectAsState()
    val draft = rememberOnboardingDraft()
    var showActivity by remember { mutableStateOf(false) }

    LaunchedEffect(profile?.onboardingComplete) {
        if (profile?.onboardingComplete == true) {
            val current = nav.currentDestination?.route
            if (current?.startsWith("onboard") == true) {
                nav.navigate(Routes.MAIN) { popUpTo(0) { inclusive = true } }
            }
        }
    }

    NavHost(
        navController = nav,
        startDestination = Routes.ONBOARD_GOAL
    ) {
        composable(Routes.ONBOARD_GOAL) {
            GoalSelectionScreen(
                draft = draft,
                onNext = { if (draft.goal != null) nav.navigate(Routes.ONBOARD_PROFILE) },
                onExistingAccount = {
                    if (profile?.onboardingComplete == true) {
                        nav.navigate(Routes.MAIN) { popUpTo(0) { inclusive = true } }
                    }
                }
            )
        }
        composable(Routes.ONBOARD_PROFILE) {
            ProfileScreen(
                draft = draft,
                showActivitySheet = showActivity,
                onShowActivity = { showActivity = it },
                onNext = { nav.navigate(Routes.ONBOARD_INTAKE) }
            )
        }
        composable(Routes.ONBOARD_INTAKE) {
            val goal = draft.goal ?: GoalType.MAINTAIN
            val pace = draft.pace
            val cal = vm.previewCalories(
                goal, draft.sex, draft.birthDate,
                draft.heightCm.toFloatOrNull() ?: 175f,
                draft.weightKg.toFloatOrNull() ?: 75f,
                draft.activity, pace
            )
            val weeks = vm.previewWeeks(
                goal,
                draft.weightKg.toFloatOrNull() ?: 75f,
                draft.targetKg.toFloatOrNull() ?: 70f,
                pace
            )
            IntakeScreen(
                calories = cal,
                weeksText = weeks?.let { "Ще достигнеш целта за $it седмици." },
                goal = goal,
                pace = pace,
                onPaceChange = { draft.pace = it },
                onNext = {
                    vm.completeOnboarding(
                        goal, draft.sex, draft.birthDate,
                        draft.heightCm.toFloatOrNull() ?: 175f,
                        draft.weightKg.toFloatOrNull() ?: 75f,
                        draft.targetKg.toFloatOrNull() ?: 70f,
                        draft.activity, draft.pace
                    )
                    nav.navigate(Routes.MAIN) { popUpTo(0) { inclusive = true } }
                }
            )
        }
        composable(Routes.MAIN) {
            MainShell(
                vm = vm,
                onFood = { nav.navigate(Routes.FOOD_SEARCH) },
                onDetails = { nav.navigate(Routes.DETAILS) },
                onReminders = { nav.navigate(Routes.REMINDERS) },
                onProfileView = { nav.navigate(Routes.PROFILE_VIEW) },
                onYourNumbers = { nav.navigate(Routes.YOUR_NUMBERS) },
                onMacroEdit = { nav.navigate(Routes.MACRO_EDIT) },
                onPickDate = { nav.navigate(Routes.CALENDAR) },
                themeMode = themeMode,
                onThemeChange = onThemeChange
            )
        }
        composable(Routes.PROFILE_VIEW) {
            val p by vm.profile.collectAsState()
            val displayName by vm.displayName.collectAsState()
            ProfileReadOnlyScreen(
                profile = p,
                displayName = displayName,
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate(Routes.PROFILE_EDIT) },
                onSaveName = { vm.setDisplayName(it) }
            )
        }
        composable(Routes.YOUR_NUMBERS) {
            val p by vm.profile.collectAsState()
            val weights by vm.weightEntries.collectAsState()
            YourNumbersScreen(
                profile = p,
                currentWeightKg = weights.lastOrNull()?.weightKg ?: p?.weightKg,
                onBack = { nav.popBackStack() }
            )
        }
        composable(Routes.PROFILE_EDIT) {
            val p by vm.profile.collectAsState()
            ProfileEditScreen(
                profile = p,
                onBack = { nav.popBackStack() },
                onSave = { sex, h, w, t, act, goal, pace, birth ->
                    vm.updateProfile(sex, h, w, t, act, goal, pace, birth)
                    nav.popBackStack()
                }
            )
        }
        composable(Routes.CALENDAR) {
            val ctx = LocalContext.current
            val db = remember { AppDatabase.get(ctx) }
            var calDate by remember { mutableStateOf(vm.selectedDate.value) }
            var totals by remember { mutableStateOf(DayTotalsRow(0, 0f, 0f, 0f, 0f, 0f, 0f, 0f)) }
            var logs by remember { mutableStateOf(emptyList<MealLogEntity>()) }
            LaunchedEffect(calDate) {
                combine(
                    db.mealLogDao().dayTotals(calDate.toEpochDay()),
                    db.mealLogDao().forDay(calDate.toEpochDay())
                ) { t, l -> Pair(t, l) }.collect { (t, l) ->
                    totals = t
                    logs = l
                }
            }
            CalendarScreen(
                pickedDate = calDate,
                totals = totals,
                logs = logs,
                onBack = { nav.popBackStack() },
                onDateChange = { calDate = it },
                onApplyDate = {
                    vm.setDate(it)
                    nav.popBackStack()
                }
            )
        }
        composable(Routes.INFO) {
            InfoScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.FEEDBACK) {
            FeedbackScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.REMINDERS) {
            val ctx = LocalContext.current
            val prefs = remember { ReminderPreferences(ctx) }
            val settings by prefs.settings.collectAsState(initial = ReminderSettings())
            RemindersScreen(
                settings = settings,
                onBack = { nav.popBackStack() },
                onRequestPermission = { onGranted ->
                    onRequestNotificationPermission { granted ->
                        if (granted) onGranted()
                    }
                }
            )
        }
        foodRoutes(nav, vm)
        composable(Routes.DETAILS) {
            val date by vm.selectedDate.collectAsState()
            val totals by vm.dayTotals.collectAsState()
            MicroDetailsScreen(
                dateLabel = if (date == LocalDate.now()) "Днес" else date.format(TrackerViewModel.dateFmt),
                profile = profile,
                totals = totals,
                onPrevDay = { vm.shiftDate(-1) },
                onNextDay = { vm.shiftDate(1) },
                onEdit = { nav.navigate(Routes.MACRO_EDIT) }
            )
        }
        composable(Routes.MACRO_EDIT) {
            MacroEditScreen(
                profile = profile,
                onBack = { nav.popBackStack() },
                onSave = { c, cp, pp, fp ->
                    vm.updateMacros(c, cp, pp, fp)
                    nav.popBackStack()
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        if (profile?.onboardingComplete == true) {
            nav.navigate(Routes.MAIN) { popUpTo(0) { inclusive = true } }
        }
    }

    LaunchedEffect(openFoodSearch, profile?.onboardingComplete) {
        if (openFoodSearch && profile?.onboardingComplete == true) {
            nav.navigate(Routes.FOOD_SEARCH)
            onFoodSearchConsumed()
        }
    }
}

private fun NavGraphBuilder.foodRoutes(nav: NavHostController, vm: TrackerViewModel) {
    composable(Routes.FOOD_SEARCH) {
        val ctx = LocalContext.current
        val db = remember { AppDatabase.get(ctx) }
        val selectedDate by vm.selectedDate.collectAsState()
        val pendingMeal by vm.pendingMeal.collectAsState()
        val dbReady by vm.foodDbReady.collectAsState()
        var query by remember { mutableStateOf("") }
        var showCreate by remember { mutableStateOf(false) }
        var foodToLog by remember { mutableStateOf<FoodEntity?>(null) }
        // Typing must stay responsive against ~30 000 rows: run the query 300 ms after
        // the last keystroke, and keep it as a Flow so results also appear when the
        // database is still being seeded on first launch.
        var debounced by remember { mutableStateOf("") }
        LaunchedEffect(query) {
            delay(300)
            debounced = query.trim()
        }
        val results by remember(debounced) {
            if (debounced.isBlank()) flowOf(emptyList())
            else db.foodDao().search(SearchKey.ofQuery(debounced), debounced.lowercase())
        }.collectAsState(initial = emptyList())
        FoodSearchScreen(
            query = query,
            onQueryChange = { query = it },
            foods = results,
            loading = !dbReady,
            onCategory = { nav.navigate("food_category/$it") },
            onRecipes = { nav.navigate(Routes.MY_RECIPES) },
            onFavorites = { nav.navigate(Routes.FAVORITES) },
            onAddProduct = { showCreate = true },
            onScan = { nav.navigate(Routes.SCANNER) },
            onFoodClick = { foodToLog = it }
        )
        if (showCreate) {
            CreationSheet(
                onDismiss = { showCreate = false },
                onRecipe = { showCreate = false; nav.navigate(Routes.NEW_RECIPE) },
                onProduct = { showCreate = false; nav.navigate(Routes.NEW_PRODUCT) }
            )
        }
        foodToLog?.let { food ->
            LogFoodSheet(
                food = food,
                defaultDate = selectedDate,
                initialMeal = pendingMeal,
                onDismiss = {
                    foodToLog = null
                    vm.setPendingMeal(null)
                },
                onConfirm = { grams, meal, date, favorite ->
                    vm.logFood(food, meal, grams, date)
                    vm.setFavorite(food.id, favorite)
                    foodToLog = null
                    vm.setPendingMeal(null)
                    nav.popBackStack(Routes.MAIN, false)
                }
            )
        }
    }
    composable(
        route = Routes.FOOD_CATEGORY,
        arguments = listOf(navArgument("category") { type = NavType.StringType })
    ) { entry ->
        val cat = entry.arguments?.getString("category") ?: ""
        val ctx = LocalContext.current
        val db = remember { AppDatabase.get(ctx) }
        val selectedDate by vm.selectedDate.collectAsState()
        val pendingMeal by vm.pendingMeal.collectAsState()
        var foodToLog by remember { mutableStateOf<FoodEntity?>(null) }
        // A Flow, not a one-shot read: a 6 000-row category arrives as it is queried.
        val foods by remember(cat) { db.foodDao().byCategory(cat, "") }
            .collectAsState(initial = emptyList())
        CategoryFoodsScreen(
            category = cat,
            foods = foods,
            onBack = { nav.popBackStack() },
            onSelect = { foodToLog = it }
        )
        foodToLog?.let { food ->
            LogFoodSheet(
                food = food,
                defaultDate = selectedDate,
                initialMeal = pendingMeal,
                onDismiss = {
                    foodToLog = null
                    vm.setPendingMeal(null)
                },
                onConfirm = { grams, meal, date, favorite ->
                    vm.logFood(food, meal, grams, date)
                    vm.setFavorite(food.id, favorite)
                    foodToLog = null
                    vm.setPendingMeal(null)
                    nav.popBackStack(Routes.MAIN, false)
                }
            )
        }
    }
    composable(Routes.FAVORITES) {
        val favorites by vm.favoriteFoods.collectAsState()
        val selectedDate by vm.selectedDate.collectAsState()
        val pendingMeal by vm.pendingMeal.collectAsState()
        var foodToLog by remember { mutableStateOf<FoodEntity?>(null) }
        FavoriteFoodsScreen(
            favorites = favorites,
            onBack = { nav.popBackStack() },
            onSelect = { foodToLog = it }
        )
        foodToLog?.let { food ->
            LogFoodSheet(
                food = food,
                defaultDate = selectedDate,
                initialMeal = pendingMeal,
                onDismiss = {
                    foodToLog = null
                    vm.setPendingMeal(null)
                },
                onConfirm = { grams, meal, date, favorite ->
                    vm.logFood(food, meal, grams, date)
                    vm.setFavorite(food.id, favorite)
                    foodToLog = null
                    vm.setPendingMeal(null)
                    nav.popBackStack(Routes.MAIN, false)
                }
            )
        }
    }
    composable(Routes.MY_RECIPES) {
        var showCreate by remember { mutableStateOf(false) }
        val ctx = LocalContext.current
        val db = remember { AppDatabase.get(ctx) }
        var recipes by remember { mutableStateOf(emptyList<FoodEntity>()) }
        LaunchedEffect(Unit) { recipes = db.foodDao().recipes().first() }
        MyRecipesScreen(
            query = "",
            onQueryChange = {},
            recipes = recipes,
            onBack = { nav.popBackStack() },
            onCreate = { showCreate = true }
        )
        if (showCreate) {
            CreationSheet(
                onDismiss = { showCreate = false },
                onRecipe = { nav.navigate(Routes.NEW_RECIPE); showCreate = false },
                onProduct = { nav.navigate(Routes.NEW_PRODUCT); showCreate = false }
            )
        }
    }
    composable(Routes.SCANNER) {
        val selectedDate by vm.selectedDate.collectAsState()
        val pendingMeal by vm.pendingMeal.collectAsState()
        ScannerScreen(
            vm = vm,
            defaultDate = selectedDate,
            initialMeal = pendingMeal,
            onClose = { nav.popBackStack() },
            onLogged = {
                vm.setPendingMeal(null)
                nav.popBackStack(Routes.MAIN, false)
            }
        )
    }
    composable(Routes.NEW_PRODUCT) {
        NewProductScreen(
            onBack = { nav.popBackStack() },
            onSave = { vm.saveCustomProduct(it); nav.popBackStack() }
        )
    }
    composable(Routes.NEW_RECIPE) {
        NewRecipeScreen(
            onBack = { nav.popBackStack() },
            onSave = { vm.saveRecipe(it, emptyList()); nav.popBackStack() }
        )
    }
}

@Composable
private fun MainShell(
    vm: TrackerViewModel,
    onFood: () -> Unit,
    onDetails: () -> Unit,
    onReminders: () -> Unit,
    onProfileView: () -> Unit,
    onYourNumbers: () -> Unit,
    onMacroEdit: () -> Unit,
    onPickDate: () -> Unit,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    var progressSubTab by remember { mutableStateOf(ProgressSubTab.WEIGHT) }

    // Back from Прогрес / Дневник / Още (also after a deep link from Начало) returns to Начало
    BackHandler(enabled = tab != 0) { tab = 0 }

    val profile by vm.profile.collectAsState()
    val date by vm.selectedDate.collectAsState()
    val totals by vm.dayTotals.collectAsState()
    val logs by vm.mealLogs.collectAsState()
    val mealCals by vm.mealCalories.collectAsState()
    val weights by vm.weightEntries.collectAsState()
    val favorites by vm.favoriteFoods.collectAsState()
    val selectedDate by vm.selectedDate.collectAsState()
    val waterLogs by vm.waterLogs.collectAsState()
    val waterIncrement by vm.waterIncrement.collectAsState()
    val waterGoal by vm.waterGoalMl.collectAsState()
    val streak by vm.loggingStreak.collectAsState()
    val displayName by vm.displayName.collectAsState()
    val ctx = LocalContext.current
    val reminderPrefs = remember { ReminderPreferences(ctx) }
    val reminderSettings by reminderPrefs.settings.collectAsState(initial = ReminderSettings())
    var foodToLog by remember { mutableStateOf<FoodEntity?>(null) }
    var logToEdit by remember { mutableStateOf<MealLogEntity?>(null) }
    var logFoodForEdit by remember { mutableStateOf<FoodEntity?>(null) }

    val dateLabel = if (date == LocalDate.now()) "Днес" else date.format(TrackerViewModel.dateFmt)
    val currentWeight = weights.lastOrNull()?.weightKg ?: profile?.weightKg ?: 0f
    val sex = runCatching { Sex.valueOf(profile?.sex ?: "") }.getOrDefault(Sex.MALE)
    val activity = runCatching { ActivityLevel.valueOf(profile?.activityLevel ?: "") }
        .getOrDefault(ActivityLevel.LIGHT)
    val age = profile?.let {
        Period.between(LocalDate.ofEpochDay(it.birthDateEpochDay), LocalDate.now()).years.coerceAtLeast(1)
    } ?: 25
    val bmr = profile?.let {
        CalculationEngine.calculateBmr(sex, currentWeight, it.heightCm, age).toInt()
    } ?: 0
    val tdee = if (bmr > 0) {
        CalculationEngine.calculateTdee(bmr.toFloat(), activity).toInt()
    } else 0

    // generic add paths (bottom-bar "+", Дневник "(+)"): no meal pick carried over, so the
    // sheet falls back to the meal that matches the current time
    val openAddFood = {
        vm.setPendingMeal(null)
        onFood()
    }

    Scaffold(
        containerColor = PitchBlack,
        bottomBar = {
            MainBottomBar(
                selectedTab = tab,
                onTabSelected = { tab = it },
                onAddFood = openAddFood
            )
        }
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .statusBarsPadding()
        ) {
            when (tab) {
                0 -> HomeScreen(
                    selectedDate = date,
                    profile = profile,
                    totals = totals,
                    mealCals = mealCals,
                    waterLogs = waterLogs,
                    waterIncrementMl = waterIncrement,
                    waterGoalMl = waterGoal,
                    streak = streak,
                    weightEntries = weights,
                    onPrevDay = { vm.shiftDate(-1) },
                    onNextDay = { vm.shiftDate(1) },
                    onPickDate = onPickDate,
                    onDetails = onDetails,
                    onMacroEdit = onMacroEdit,
                    onOpenDiary = { tab = 2 },
                    onStreakClick = {
                        progressSubTab = ProgressSubTab.NUTRITION
                        tab = 1
                    },
                    onProfileClick = { tab = 3 },
                    onOpenProgressWeight = {
                        progressSubTab = ProgressSubTab.WEIGHT
                        tab = 1
                    },
                    onAddWater = { vm.addQuickWater() },
                    onRemoveLastWater = { vm.removeLastWater() },
                    onWaterIncrementChange = { ml -> vm.changeWaterIncrement(ml) },
                    onRecordWeight = { vm.addWeight(it) },
                    onMealClick = { meal ->
                        vm.setPendingMeal(meal)
                        onFood()
                    }
                )
                1 -> ProgressScreen(
                    profile = profile,
                    weights = weights,
                    streak = streak,
                    waterGoalMl = waterGoal,
                    subTab = progressSubTab,
                    onSubTabChange = { progressSubTab = it },
                    onProfileClick = { tab = 3 },
                    onMacroEdit = onMacroEdit,
                    loadCalories = { start, end -> vm.calorieRowsInRange(start, end) },
                    loadWater = { start, end -> vm.waterRowsInRange(start, end) }
                )
                2 -> DiaryScreen(
                    dateLabel = dateLabel,
                    totals = totals,
                    logs = logs,
                    favorites = favorites,
                    onPrevDay = { vm.shiftDate(-1) },
                    onNextDay = { vm.shiftDate(1) },
                    onAdd = openAddFood,
                    onFavoriteClick = { foodToLog = it },
                    onLogClick = { log ->
                        logToEdit = log
                    }
                )
                3 -> MoreScreen(
                    displayName = displayName,
                    dailyCalories = profile?.dailyCalories ?: 0,
                    bmr = bmr,
                    tdee = tdee,
                    remindersEnabled = reminderSettings.enabled,
                    onProfile = onProfileView,
                    onYourNumbers = onYourNumbers,
                    onReminders = onReminders,
                    themeMode = themeMode,
                    onThemeChange = onThemeChange
                )
            }
        }
        foodToLog?.let { food ->
            LogFoodSheet(
                food = food,
                defaultDate = selectedDate,
                onDismiss = { foodToLog = null },
                onConfirm = { grams, meal, date, favorite ->
                    vm.logFood(food, meal, grams, date)
                    vm.setFavorite(food.id, favorite)
                    foodToLog = null
                }
            )
        }
        if (logToEdit != null) {
            LaunchedEffect(logToEdit?.id) {
                val currentLog = logToEdit ?: return@LaunchedEffect
                logFoodForEdit = vm.getFood(currentLog.foodId)
            }
            val food = logFoodForEdit
            if (food != null && logToEdit != null) {
                LogFoodSheet(
                    food = food,
                    defaultDate = selectedDate,
                    initialMeal = MealType.entries.find { it.name == logToEdit!!.mealType } ?: MealType.LUNCH,
                    existingLog = logToEdit,
                    onDismiss = {
                        logToEdit = null
                        logFoodForEdit = null
                    },
                    onConfirm = { grams, meal, date, favorite ->
                        vm.updateLog(logToEdit!!, grams, meal, date)
                        vm.setFavorite(food.id, favorite)
                        logToEdit = null
                        logFoodForEdit = null
                    },
                    onDelete = {
                        vm.deleteLog(logToEdit!!.id)
                        logToEdit = null
                        logFoodForEdit = null
                    }
                )
            }
        }
    }
}


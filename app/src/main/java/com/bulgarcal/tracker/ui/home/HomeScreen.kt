package com.bulgarcal.tracker.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bulgarcal.tracker.data.DayTotalsRow
import com.bulgarcal.tracker.data.UserProfileEntity
import com.bulgarcal.tracker.data.WaterLogEntity
import com.bulgarcal.tracker.data.WeightEntryEntity
import com.bulgarcal.tracker.domain.MealType
import com.bulgarcal.tracker.ui.components.BgFormat
import com.bulgarcal.tracker.ui.components.HomeTopBar
import com.bulgarcal.tracker.ui.theme.BorderGray
import com.bulgarcal.tracker.ui.theme.EmeraldTeal
import com.bulgarcal.tracker.ui.theme.TextSilver
import com.bulgarcal.tracker.ui.theme.TextWhite
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Начало. Sections live in HomeSections.kt, the drink-size sheet in WaterSizeSheet.kt.
 * Navigation is entirely callback-driven (see MainShell in AppNavigation.kt).
 */
@Composable
fun HomeScreen(
    selectedDate: LocalDate,
    profile: UserProfileEntity?,
    totals: DayTotalsRow,
    mealCals: Map<String, Int>,
    waterLogs: List<WaterLogEntity>,
    waterIncrementMl: Int,
    waterGoalMl: Int,
    streak: Int,
    weightEntries: List<WeightEntryEntity>,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onPickDate: () -> Unit,
    onDetails: () -> Unit,
    onMacroEdit: () -> Unit,
    onOpenDiary: () -> Unit,
    onStreakClick: () -> Unit,
    onProfileClick: () -> Unit,
    onOpenProgressWeight: () -> Unit,
    onAddWater: () -> Unit,
    onRemoveLastWater: () -> Unit,
    onWaterIncrementChange: (Int) -> Unit,
    onRecordWeight: (Float) -> Unit,
    /** Tapping a meal tile opens the add-food flow pre-set to that meal. */
    onMealClick: (MealType) -> Unit
) {
    val targetCal = profile?.dailyCalories ?: 2000
    val pct = if (targetCal > 0) ((totals.calories * 100f) / targetCal).roundToInt() else 0
    var showWaterSheet by remember { mutableStateOf(false) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var showWaterInfo by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(8.dp))
        HomeTopBar(
            selectedDate = selectedDate,
            streak = streak,
            onPrevDay = onPrevDay,
            onNextDay = onNextDay,
            onStreakClick = onStreakClick,
            onProfileClick = onProfileClick,
            onDateClick = onPickDate
        )
        Spacer(Modifier.height(16.dp))
        CalorieRingSection(
            eaten = totals.calories,
            budget = targetCal,
            percentOfGoal = pct,
            carbs = totals.carbs,
            protein = totals.protein,
            fats = totals.fats,
            carbsTarget = profile?.carbsGrams ?: 0f,
            proteinTarget = profile?.proteinGrams ?: 0f,
            fatTarget = profile?.fatGrams ?: 0f
        )
        Spacer(Modifier.height(16.dp))
        MealsSection(
            mealCals = mealCals,
            onOpenDiary = onOpenDiary,
            onMealClick = onMealClick
        )
        Spacer(Modifier.height(16.dp))
        MacroRatioSection(
            totals = totals,
            profile = profile,
            onDetails = onDetails,
            onMacroEdit = onMacroEdit
        )
        Spacer(Modifier.height(16.dp))
        WaterSection(
            waterLogs = waterLogs,
            incrementMl = waterIncrementMl,
            goalMl = waterGoalMl,
            onAdd = onAddWater,
            onRemove = onRemoveLastWater,
            onChangeSize = { showWaterSheet = true },
            onInfo = { showWaterInfo = true }
        )
        Spacer(Modifier.height(16.dp))
        TargetWeightSection(
            selectedDate = selectedDate,
            profile = profile,
            entries = weightEntries,
            onProgress = onOpenProgressWeight,
            onRecord = { showWeightDialog = true }
        )
        Spacer(Modifier.height(24.dp))
    }

    if (showWaterSheet) {
        WaterSizeSheet(
            selectedMl = waterIncrementMl,
            onSelect = {
                onWaterIncrementChange(it)
                showWaterSheet = false
            },
            onDismiss = { showWaterSheet = false }
        )
    }
    if (showWaterInfo) {
        AlertDialog(
            onDismissRequest = { showWaterInfo = false },
            confirmButton = {
                TextButton(onClick = { showWaterInfo = false }) { Text("OK", color = EmeraldTeal) }
            },
            title = { Text("Вода", color = TextWhite) },
            text = {
                Text(
                    "Дневната цел е ${BgFormat.litersFromMl(waterGoalMl)} L, изчислена от теглото ти " +
                        "(35 ml на kg). Добавяй напитки с + и премахвай последната с −.",
                    color = TextSilver
                )
            }
        )
    }
    if (showWeightDialog) {
        WeightRecordDialog(
            initial = weightEntries.lastOrNull()?.weightKg ?: profile?.weightKg ?: 0f,
            onDismiss = { showWeightDialog = false },
            onConfirm = {
                onRecordWeight(it)
                showWeightDialog = false
            }
        )
    }
}

@Composable
private fun WeightRecordDialog(
    initial: Float,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var text by remember { mutableStateOf(if (initial > 0f) BgFormat.decimalOne(initial) else "") }
    val value = text.replace(',', '.').toFloatOrNull()?.takeIf { it in 20f..400f }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Запиши тегло", color = TextWhite) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("kg") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = EmeraldTeal,
                    unfocusedBorderColor = BorderGray
                )
            )
        },
        confirmButton = {
            TextButton(
                enabled = value != null,
                onClick = { value?.let(onConfirm) }
            ) { Text("Запиши", color = if (value != null) EmeraldTeal else TextSilver) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отказ", color = TextSilver) }
        }
    )
}

package com.bulgarcal.tracker.ui.food

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulgarcal.tracker.data.FoodEntity
import com.bulgarcal.tracker.ui.food.ExtendedMacroRow
import com.bulgarcal.tracker.ui.food.FoodSourceLine
import com.bulgarcal.tracker.domain.MealType
import com.bulgarcal.tracker.data.MealLogEntity
import com.bulgarcal.tracker.ui.components.TealButton
import com.bulgarcal.tracker.ui.theme.BorderGray
import com.bulgarcal.tracker.ui.theme.CarbsRed
import com.bulgarcal.tracker.ui.theme.EmeraldTeal
import com.bulgarcal.tracker.ui.theme.FatOrange
import com.bulgarcal.tracker.ui.theme.InputGray
import com.bulgarcal.tracker.ui.theme.ProteinBlue
import com.bulgarcal.tracker.ui.theme.TextSilver
import com.bulgarcal.tracker.ui.theme.TextWhite
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LogFoodSheet(
    food: FoodEntity,
    defaultDate: LocalDate,
    /** Explicit meal pick (meal tile / notification). `null` = use the meal that fits the clock. */
    initialMeal: MealType? = null,
    existingLog: MealLogEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (grams: Float, meal: MealType, date: LocalDate, favorite: Boolean) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var grams by remember { mutableStateOf(existingLog?.grams?.toInt()?.toString() ?: "100") }
    var meal by remember {
        mutableStateOf(
            existingLog?.let { m -> MealType.entries.find { it.name == m.mealType } }
                ?: initialMeal
                ?: MealType.forCurrentTime()
        )
    }
    var logDate by remember { mutableStateOf(existingLog?.let { LocalDate.ofEpochDay(it.dateEpochDay) } ?: defaultDate) }
    var favorite by remember { mutableStateOf(food.isFavorite) }
    val dateFmt = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale("bg")) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Text(food.name, color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                "${food.calories} kcal / ${food.servingGrams.toInt()}g",
                color = TextSilver,
                fontSize = 13.sp
            )
            FoodSourceLine(food.source)
            Spacer(Modifier.height(12.dp))
            MacroSummaryRow(food = food, grams = grams.toFloatOrNull() ?: 100f)
            Spacer(Modifier.height(6.dp))
            ExtendedMacroRow(food = food, grams = grams.toFloatOrNull() ?: 100f)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = grams,
                onValueChange = { grams = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Грамаж (g)", color = TextSilver) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = fieldColors()
            )
            Spacer(Modifier.height(12.dp))
            Text("Ястие", color = TextSilver, fontSize = 13.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MealType.entries.forEach { m ->
                    FilterChip(
                        selected = meal == m,
                        onClick = { meal = m },
                        label = { Text(m.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldTeal.copy(alpha = 0.3f),
                            selectedLabelColor = TextWhite,
                            labelColor = TextSilver
                        )
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Дата", color = TextSilver, fontSize = 13.sp)
            RowDatePicker(logDate, dateFmt) { logDate = it }
            Spacer(Modifier.height(12.dp))
            androidx.compose.foundation.layout.Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                androidx.compose.foundation.layout.Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = if (favorite) EmeraldTeal else TextSilver)
                    Text(" Любимо", color = TextWhite, modifier = Modifier.padding(start = 8.dp))
                }
                Switch(
                    checked = favorite,
                    onCheckedChange = { favorite = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = EmeraldTeal)
                )
            }
            Spacer(Modifier.height(20.dp))
            TealButton(if (existingLog == null) "Добави" else "Запази") {
                val g = grams.toFloatOrNull() ?: 100f
                onConfirm(g.coerceAtLeast(1f), meal, logDate, favorite)
            }
            if (existingLog != null && onDelete != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Изтрий записа",
                    color = TextSilver,
                    modifier = Modifier
                        .clickable { onDelete() }
                        .padding(vertical = 8.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RowDatePicker(
    date: LocalDate,
    fmt: DateTimeFormatter,
    onChange: (LocalDate) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text("‹", color = TextWhite, modifier = Modifier.clickable { onChange(date.minusDays(1)) }.padding(12.dp))
        Text(date.format(fmt), color = EmeraldTeal, fontWeight = FontWeight.Medium)
        Text("›", color = TextWhite, modifier = Modifier.clickable { onChange(date.plusDays(1)) }.padding(12.dp))
    }
}

/**
 * Macros for the grams currently typed in, scaled exactly like the stored log
 * (`value * grams / servingGrams`), so what you see is what gets logged.
 */
@Composable
private fun MacroSummaryRow(food: FoodEntity, grams: Float) {
    val factor = grams / food.servingGrams.coerceAtLeast(1f)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        MacroSummaryCell("Енергия", "${(food.calories * factor).toInt()} kcal", TextWhite)
        MacroSummaryCell("Въглехи", "${(food.carbs * factor).toInt()} g", CarbsRed)
        MacroSummaryCell("Протеини", "${(food.protein * factor).toInt()} g", ProteinBlue)
        MacroSummaryCell("Мазнини", "${(food.fats * factor).toInt()} g", FatOrange)
    }
}

@Composable
private fun MacroSummaryCell(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextSilver, fontSize = 12.sp)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = InputGray,
    unfocusedContainerColor = InputGray,
    focusedBorderColor = BorderGray,
    unfocusedBorderColor = BorderGray,
    focusedTextColor = TextWhite,
    unfocusedTextColor = TextWhite
)

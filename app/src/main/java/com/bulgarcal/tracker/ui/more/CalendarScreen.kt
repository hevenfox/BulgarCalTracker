package com.bulgarcal.tracker.ui.more

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulgarcal.tracker.data.DayTotalsRow
import com.bulgarcal.tracker.data.MealLogEntity
import com.bulgarcal.tracker.domain.MealType
import com.bulgarcal.tracker.ui.components.FormScreenInsets
import com.bulgarcal.tracker.ui.components.ScreenTopBar
import com.bulgarcal.tracker.ui.components.TealButton
import com.bulgarcal.tracker.ui.theme.CaloriePurple
import com.bulgarcal.tracker.ui.theme.SurfaceGray
import com.bulgarcal.tracker.ui.theme.TextSilver
import com.bulgarcal.tracker.ui.theme.TextWhite
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    pickedDate: LocalDate,
    totals: DayTotalsRow,
    logs: List<MealLogEntity>,
    onBack: () -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onApplyDate: (LocalDate) -> Unit
) {
    var picked by remember(pickedDate) { mutableStateOf(pickedDate) }
    var showPicker by remember { mutableStateOf(false) }
    val fmt = remember { DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("bg")) }

    FormScreenInsets {
        ScreenTopBar("Календар", onBack = onBack)
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Text("Избери дата", color = TextSilver, fontSize = 13.sp)
            Text(picked.format(fmt), color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            TealButton("Промени дата", modifier = Modifier.fillMaxWidth()) { showPicker = true }
            Spacer(Modifier.height(16.dp))
            Text("Общо: ${totals.calories} kcal", color = CaloriePurple, fontWeight = FontWeight.Bold)
            Text(
                "Въглехидрати ${totals.carbs.toInt()}g · Протеини ${totals.protein.toInt()}g · Мазнини ${totals.fats.toInt()}g",
                color = TextSilver,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(12.dp))
            Text("Храни", color = TextWhite, fontWeight = FontWeight.Bold)
            LazyColumn(Modifier.weight(1f)) {
                if (logs.isEmpty()) {
                    item { Text("Няма записи за този ден.", color = TextSilver, modifier = Modifier.padding(16.dp)) }
                } else {
                    items(logs) { log ->
                        val meal = MealType.entries.find { it.name == log.mealType }?.label ?: log.mealType
                        Surface(color = SurfaceGray, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(log.foodName, color = TextWhite, fontWeight = FontWeight.Medium)
                                Text("$meal · ${log.grams.toInt()}g · ${log.calories} kcal", color = TextSilver, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            TealButton("Отвори този ден в приложението") {
                onApplyDate(picked)
            }
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = picked.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        val d = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
                        picked = d
                        onDateChange(d)
                    }
                    showPicker = false
                }) { Text("OK", color = CaloriePurple) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Отказ", color = TextSilver) }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

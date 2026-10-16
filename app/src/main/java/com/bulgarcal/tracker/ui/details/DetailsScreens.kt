package com.bulgarcal.tracker.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulgarcal.tracker.data.DayTotalsRow
import com.bulgarcal.tracker.data.UserProfileEntity
import com.bulgarcal.tracker.domain.CalculationEngine
import com.bulgarcal.tracker.ui.components.DateNavigator
import com.bulgarcal.tracker.ui.components.ScreenTopBar
import com.bulgarcal.tracker.ui.components.TealButton
import com.bulgarcal.tracker.ui.theme.CaloriePurple
import com.bulgarcal.tracker.ui.theme.CarbsRed
import com.bulgarcal.tracker.ui.theme.EmeraldTeal
import com.bulgarcal.tracker.ui.theme.FatOrange
import com.bulgarcal.tracker.ui.theme.ProteinBlue
import com.bulgarcal.tracker.ui.theme.SurfaceGray
import com.bulgarcal.tracker.ui.theme.TextSilver
import com.bulgarcal.tracker.ui.theme.TextWhite

@Composable
fun MicroDetailsScreen(
    dateLabel: String,
    profile: UserProfileEntity?,
    totals: DayTotalsRow,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onEdit: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Детайли", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        DateNavigator(dateLabel, onPrevDay, onNextDay)
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Процентни съотношения", color = TextWhite, fontWeight = FontWeight.Bold)
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = EmeraldTeal)
            }
        }
        profile?.let { p ->
            RatioRow("Енергия", p.dailyCalories, totals.calories, CaloriePurple)
            RatioRow("Въглехидрати", p.carbsGrams.toInt(), totals.carbs.toInt(), CarbsRed)
            RatioRow("Протеини", p.proteinGrams.toInt(), totals.protein.toInt(), ProteinBlue)
            RatioRow("Мазнини", p.fatGrams.toInt(), totals.fats.toInt(), FatOrange)
        }
        Spacer(Modifier.height(16.dp))
        DetailSection("Въглехидрати", listOf(
            "Нетни" to "${(totals.carbs - totals.fiber).toInt()} g",
            "Фибри" to "${totals.fiber.toInt()} g",
            "Захар" to "${totals.sugars.toInt()} g",
            "ГИ" to "—",
            "ГЛ" to "—"
        ))
        DetailSection("Мазнини", listOf(
            "Мононенаситени" to "—",
            "Полиненаситени" to "—",
            "Наситени" to "${totals.saturatedFats.toInt()} g",
            "Трансмазнини" to "0 g"
        ))
        DetailSection("Други", listOf(
            "Холестерол" to "—",
            "Сол" to "${totals.salt.toInt()} g",
            "Калий" to "—",
            "Вода" to "—"
        ))
    }
}

@Composable
private fun RatioRow(label: String, target: Int, current: Int, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        PillBadge("$label ${current}", color.copy(alpha = 0.3f), color)
        PillBadge("Цел $target", SurfaceGray, TextSilver)
    }
}

@Composable
private fun PillBadge(text: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color) {
    Surface(color = bg, shape = RoundedCornerShape(20.dp)) {
        Text(text, color = fg, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp)
    }
}

@Composable
private fun DetailSection(title: String, items: List<Pair<String, String>>) {
    Text(title, color = TextWhite, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
    items.forEach { (k, v) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(k, color = TextSilver)
            Text(v, color = TextWhite)
        }
    }
}

@Composable
fun MacroEditScreen(
    profile: UserProfileEntity?,
    onBack: () -> Unit,
    onSave: (Int, Int, Int, Int) -> Unit
) {
    var calories by remember { mutableFloatStateOf((profile?.dailyCalories ?: 2000).toFloat()) }
    var carbsPct by remember { mutableIntStateOf(profile?.carbsPercent ?: 50) }
    var proteinPct by remember { mutableIntStateOf(profile?.proteinPercent ?: 20) }
    var fatPct by remember { mutableIntStateOf(profile?.fatPercent ?: 30) }

    fun rebalance(changed: String, newVal: Int) {
        when (changed) {
            "c" -> { carbsPct = newVal; val rem = 100 - newVal; proteinPct = rem / 2; fatPct = rem - proteinPct }
            "p" -> { proteinPct = newVal; fatPct = (100 - carbsPct - newVal).coerceAtLeast(0) }
            "f" -> { fatPct = newVal; proteinPct = (100 - carbsPct - newVal).coerceAtLeast(0) }
        }
    }

    val macros = CalculationEngine.macroTargetsFromPercents(calories.toInt(), carbsPct, proteinPct, fatPct)

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        ScreenTopBar("Детайли", onBack = onBack, onSave = { onSave(calories.toInt(), carbsPct, proteinPct, fatPct) })
        Text("Калории", color = TextWhite, fontWeight = FontWeight.Bold)
        Slider(
            value = calories,
            onValueChange = { calories = it },
            valueRange = 1075f..3230f,
            colors = SliderDefaults.colors(thumbColor = EmeraldTeal, activeTrackColor = EmeraldTeal)
        )
        Text("${calories.toInt()} kcal", color = TextWhite)
        Text("Тази настройка ще презапише калориите, които се изчисляват от приложението.", color = TextSilver, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        MacroSlider("Въглехидрати", carbsPct, macros.carbsGrams, CarbsRed) { rebalance("c", it) }
        MacroSlider("Протеини", proteinPct, macros.proteinGrams, ProteinBlue) { rebalance("p", it) }
        MacroSlider("Мазнини", fatPct, macros.fatGrams, FatOrange) { rebalance("f", it) }
        Text("Общо: ${carbsPct + proteinPct + fatPct}%", color = if (carbsPct + proteinPct + fatPct == 100) EmeraldTeal else CarbsRed)
    }
}

@Composable
private fun MacroSlider(label: String, percent: Int, grams: Float, color: androidx.compose.ui.graphics.Color, onChange: (Int) -> Unit) {
    Text("$label — $percent% (${grams.toInt()} g)", color = TextWhite, modifier = Modifier.padding(top = 12.dp))
    Slider(
        value = percent.toFloat(),
        onValueChange = { onChange(it.toInt()) },
        valueRange = 5f..80f,
        colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color)
    )
}

package com.bulgarcal.tracker.ui.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulgarcal.tracker.data.DayCalorieRow
import com.bulgarcal.tracker.data.UserProfileEntity
import com.bulgarcal.tracker.data.WaterDayRow
import com.bulgarcal.tracker.data.WeightEntryEntity
import com.bulgarcal.tracker.domain.CalorieDayStatus
import com.bulgarcal.tracker.domain.ProgressAnalytics
import com.bulgarcal.tracker.domain.ProgressSubTab
import com.bulgarcal.tracker.domain.ProgressTimeRange
import com.bulgarcal.tracker.domain.TargetDirection
import com.bulgarcal.tracker.domain.WaterDayStatus
import com.bulgarcal.tracker.ui.components.BgFormat
import com.bulgarcal.tracker.ui.components.SectionCard
import com.bulgarcal.tracker.ui.components.SectionHeader
import com.bulgarcal.tracker.ui.theme.CarbsRed
import com.bulgarcal.tracker.ui.theme.EmeraldTeal
import com.bulgarcal.tracker.ui.theme.FatOrange
import com.bulgarcal.tracker.ui.theme.InputGray
import com.bulgarcal.tracker.ui.theme.PitchBlack
import com.bulgarcal.tracker.ui.theme.ProteinBlue
import com.bulgarcal.tracker.ui.theme.TextSilver
import com.bulgarcal.tracker.ui.theme.TextWhite
import com.bulgarcal.tracker.ui.theme.WaterBlue
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun ProgressScreen(
    profile: UserProfileEntity?,
    weights: List<WeightEntryEntity>,
    streak: Int,
    waterGoalMl: Int,
    subTab: ProgressSubTab,
    onSubTabChange: (ProgressSubTab) -> Unit,
    onProfileClick: () -> Unit,
    onMacroEdit: () -> Unit,
    loadCalories: suspend (LocalDate, LocalDate) -> List<DayCalorieRow>,
    loadWater: suspend (LocalDate, LocalDate) -> List<WaterDayRow>
) {
    var range by remember { mutableStateOf(if (subTab == ProgressSubTab.NUTRITION) ProgressTimeRange.W1 else ProgressTimeRange.M1) }
    var calorieRows by remember { mutableStateOf(emptyList<DayCalorieRow>()) }
    var waterRows by remember { mutableStateOf(emptyList<WaterDayRow>()) }
    val end = LocalDate.now()
    val start = ProgressAnalytics.dateRangeEnd(end, range)
    val allowedRanges = if (subTab == ProgressSubTab.NUTRITION) {
        listOf(
            ProgressTimeRange.W1, ProgressTimeRange.M1, ProgressTimeRange.M3,
            ProgressTimeRange.M6, ProgressTimeRange.Y1, ProgressTimeRange.ALL
        )
    } else {
        listOf(
            ProgressTimeRange.M1, ProgressTimeRange.M3, ProgressTimeRange.M6,
            ProgressTimeRange.Y1, ProgressTimeRange.ALL
        )
    }

    LaunchedEffect(subTab) {
        if (range !in allowedRanges) {
            range = allowedRanges.first()
        }
    }

    LaunchedEffect(range, subTab) {
        calorieRows = loadCalories(start, end)
        waterRows = loadWater(start, end)
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Прогрес", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(InputGray)
                    .clickable(onClick = onProfileClick),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = EmeraldTeal, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        SubTabBar(subTab, onSubTabChange)
        Spacer(Modifier.height(12.dp))
        RangeChips(
            selected = range,
            allowed = allowedRanges,
            onSelect = { range = it }
        )
        Spacer(Modifier.height(16.dp))
        if (subTab == ProgressSubTab.WEIGHT) {
            WeightTab(profile, weights, start, end)
        } else {
            NutritionTab(
                profile = profile,
                calorieRows = calorieRows,
                waterRows = waterRows,
                streak = streak,
                waterGoalMl = waterGoalMl,
                start = start,
                end = end,
                onMacroEdit = onMacroEdit
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SubTabBar(selected: ProgressSubTab, onSelect: (ProgressSubTab) -> Unit) {
    Surface(color = InputGray, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(4.dp)) {
            listOf(ProgressSubTab.WEIGHT to "Тегло", ProgressSubTab.NUTRITION to "Хранене").forEach { (tab, label) ->
                val on = selected == tab
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (on) PitchBlack else Color.Transparent)
                        .clickable { onSelect(tab) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = if (on) TextWhite else TextSilver, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun RangeChips(
    selected: ProgressTimeRange,
    allowed: List<ProgressTimeRange>,
    onSelect: (ProgressTimeRange) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        allowed.forEach { r ->
            val on = r == selected
            Surface(
                color = if (on) EmeraldTeal else InputGray,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.clickable { onSelect(r) }
            ) {
                Text(
                    r.label,
                    color = if (on) PitchBlack else TextSilver,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun WeightTab(
    profile: UserProfileEntity?,
    weights: List<WeightEntryEntity>,
    start: LocalDate,
    end: LocalDate
) {
    val filtered = weights.filter { it.dateEpochDay in start.toEpochDay()..end.toEpochDay() }
        .ifEmpty { weights }
    val current = filtered.lastOrNull()?.weightKg ?: profile?.weightKg ?: 0f
    val first = filtered.firstOrNull()?.weightKg ?: current
    val target = profile?.targetWeightKg ?: current
    val remaining = abs(target - current)
    val changed = first - current
    val changeLabel = when {
        abs(changed) < 0.05f -> "Без промяна"
        changed > 0 -> "Свалени ${BgFormat.decimalOne(changed)} kg"
        else -> "Качени ${BgFormat.decimalOne(-changed)} kg"
    }

    Text("${BgFormat.decimalOne(current)} kg", color = TextWhite, fontSize = 32.sp, fontWeight = FontWeight.Bold)
    Text(
        "Цел ${BgFormat.decimalOne(target)} kg · остават ${BgFormat.decimalOne(remaining)} kg",
        color = TextSilver,
        fontSize = 14.sp
    )
    Spacer(Modifier.height(16.dp))
    SectionCard {
        SectionHeader("Измервания")
        Spacer(Modifier.height(8.dp))
        WeightChart(
            entries = filtered,
            targetKg = target,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )
        Text(changeLabel, color = EmeraldTeal, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun WeightChart(entries: List<WeightEntryEntity>, targetKg: Float, modifier: Modifier) {
    val axisFmt = DateTimeFormatter.ofPattern("dd MMM", Locale("bg"))
    Column(modifier) {
        Canvas(Modifier.weight(1f).fillMaxWidth()) {
            if (entries.isEmpty()) return@Canvas
            val weights = entries.map { it.weightKg }
            val minW = (weights.minOrNull() ?: 0f).coerceAtMost(targetKg) - 2f
            val maxW = (weights.maxOrNull() ?: 0f).coerceAtLeast(targetKg) + 2f
            val range = (maxW - minW).coerceAtLeast(1f)
            val w = size.width
            val h = size.height
            fun yOf(v: Float) = h - ((v - minW) / range) * h
            val targetY = yOf(targetKg)
            drawLine(
                color = Color.Gray,
                start = Offset(0f, targetY),
                end = Offset(w, targetY),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
            )
            val path = Path()
            val fill = Path()
            entries.forEachIndexed { i, e ->
                val x = if (entries.size == 1) w / 2 else i.toFloat() / (entries.size - 1) * w
                val y = yOf(e.weightKg)
                if (i == 0) {
                    path.moveTo(x, y)
                    fill.moveTo(x, h)
                    fill.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fill.lineTo(x, y)
                }
            }
            fill.lineTo(w, h)
            fill.close()
            drawPath(fill, Brush.verticalGradient(listOf(EmeraldTeal.copy(alpha = 0.28f), Color.Transparent)))
            drawPath(path, EmeraldTeal, style = Stroke(width = 4f, cap = StrokeCap.Round))
            entries.forEachIndexed { i, e ->
                val x = if (entries.size == 1) w / 2 else i.toFloat() / (entries.size - 1) * w
                val y = yOf(e.weightKg)
                drawCircle(EmeraldTeal, radius = 5f, center = Offset(x, y))
            }
        }
        if (entries.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    LocalDate.ofEpochDay(entries.first().dateEpochDay).format(axisFmt),
                    color = TextSilver,
                    fontSize = 11.sp
                )
                Text(
                    LocalDate.ofEpochDay(entries.last().dateEpochDay).format(axisFmt),
                    color = TextSilver,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun NutritionTab(
    profile: UserProfileEntity?,
    calorieRows: List<DayCalorieRow>,
    waterRows: List<WaterDayRow>,
    streak: Int,
    waterGoalMl: Int,
    start: LocalDate,
    end: LocalDate,
    onMacroEdit: () -> Unit
) {
    val goal = profile?.dailyCalories ?: 2000
    val days = ProgressAnalytics.daysInRange(start, end)
    val logged = calorieRows.filter { it.calories > 0 }
    val avg = if (logged.isEmpty()) 0 else logged.sumOf { it.calories } / logged.size
    val under = goal - avg
    val inTargetDays = logged.count { ProgressAnalytics.calorieDayStatus(it.calories, goal) == CalorieDayStatus.ON_TARGET }
    val weekStart = end.with(DayOfWeek.MONDAY)
    val weekDays = (0..6).map { weekStart.plusDays(it.toLong()) }
    val calByDay = calorieRows.associateBy { it.dateEpochDay }
    val waterByDay = waterRows.associateBy { it.dateEpochDay }

    Text("Средно калории на ден", color = TextSilver, fontSize = 13.sp)
    Text(
        "${BgFormat.calories(avg)} / ${BgFormat.calories(goal)} kcal",
        color = TextWhite,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold
    )
    Text(
        if (under >= 0) "${BgFormat.calories(under)} kcal под целта"
        else "${BgFormat.calories(-under)} kcal над целта",
        color = FatOrange,
        fontSize = 14.sp
    )
    Spacer(Modifier.height(16.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        StatCell("${logged.size}/$days", "Записани дни")
        StatCell("$inTargetDays", "В целта")
        StatCell("🔥 $streak", "Серия")
    }
    Spacer(Modifier.height(16.dp))
    SectionCard {
        SectionHeader("Калории")
        Spacer(Modifier.height(8.dp))
        WeekCalorieBars(weekDays, calByDay, goal)
        Spacer(Modifier.height(10.dp))
        LegendRow(
            listOf(
                Color(0xFF4ADE80) to "В целта",
                Color(0xFF86EFAC) to "Под",
                Color(0xFF166534) to "Много под",
                FatOrange to "Много над",
                Color(0xFFFBBF24) to "Над",
                Color(0xFF4B5563) to "Без запис"
            )
        )
    }
    Spacer(Modifier.height(16.dp))
    AverageMacrosCard(profile, logged, onMacroEdit)
    Spacer(Modifier.height(16.dp))
    SectionCard {
        SectionHeader("Вода")
        Text(
            "${BgFormat.litersFromMl(if (waterRows.isEmpty()) 0 else waterRows.sumOf { it.totalMl } / waterRows.size.coerceAtLeast(1))} L средно / ден",
            color = TextSilver,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(8.dp))
        WeekWaterBars(weekDays, waterByDay, waterGoalMl)
        Spacer(Modifier.height(8.dp))
        LegendRow(
            listOf(
                WaterBlue to "В целта",
                Color(0xFF1E3A5F) to "Под",
                Color(0xFF4B5563) to "Без запис"
            )
        )
    }
}

@Composable
private fun StatCell(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, color = TextSilver, fontSize = 12.sp)
    }
}

@Composable
private fun WeekCalorieBars(
    days: List<LocalDate>,
    byDay: Map<Long, DayCalorieRow>,
    goal: Int
) {
    val labels = listOf("п", "в", "с", "ч", "п", "с", "н")
    val maxH = days.maxOfOrNull { (byDay[it.toEpochDay()]?.calories ?: 0) }?.coerceAtLeast(goal) ?: goal
    Column {
        Box(Modifier.fillMaxWidth().height(140.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val goalY = size.height - (goal.toFloat() / maxH.coerceAtLeast(1) * size.height)
                drawLine(
                    Color.Gray,
                    Offset(0f, goalY),
                    Offset(size.width, goalY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
                val slot = size.width / 7f
                days.forEachIndexed { i, d ->
                    val cal = byDay[d.toEpochDay()]?.calories ?: 0
                    val status = ProgressAnalytics.calorieDayStatus(cal, goal)
                    val color = when (status) {
                        CalorieDayStatus.ON_TARGET -> Color(0xFF4ADE80)
                        CalorieDayStatus.UNDER -> Color(0xFF86EFAC)
                        CalorieDayStatus.WAY_UNDER -> Color(0xFF166534)
                        CalorieDayStatus.OVER -> Color(0xFFFBBF24)
                        CalorieDayStatus.WAY_OVER -> FatOrange
                        CalorieDayStatus.NO_LOG -> Color(0xFF4B5563)
                    }
                    val barH = if (cal <= 0) 6f else (cal.toFloat() / maxH) * size.height
                    val left = slot * i + slot * 0.28f
                    val width = slot * 0.44f
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(left, size.height - barH),
                        size = androidx.compose.ui.geometry.Size(width, barH)
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            labels.forEach { l ->
                Text(l, color = TextSilver, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
private fun WeekWaterBars(
    days: List<LocalDate>,
    byDay: Map<Long, WaterDayRow>,
    goalMl: Int
) {
    val labels = listOf("п", "в", "с", "ч", "п", "с", "н")
    val maxH = days.maxOfOrNull { byDay[it.toEpochDay()]?.totalMl ?: 0 }?.coerceAtLeast(goalMl) ?: goalMl
    Column {
        Box(Modifier.fillMaxWidth().height(110.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val goalY = size.height - (goalMl.toFloat() / maxH.coerceAtLeast(1) * size.height)
                drawLine(
                    Color.Gray,
                    Offset(0f, goalY),
                    Offset(size.width, goalY),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                )
                val slot = size.width / 7f
                days.forEachIndexed { i, d ->
                    val ml = byDay[d.toEpochDay()]?.totalMl ?: 0
                    val status = ProgressAnalytics.waterDayStatus(ml, goalMl)
                    val color = when (status) {
                        WaterDayStatus.ON_TARGET -> WaterBlue
                        WaterDayStatus.UNDER -> Color(0xFF1E3A5F)
                        WaterDayStatus.NO_LOG -> Color(0xFF4B5563)
                    }
                    val barH = if (ml <= 0) 6f else (ml.toFloat() / maxH) * size.height
                    val left = slot * i + slot * 0.28f
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(left, size.height - barH),
                        size = androidx.compose.ui.geometry.Size(slot * 0.44f, barH)
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            labels.forEach { l ->
                Text(l, color = TextSilver, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
private fun LegendRow(items: List<Pair<Color, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (c, l) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(c))
                        Text(" $l", color = TextSilver, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AverageMacrosCard(
    profile: UserProfileEntity?,
    logged: List<DayCalorieRow>,
    onMacroEdit: () -> Unit
) {
    val days = logged.size.coerceAtLeast(1)
    val avgC = if (logged.isEmpty()) 0f else logged.sumOf { it.carbs.toDouble() }.toFloat() / days
    val avgP = if (logged.isEmpty()) 0f else logged.sumOf { it.protein.toDouble() }.toFloat() / days
    val avgF = if (logged.isEmpty()) 0f else logged.sumOf { it.fats.toDouble() }.toFloat() / days
    val avgKcal = if (logged.isEmpty()) 0 else logged.sumOf { it.calories } / days
    val shares = ProgressAnalytics.macroShares(avgC, avgP, avgF, avgKcal)
    val targetC = profile?.carbsPercent ?: 50
    val targetP = profile?.proteinPercent ?: 20
    val targetF = profile?.fatPercent ?: 30
    val inTarget = ProgressAnalytics.isMacroBalanceOnTarget(shares, targetC, targetP, targetF)

    fun arrow(actual: Int, target: Int): String = when (ProgressAnalytics.shareDirection(actual, target)) {
        TargetDirection.ABOVE -> "▲  "
        TargetDirection.BELOW -> "▼  "
        TargetDirection.ON -> "   "
    }

    SectionCard {
        SectionHeader("Средни макроси / ден")
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
                Canvas(Modifier.size(110.dp)) {
                    val stroke = 16.dp.toPx()
                    var start = -90f
                    if (shares.hasData) {
                        listOf(
                            CarbsRed to shares.carbsPct,
                            ProteinBlue to shares.proteinPct,
                            FatOrange to shares.fatPct,
                            Color.Gray to shares.otherPct
                        ).forEach { (col, pct) ->
                            if (pct > 0) {
                                val sweep = pct / 100f * 360f
                                drawArc(col, start, sweep, false, style = Stroke(stroke))
                                start += sweep
                            }
                        }
                    } else {
                        drawArc(Color.Gray.copy(alpha = 0.4f), -90f, 360f, false, style = Stroke(stroke))
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        when {
                            !shares.hasData -> "—"
                            inTarget -> "В"
                            else -> "Извън"
                        },
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text("целта", color = TextSilver, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.size(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${arrow(shares.carbsPct, targetC)}${shares.carbsPct}%  Въглехи", color = CarbsRed, fontSize = 13.sp)
                Text("${arrow(shares.proteinPct, targetP)}${shares.proteinPct}%  Протеини", color = ProteinBlue, fontSize = 13.sp)
                Text("${arrow(shares.fatPct, targetF)}${shares.fatPct}%  Мазнини", color = FatOrange, fontSize = 13.sp)
                if (shares.otherPct > 0) Text("   ${shares.otherPct}%  Други", color = TextSilver, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        MacroBarLine("Въглехидрати", avgC, profile?.carbsGrams ?: 0f, CarbsRed)
        MacroBarLine("Протеини", avgP, profile?.proteinGrams ?: 0f, ProteinBlue)
        MacroBarLine("Мазнини", avgF, profile?.fatGrams ?: 0f, FatOrange)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Твоят план — $targetC / $targetP / $targetF",
                color = TextSilver,
                fontSize = 13.sp
            )
            Text("Редактиране >", color = EmeraldTeal, modifier = Modifier.clickable(onClick = onMacroEdit))
        }
    }
}

@Composable
private fun MacroBarLine(label: String, current: Float, target: Float, color: Color) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = color, fontSize = 13.sp)
            Text("${current.toInt()} / ${target.toInt()} g", color = TextWhite, fontSize = 13.sp)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(InputGray)) {
            Box(
                Modifier
                    .fillMaxWidth(if (target > 0) (current / target).coerceIn(0f, 1f) else 0f)
                    .height(6.dp)
                    .background(color)
            )
        }
    }
}

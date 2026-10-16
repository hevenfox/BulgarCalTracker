package com.bulgarcal.tracker.ui.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulgarcal.tracker.R
import com.bulgarcal.tracker.data.DayTotalsRow
import com.bulgarcal.tracker.data.GoalType
import com.bulgarcal.tracker.data.PaceOption
import com.bulgarcal.tracker.data.UserProfileEntity
import com.bulgarcal.tracker.data.WaterLogEntity
import com.bulgarcal.tracker.data.WeightEntryEntity
import com.bulgarcal.tracker.domain.CalculationEngine
import com.bulgarcal.tracker.domain.MacroShares
import com.bulgarcal.tracker.domain.MealType
import com.bulgarcal.tracker.domain.ProgressAnalytics
import com.bulgarcal.tracker.domain.TargetDirection
import com.bulgarcal.tracker.ui.components.BgFormat
import com.bulgarcal.tracker.ui.components.MacroColumnBar
import com.bulgarcal.tracker.ui.components.SectionCard
import com.bulgarcal.tracker.ui.components.SectionHeader
import com.bulgarcal.tracker.ui.theme.CardOutline
import com.bulgarcal.tracker.ui.theme.CarbsRed
import com.bulgarcal.tracker.ui.theme.EmeraldTeal
import com.bulgarcal.tracker.ui.theme.FatOrange
import com.bulgarcal.tracker.ui.theme.InputGray
import com.bulgarcal.tracker.ui.theme.OtherGray
import com.bulgarcal.tracker.ui.theme.ProteinBlue
import com.bulgarcal.tracker.ui.theme.TextSilver
import com.bulgarcal.tracker.ui.theme.TextWhite
import com.bulgarcal.tracker.ui.theme.TileFill
import com.bulgarcal.tracker.ui.theme.WarningAmber
import com.bulgarcal.tracker.ui.theme.WaterBlue
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val RingTrack = Color(0xFF2F3432)

// ─────────────────────────────────────────────────────────────────────────────
// Целево табло (calorie ring + macro columns)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun CalorieRingSection(
    eaten: Int,
    budget: Int,
    percentOfGoal: Int,
    carbs: Float,
    protein: Float,
    fats: Float,
    carbsTarget: Float,
    proteinTarget: Float,
    fatTarget: Float
) {
    val over = budget > 0 && eaten > budget
    val remaining = (budget - eaten).coerceAtLeast(0)
    val progress = if (budget > 0) (eaten.toFloat() / budget).coerceIn(0f, 1f) else 0f
    val arcColor = if (over) WarningAmber else EmeraldTeal

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
            Canvas(Modifier.size(200.dp)) {
                if (eaten <= 0) {
                    // empty day: ring made of short radial ticks (reference)
                    val ticks = 60
                    val outer = size.minDimension / 2f
                    for (i in 0 until ticks) {
                        val a = Math.toRadians(i * 360.0 / ticks - 90.0)
                        val len = if (i % 5 == 0) 13.dp.toPx() else 9.dp.toPx()
                        val cosA = cos(a).toFloat()
                        val sinA = sin(a).toFloat()
                        drawLine(
                            color = Color(0xFF3A403D),
                            start = Offset(center.x + cosA * (outer - len), center.y + sinA * (outer - len)),
                            end = Offset(center.x + cosA * outer, center.y + sinA * outer),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                } else {
                    val stroke = 10.dp.toPx()
                    val topLeft = Offset(stroke / 2f, stroke / 2f)
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = RingTrack,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke)
                    )
                    drawArc(
                        color = arcColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when {
                    eaten <= 0 -> {
                        Text(BgFormat.calories(budget), color = TextWhite, fontSize = 44.sp, fontWeight = FontWeight.Medium)
                        Text("kcal за деня", color = TextSilver, fontSize = 15.sp)
                    }
                    over -> {
                        Text(BgFormat.calories(eaten - budget), color = WarningAmber, fontSize = 44.sp, fontWeight = FontWeight.Medium)
                        Text("kcal над бюджета", color = TextSilver, fontSize = 15.sp)
                        Text("от ${BgFormat.calories(budget)} бюджет", color = TextSilver.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                    else -> {
                        Text(BgFormat.calories(remaining), color = TextWhite, fontSize = 44.sp, fontWeight = FontWeight.Medium)
                        Text("kcal остават", color = TextSilver, fontSize = 15.sp)
                        Text("от ${BgFormat.calories(budget)} бюджет", color = TextSilver.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MacroColumnBar("Въглехидрати", carbs, carbsTarget, CarbsRed, Modifier.weight(1f))
            MacroColumnBar("Протеини", protein, proteinTarget, ProteinBlue, Modifier.weight(1f))
            MacroColumnBar("Мазнини", fats, fatTarget, FatOrange, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = TextWhite, fontWeight = FontWeight.SemiBold)) {
                    append(BgFormat.calories(eaten))
                }
                append(" kcal изядени")
                withStyle(SpanStyle(color = TextSilver.copy(alpha = 0.5f))) { append("    ·    ") }
                withStyle(SpanStyle(color = TextWhite, fontWeight = FontWeight.SemiBold)) {
                    append("$percentOfGoal%")
                }
                append(" от целта")
            },
            color = TextSilver,
            fontSize = 14.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ХРАНЕНИЯ
// ─────────────────────────────────────────────────────────────────────────────

@DrawableRes
private fun mealIcon(meal: MealType): Int = when (meal) {
    MealType.BREAKFAST -> R.drawable.meal_breakfast
    MealType.LUNCH -> R.drawable.meal_lunch
    MealType.SNACK -> R.drawable.meal_snack
    MealType.DINNER -> R.drawable.meal_dinner
}

private fun mealTint(meal: MealType): Color = when (meal) {
    MealType.BREAKFAST -> Color(0xFF2A241E)
    MealType.LUNCH -> Color(0xFF1F2A24)
    MealType.SNACK -> Color(0xFF262230)
    MealType.DINNER -> Color(0xFF1F2533)
}

@Composable
internal fun MealsSection(
    mealCals: Map<String, Int>,
    onOpenDiary: () -> Unit,
    onMealClick: (MealType) -> Unit
) {
    SectionCard {
        SectionHeader("Хранения", "Дневник >", onOpenDiary)
        Spacer(Modifier.height(14.dp))
        MealType.entries.toList().chunked(2).forEachIndexed { index, row ->
            if (index > 0) Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { meal ->
                    MealTile(
                        meal = meal,
                        calories = mealCals[meal.name] ?: 0,
                        onClick = { onMealClick(meal) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MealTile(
    meal: MealType,
    calories: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    // empty meals are dimmed, like the reference
    val dim = if (calories > 0) 1f else 0.55f
    Row(
        modifier
            .clip(shape)
            .background(TileFill)
            .border(1.dp, CardOutline, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(mealTint(meal)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(mealIcon(meal)),
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .alpha(dim)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                meal.label,
                color = TextWhite.copy(alpha = dim),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    BgFormat.calories(calories),
                    color = TextWhite.copy(alpha = dim),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    " kcal",
                    color = TextSilver.copy(alpha = dim),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// КАЛОРИЙНО СЪОТНОШЕНИЕ
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun MacroRatioSection(
    totals: DayTotalsRow,
    profile: UserProfileEntity?,
    onDetails: () -> Unit,
    onMacroEdit: () -> Unit
) {
    val shares = ProgressAnalytics.macroShares(totals.carbs, totals.protein, totals.fats, totals.calories)
    val targetC = profile?.carbsPercent ?: 50
    val targetP = profile?.proteinPercent ?: 20
    val targetF = profile?.fatPercent ?: 30
    val onTarget = ProgressAnalytics.isMacroBalanceOnTarget(shares, targetC, targetP, targetF)

    SectionCard {
        SectionHeader("Калорийно съотношение", "Детайли >", onDetails)
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MacroDonut(
                shares = shares,
                targetC = targetC,
                targetP = targetP,
                targetF = targetF,
                onTarget = onTarget,
                modifier = Modifier.size(140.dp)
            )
            Spacer(Modifier.width(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                RatioLegendRow("Въглехи", CarbsRed, shares.hasData, shares.carbsPct, targetC)
                RatioLegendRow("Протеини", ProteinBlue, shares.hasData, shares.proteinPct, targetP)
                RatioLegendRow("Мазнини", FatOrange, shares.hasData, shares.fatPct, targetF)
                if (shares.hasData && shares.otherPct > 0) {
                    RatioLegendRow("Други", OtherGray, true, shares.otherPct, null)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = CardOutline)
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TargetSplitRow(targetC, targetP, targetF)
            Text(
                "Редактиране >",
                color = EmeraldTeal,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onMacroEdit)
            )
        }
    }
}

/** "Цел — ● 50 / ● 20 / ● 30" — shared look with Прогрес → "Твоят план". */
@Composable
internal fun TargetSplitRow(carbsPct: Int, proteinPct: Int, fatPct: Int, label: String = "Цел") {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label — ", color = TextSilver, fontSize = 14.sp)
        SplitItem(CarbsRed, carbsPct.toString())
        Text(" / ", color = TextSilver, fontSize = 14.sp)
        SplitItem(ProteinBlue, proteinPct.toString())
        Text(" / ", color = TextSilver, fontSize = 14.sp)
        SplitItem(FatOrange, fatPct.toString())
    }
}

@Composable
private fun SplitItem(color: Color, value: String) {
    Box(Modifier.size(9.dp).clip(CircleShape).background(color))
    Spacer(Modifier.width(6.dp))
    Text(value, color = TextWhite, fontSize = 14.sp)
}

@Composable
private fun MacroDonut(
    shares: MacroShares,
    targetC: Int,
    targetP: Int,
    targetF: Int,
    onTarget: Boolean,
    modifier: Modifier = Modifier
) {
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val outerStroke = 4.dp.toPx()
            val donutStroke = 16.dp.toPx()
            val gap = 6.dp.toPx()

            // thin outer ring = the target split from the profile
            val outerTopLeft = Offset(outerStroke / 2f, outerStroke / 2f)
            val outerSize = Size(size.width - outerStroke, size.height - outerStroke)
            var start = -90f
            listOf(CarbsRed to targetC, ProteinBlue to targetP, FatOrange to targetF).forEach { (color, pct) ->
                val sweep = 360f * pct / 100f
                drawArc(
                    color = color.copy(alpha = if (shares.hasData) 0.35f else 0.45f),
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = outerTopLeft,
                    size = outerSize,
                    style = Stroke(width = outerStroke)
                )
                start += sweep
            }

            // thick donut = what was actually eaten
            if (shares.hasData) {
                val inset = outerStroke + gap + donutStroke / 2f
                val topLeft = Offset(inset, inset)
                val donutSize = Size(size.width - inset * 2f, size.height - inset * 2f)
                var s = -90f
                listOf(
                    CarbsRed to shares.carbsPct,
                    ProteinBlue to shares.proteinPct,
                    FatOrange to shares.fatPct,
                    OtherGray to shares.otherPct
                ).forEach { (color, pct) ->
                    if (pct > 0) {
                        val sweep = 360f * pct / 100f
                        drawArc(
                            color = color,
                            startAngle = s,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = donutSize,
                            style = Stroke(width = donutStroke)
                        )
                        s += sweep
                    }
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when {
                !shares.hasData -> {
                    Text("—", color = TextSilver, fontSize = 18.sp)
                    Text("няма данни", color = TextSilver.copy(alpha = 0.7f), fontSize = 11.sp)
                }
                onTarget -> Text("В целта", color = EmeraldTeal, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                else -> {
                    Text("Извън", color = WarningAmber, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("целта", color = TextSilver, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun RatioLegendRow(
    label: String,
    color: Color,
    hasData: Boolean,
    percent: Int,
    targetPercent: Int?
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (hasData) {
            val arrow = when {
                targetPercent == null -> ""
                else -> when (ProgressAnalytics.shareDirection(percent, targetPercent)) {
                    TargetDirection.ABOVE -> "▲"
                    TargetDirection.BELOW -> "▼"
                    TargetDirection.ON -> ""
                }
            }
            Box(Modifier.width(16.dp)) {
                Text(arrow, color = WarningAmber, fontSize = 11.sp)
            }
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
            Spacer(Modifier.width(8.dp))
            Text(
                "$percent%",
                color = TextWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(46.dp)
            )
        } else {
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
            Spacer(Modifier.width(10.dp))
        }
        Text(label, color = TextSilver, fontSize = 15.sp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ВОДА
// ─────────────────────────────────────────────────────────────────────────────

@Composable
internal fun WaterSection(
    waterLogs: List<WaterLogEntity>,
    incrementMl: Int,
    goalMl: Int,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    onChangeSize: () -> Unit,
    onInfo: () -> Unit
) {
    val totalMl = waterLogs.sumOf { it.milliliters }
    val pct = if (goalMl > 0) ((totalMl * 100f) / goalMl).roundToInt().coerceAtMost(100) else 0
    val drinks = waterLogs.size
    val drinksLabel = when (drinks) {
        1 -> "1 напитка днес"
        else -> "$drinks напитки днес"
    }

    SectionCard {
        SectionHeader("Вода")
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(88.dp)) {
                Canvas(Modifier.size(80.dp)) {
                    val stroke = 9.dp.toPx()
                    val topLeft = Offset(stroke / 2f, stroke / 2f)
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = RingTrack,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke)
                    )
                    if (pct > 0) {
                        drawArc(
                            color = WaterBlue,
                            startAngle = -90f,
                            sweepAngle = 360f * (pct / 100f),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                }
                Text("$pct%", color = WaterBlue, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = TextWhite, fontSize = 30.sp, fontWeight = FontWeight.Bold)) {
                                append(BgFormat.litersFromMl(totalMl))
                            }
                            withStyle(SpanStyle(color = TextSilver, fontSize = 16.sp)) {
                                append(" / ${BgFormat.litersFromMl(goalMl)} L")
                            }
                        }
                    )
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = "Информация",
                        tint = TextSilver,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(18.dp)
                            .clickable(onClick = onInfo)
                    )
                }
                Text(drinksLabel, color = TextSilver, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            WaterStepButton(Icons.Default.Remove, "Премахни последната напитка", onRemove, enabled = totalMl > 0)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onChangeSize)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("$incrementMl ml", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text("докоснете за промяна ▾", color = TextSilver, fontSize = 12.sp)
            }
            WaterStepButton(Icons.Default.Add, "Добави напитка", onAdd, filled = true)
        }
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = CardOutline)
        // decoration only: no click handler, semantically disabled
        Row(
            Modifier
                .fillMaxWidth()
                .semantics { disabled() }
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
            Text(
                "Храносмилателната система зависи от нея",
                color = TextSilver,
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp)
            )
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSilver.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun WaterStepButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    filled: Boolean = false
) {
    val shape = RoundedCornerShape(16.dp)
    val bg = when {
        filled -> WaterBlue
        enabled -> Color(0xFF26313A)
        else -> Color(0xFF222A2F)
    }
    val tint = when {
        filled -> TextWhite
        enabled -> WaterBlue
        else -> WaterBlue.copy(alpha = 0.35f)
    }
    Box(
        Modifier
            .size(56.dp)
            .clip(shape)
            .background(bg)
            .semantics { role = Role.Button }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(26.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ЦЕЛЕВО ТЕГЛО
// ─────────────────────────────────────────────────────────────────────────────

private val targetDateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale("bg"))

@Composable
internal fun TargetWeightSection(
    selectedDate: LocalDate,
    profile: UserProfileEntity?,
    entries: List<WeightEntryEntity>,
    onProgress: () -> Unit,
    onRecord: () -> Unit
) {
    val current = entries.lastOrNull()?.weightKg ?: profile?.weightKg ?: 0f
    val start = entries.firstOrNull()?.weightKg ?: profile?.weightKg ?: current
    val target = profile?.targetWeightKg ?: current
    val remaining = abs(target - current)

    // direction-aware: works for losing and gaining; overshoot = 100%, wrong way = 0%
    val totalSpan = target - start
    val progressToward = if (abs(totalSpan) < 0.1f) 1f
    else ((current - start) / totalSpan).coerceIn(0f, 1f)

    val goal = runCatching { GoalType.valueOf(profile?.goal ?: "") }.getOrNull() ?: GoalType.MAINTAIN
    val pace = runCatching { PaceOption.valueOf(profile?.pace ?: "") }.getOrNull() ?: PaceOption.SLOW

    val delta = if (entries.size >= 2) entries.last().weightKg - entries[entries.size - 2].weightKg else 0f
    val movingToward = delta != 0f && (delta > 0f) == (target > current)

    val loggedOnDay = entries.any { it.dateEpochDay == selectedDate.toEpochDay() }
    val weeks = CalculationEngine.weeksToGoal(current, target, goal, pace)
    val targetDateLabel = weeks?.let {
        LocalDate.now().plusWeeks(it.toLong()).format(targetDateFmt).replace(".", "")
    }

    SectionCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text("ЦЕЛЕВО ТЕГЛО", color = TextSilver, fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
                Text(
                    "  ·  ${pace.homeLabel(goal)}",
                    color = TextSilver,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                "Прогрес >",
                color = EmeraldTeal,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onProgress)
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                Text(BgFormat.decimalOne(current), color = TextWhite, fontSize = 36.sp, fontWeight = FontWeight.Medium)
                Text(" kg", color = TextSilver, fontSize = 18.sp, modifier = Modifier.padding(bottom = 5.dp))
                if (delta != 0f) {
                    Text(
                        "   ${if (delta > 0f) "↗" else "↘"} ${BgFormat.decimalOne(abs(delta))}",
                        color = if (movingToward) EmeraldTeal else WarningAmber,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 7.dp)
                    )
                }
            }
            MiniSparkline(entries = entries, modifier = Modifier.size(width = 96.dp, height = 40.dp))
        }
        Spacer(Modifier.height(10.dp))
        GoalProgressBar(progress = progressToward)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${BgFormat.decimalOne(start)} старт", color = TextSilver, fontSize = 12.sp)
            Text(
                buildAnnotatedString {
                    append("остават ")
                    withStyle(SpanStyle(color = TextWhite, fontWeight = FontWeight.SemiBold)) {
                        append("${BgFormat.decimalOne(remaining)} kg")
                    }
                    append(" · ${(progressToward * 100).roundToInt()}%")
                },
                color = TextSilver,
                fontSize = 12.sp
            )
            Text("${BgFormat.decimalOne(target)} цел", color = TextSilver, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = CardOutline)
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loggedOnDay) {
                Text(
                    targetDateLabel?.let { "Цел $it" } ?: "Записано",
                    color = TextSilver,
                    fontSize = 14.sp
                )
            } else {
                Text(
                    if (selectedDate == LocalDate.now()) "Днес няма измерване" else "Няма измерване за деня",
                    color = EmeraldTeal,
                    fontSize = 14.sp
                )
            }
            Text(
                "+ Запиши тегло",
                color = EmeraldTeal,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onRecord)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

/** Mint bar with a hollow ring marker at the current progress (reference). */
@Composable
private fun GoalProgressBar(progress: Float) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(18.dp)
    ) {
        val barH = 8.dp.toPx()
        val r = 9.dp.toPx()
        val y = size.height / 2f
        val left = r
        val right = size.width - r
        drawLine(RingTrack, Offset(left, y), Offset(right, y), strokeWidth = barH, cap = StrokeCap.Round)
        val x = left + (right - left) * progress.coerceIn(0f, 1f)
        if (progress > 0f) {
            drawLine(EmeraldTeal, Offset(left, y), Offset(x, y), strokeWidth = barH, cap = StrokeCap.Round)
        }
        drawCircle(Color(0xFF1E1E1E), radius = r - 1.5.dp.toPx(), center = Offset(x, y))
        drawCircle(EmeraldTeal, radius = r - 1.5.dp.toPx(), center = Offset(x, y), style = Stroke(width = 3.dp.toPx()))
    }
}

@Composable
private fun MiniSparkline(
    entries: List<WeightEntryEntity>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val pts = entries.takeLast(8)
        if (pts.isEmpty()) return@Canvas
        val pad = 4.dp.toPx()
        val w = size.width - pad * 2f
        val h = size.height - pad * 2f
        val weights = pts.map { it.weightKg }
        val minW = weights.min()
        val maxW = weights.max()
        val range = (maxW - minW).coerceAtLeast(0.5f)
        fun px(i: Int) = if (pts.size == 1) size.width / 2f else pad + i.toFloat() / (pts.size - 1) * w
        fun py(v: Float) = pad + h - ((v - minW) / range) * h
        val path = Path()
        pts.forEachIndexed { i, e ->
            if (i == 0) path.moveTo(px(i), py(e.weightKg)) else path.lineTo(px(i), py(e.weightKg))
        }
        drawPath(path, EmeraldTeal.copy(alpha = 0.85f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(EmeraldTeal, radius = 3.5.dp.toPx(), center = Offset(px(pts.lastIndex), py(pts.last().weightKg)))
    }
}

package com.bulgarcal.tracker.domain

import com.bulgarcal.tracker.data.DayCalorieRow
import java.time.LocalDate
import kotlin.math.abs

enum class CalorieDayStatus(val label: String) {
    ON_TARGET("В целта"),
    UNDER("Под"),
    WAY_UNDER("Много под"),
    WAY_OVER("Много над"),
    OVER("Над"),
    NO_LOG("Без запис");

    companion object {
        /** Legend order used in the reference. */
        val legendOrder = listOf(ON_TARGET, UNDER, WAY_UNDER, WAY_OVER, OVER, NO_LOG)
    }
}

enum class WaterDayStatus(val label: String) {
    ON_TARGET("В целта"),
    UNDER("Под"),
    NO_LOG("Без запис")
}

enum class ProgressTimeRange(val label: String, val days: Int?) {
    W1("1с", 7),
    M1("1м", 30),
    M3("3м", 90),
    M6("6м", 180),
    Y1("1г", 365),
    ALL("Всичко", null);

    companion object {
        /** Тегло tab has no 1-week chip. */
        val weightRanges = listOf(M1, M3, M6, Y1, ALL)
        val nutritionRanges = listOf(W1, M1, M3, M6, Y1, ALL)
    }
}

enum class ProgressSubTab { WEIGHT, NUTRITION }

enum class TargetDirection { BELOW, ON, ABOVE }

/** Share of eaten calories per macro, in whole percents that always sum to 100 when [hasData]. */
data class MacroShares(
    val carbsPct: Int,
    val proteinPct: Int,
    val fatPct: Int,
    val otherPct: Int,
    val hasData: Boolean
)

object ProgressAnalytics {

    /** First day of [range] when it ends on [end] (inclusive). ALL starts at epoch day 0. */
    fun rangeStart(end: LocalDate, range: ProgressTimeRange): LocalDate =
        range.days?.let { end.minusDays(it - 1L) } ?: LocalDate.ofEpochDay(0)

    @Deprecated("Misleading name, returns the range START. Use rangeStart().")
    fun dateRangeEnd(end: LocalDate, range: ProgressTimeRange): LocalDate = rangeStart(end, range)

    fun calorieDayStatus(intake: Int, goal: Int): CalorieDayStatus {
        if (intake <= 0) return CalorieDayStatus.NO_LOG
        if (goal <= 0) return CalorieDayStatus.ON_TARGET
        val ratio = intake.toFloat() / goal
        return when {
            ratio in 0.9f..1.1f -> CalorieDayStatus.ON_TARGET
            ratio in 0.75f..0.9f -> CalorieDayStatus.UNDER
            ratio < 0.75f -> CalorieDayStatus.WAY_UNDER
            ratio in 1.1f..1.25f -> CalorieDayStatus.OVER
            else -> CalorieDayStatus.WAY_OVER
        }
    }

    fun waterDayStatus(ml: Int, goalMl: Int): WaterDayStatus {
        if (ml <= 0) return WaterDayStatus.NO_LOG
        if (goalMl <= 0) return WaterDayStatus.ON_TARGET
        return if (ml >= goalMl) WaterDayStatus.ON_TARGET else WaterDayStatus.UNDER
    }

    /**
     * Consecutive logged days ending today. If today has no logs yet the streak is counted
     * from yesterday, so it does not drop to 0 every morning before the first meal.
     */
    fun loggingStreak(rows: List<DayCalorieRow>, through: LocalDate = LocalDate.now()): Int {
        val byDay = rows.associate { it.dateEpochDay to it.calories }
        var day = through
        if ((byDay[day.toEpochDay()] ?: 0) <= 0) day = day.minusDays(1)
        var streak = 0
        while ((byDay[day.toEpochDay()] ?: 0) > 0) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    fun daysInRange(start: LocalDate, end: LocalDate): Int =
        (end.toEpochDay() - start.toEpochDay() + 1).toInt().coerceAtLeast(1)

    fun loggedDays(rows: List<DayCalorieRow>): Int = rows.count { it.calories > 0 }

    fun daysOnTarget(rows: List<DayCalorieRow>, goal: Int): Int =
        rows.count { calorieDayStatus(it.calories, goal) == CalorieDayStatus.ON_TARGET }

    /** Average over days that have logs (reference: 1 of 7 days logged, 565 kcal => "565"). */
    fun averageCaloriesPerLoggedDay(rows: List<DayCalorieRow>): Int {
        val logged = rows.filter { it.calories > 0 }
        return if (logged.isEmpty()) 0 else logged.sumOf { it.calories } / logged.size
    }

    /**
     * Carbs / protein / fat share of eaten calories, plus "Други" (calories not explained by
     * the three macros). Largest-remainder rounding keeps the total at exactly 100.
     */
    fun macroShares(carbsG: Float, proteinG: Float, fatG: Float, totalKcal: Int): MacroShares {
        val carbsKcal = carbsG * 4f
        val proteinKcal = proteinG * 4f
        val fatKcal = fatG * 9f
        val macroKcal = carbsKcal + proteinKcal + fatKcal
        val denom = maxOf(totalKcal.toFloat(), macroKcal)
        if (denom <= 0f) return MacroShares(0, 0, 0, 0, false)

        val raw = listOf(carbsKcal, proteinKcal, fatKcal, (denom - macroKcal).coerceAtLeast(0f))
            .map { it / denom * 100f }
        val result = raw.map { it.toInt() }.toMutableList()
        var remainder = 100 - result.sum()
        for (i in raw.indices.sortedByDescending { raw[it] - result[it] }) {
            if (remainder <= 0) break
            result[i] += 1
            remainder--
        }
        return MacroShares(result[0], result[1], result[2], result[3], true)
    }

    /** "В целта" vs "Извън целта" for the donut centre label. */
    fun isMacroBalanceOnTarget(
        shares: MacroShares,
        carbsTargetPct: Int,
        proteinTargetPct: Int,
        fatTargetPct: Int,
        tolerancePts: Int = 10
    ): Boolean = shares.hasData &&
        abs(shares.carbsPct - carbsTargetPct) <= tolerancePts &&
        abs(shares.proteinPct - proteinTargetPct) <= tolerancePts &&
        abs(shares.fatPct - fatTargetPct) <= tolerancePts

    /** Arrow next to a macro share in the legend (▲ above target, ▼ below). */
    fun shareDirection(actualPct: Int, targetPct: Int, tolerancePts: Int = 3): TargetDirection = when {
        actualPct > targetPct + tolerancePts -> TargetDirection.ABOVE
        actualPct < targetPct - tolerancePts -> TargetDirection.BELOW
        else -> TargetDirection.ON
    }
}

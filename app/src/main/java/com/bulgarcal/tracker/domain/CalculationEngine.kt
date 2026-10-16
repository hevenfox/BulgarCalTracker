package com.bulgarcal.tracker.domain

import com.bulgarcal.tracker.data.ActivityLevel
import com.bulgarcal.tracker.data.GoalType
import com.bulgarcal.tracker.data.PaceOption
import com.bulgarcal.tracker.data.Sex
import kotlin.math.abs
import kotlin.math.roundToInt

object CalculationEngine {

    fun calculateBmr(sex: Sex, weightKg: Float, heightCm: Float, ageYears: Int): Float {
        val base = 10f * weightKg + 6.25f * heightCm - 5f * ageYears
        return when (sex) {
            Sex.MALE -> base + 5f
            Sex.FEMALE -> base - 161f
        }
    }

    fun activityFactor(level: ActivityLevel): Float = when (level) {
        ActivityLevel.SEDENTARY -> 1.2f
        ActivityLevel.LIGHT -> 1.375f
        ActivityLevel.MODERATE -> 1.55f
        ActivityLevel.VERY_ACTIVE -> 1.725f
    }

    fun calculateTdee(bmr: Float, level: ActivityLevel): Float = bmr * activityFactor(level)

    fun calorieAdjustment(goal: GoalType, pace: PaceOption): Int = when (goal) {
        GoalType.LOSE_WEIGHT -> when (pace) {
            PaceOption.SLOW -> -250
            PaceOption.MEDIUM -> -500
        }
        GoalType.MAINTAIN -> 0
        GoalType.GAIN_WEIGHT -> when (pace) {
            PaceOption.SLOW -> 250
            PaceOption.MEDIUM -> 500
        }
    }

    fun dailyCalorieTarget(tdee: Float, goal: GoalType, pace: PaceOption): Int {
        val adjusted = tdee + calorieAdjustment(goal, pace)
        return adjusted.roundToInt().coerceIn(1075, 3230)
    }

    data class MacroTargets(
        val calories: Int,
        val carbsGrams: Float,
        val proteinGrams: Float,
        val fatGrams: Float,
        val carbsPercent: Int = 50,
        val proteinPercent: Int = 20,
        val fatPercent: Int = 30
    )

    fun defaultMacroTargets(calories: Int): MacroTargets {
        val carbs = (calories * 0.50f) / 4f
        val protein = (calories * 0.20f) / 4f
        val fat = (calories * 0.30f) / 9f
        return MacroTargets(calories, carbs, protein, fat)
    }

    fun macroTargetsFromPercents(
        calories: Int,
        carbsPercent: Int,
        proteinPercent: Int,
        fatPercent: Int
    ): MacroTargets {
        require(carbsPercent + proteinPercent + fatPercent == 100)
        val carbs = (calories * carbsPercent / 100f) / 4f
        val protein = (calories * proteinPercent / 100f) / 4f
        val fat = (calories * fatPercent / 100f) / 9f
        return MacroTargets(calories, carbs, protein, fat, carbsPercent, proteinPercent, fatPercent)
    }

    /** kg per week from pace */
    fun paceKgPerWeek(pace: PaceOption): Float = when (pace) {
        PaceOption.SLOW -> 0.25f
        PaceOption.MEDIUM -> 0.50f
    }

    fun weeksToGoal(
        currentKg: Float,
        targetKg: Float,
        goal: GoalType,
        pace: PaceOption
    ): Int? {
        if (goal == GoalType.MAINTAIN) return null
        val diff = abs(targetKg - currentKg)
        if (diff < 0.1f) return 0
        val weekly = paceKgPerWeek(pace)
        if (weekly <= 0f) return null
        return (diff / weekly).roundToInt().coerceAtLeast(1)
    }

    fun remainingWeeks(
        currentKg: Float,
        targetKg: Float,
        paceKgPerWeek: Float
    ): Int? {
        val diff = abs(targetKg - currentKg)
        if (diff < 0.1f || paceKgPerWeek <= 0f) return 0
        return (diff / paceKgPerWeek).roundToInt().coerceAtLeast(1)
    }

    /** Daily water goal: 35 ml per kg, rounded to 100 ml, between 1.5 and 4.5 L. */
    fun waterGoalMl(weightKg: Float): Int {
        val rounded = (weightKg * 35f / 100f).roundToInt() * 100
        return rounded.coerceIn(1500, 4500)
    }

    /** BJ Devine ideal body weight (kg) */
    fun idealWeightDevineKg(sex: Sex, heightCm: Float): Float {
        val inchesOver5ft = (heightCm / 2.54f - 60f).coerceAtLeast(0f)
        return when (sex) {
            Sex.MALE -> 50f + 2.3f * inchesOver5ft
            Sex.FEMALE -> 45.5f + 2.3f * inchesOver5ft
        }
    }
}

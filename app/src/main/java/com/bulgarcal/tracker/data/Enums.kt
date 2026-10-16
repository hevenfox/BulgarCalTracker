package com.bulgarcal.tracker.data

import java.util.Locale

enum class GoalType { LOSE_WEIGHT, MAINTAIN, GAIN_WEIGHT }

enum class Sex { MALE, FEMALE }

enum class ActivityLevel(val label: String, val factor: Float) {
    SEDENTARY("Заседнал начин на живот", 1.2f),
    LIGHT("Спортна активност 1–3 пъти седмично", 1.375f),
    MODERATE("Спортна активност 3–5 пъти седмично", 1.55f),
    VERY_ACTIVE("Спортна активност 6–7 пъти седмично", 1.725f)
}

enum class PaceOption(val label: String, val kgPerWeek: Float) {
    SLOW("-0.25 / +0.25 кг/седм.", 0.25f),
    MEDIUM("-0.50 / +0.50 кг/седм.", 0.50f);

    fun displayForGoal(goal: GoalType): String = when (goal) {
        GoalType.LOSE_WEIGHT -> when (this) {
            SLOW -> "-0.25 кг/седм."
            MEDIUM -> "-0.50 кг/седм."
        }
        GoalType.GAIN_WEIGHT -> when (this) {
            SLOW -> "+0.25 кг/седм."
            MEDIUM -> "+0.50 кг/седм."
        }
        GoalType.MAINTAIN -> "Поддържане"
    }

    private fun kgText(): String = String.format(Locale("bg"), "%.2f", kgPerWeek)

    /** Home → ЦЕЛЕВО ТЕГЛО header, e.g. "качване 0,25 kg/седм." */
    fun homeLabel(goal: GoalType): String = when (goal) {
        GoalType.LOSE_WEIGHT -> "сваляне ${kgText()} kg/седм."
        GoalType.GAIN_WEIGHT -> "качване ${kgText()} kg/седм."
        GoalType.MAINTAIN -> "поддържане"
    }

    /** Profile screen → Цел, e.g. "Качване на 0,25kg на седмица" */
    fun profileLabel(goal: GoalType): String = when (goal) {
        GoalType.LOSE_WEIGHT -> "Сваляне на ${kgText()}kg на седмица"
        GoalType.GAIN_WEIGHT -> "Качване на ${kgText()}kg на седмица"
        GoalType.MAINTAIN -> "Поддържане на теглото"
    }
}

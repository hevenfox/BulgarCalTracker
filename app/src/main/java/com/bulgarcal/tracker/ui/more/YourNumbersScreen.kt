package com.bulgarcal.tracker.ui.more

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulgarcal.tracker.data.ActivityLevel
import com.bulgarcal.tracker.data.GoalType
import com.bulgarcal.tracker.data.PaceOption
import com.bulgarcal.tracker.data.Sex
import com.bulgarcal.tracker.data.UserProfileEntity
import com.bulgarcal.tracker.domain.CalculationEngine
import com.bulgarcal.tracker.ui.components.BgFormat
import com.bulgarcal.tracker.ui.components.FormScreenInsets
import com.bulgarcal.tracker.ui.components.ScreenTopBar
import com.bulgarcal.tracker.ui.theme.TextSilver
import com.bulgarcal.tracker.ui.theme.TextWhite
import java.time.LocalDate
import java.time.Period
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun YourNumbersScreen(
    profile: UserProfileEntity?,
    currentWeightKg: Float?,
    onBack: () -> Unit
) {
    FormScreenInsets {
        ScreenTopBar("Вашите числа", onBack = onBack)
        if (profile == null) {
            Text("Няма запазен профил.", color = TextSilver, modifier = Modifier.padding(16.dp))
            return@FormScreenInsets
        }
        val sex = runCatching { Sex.valueOf(profile.sex) }.getOrDefault(Sex.MALE)
        val activity = runCatching { ActivityLevel.valueOf(profile.activityLevel) }
            .getOrDefault(ActivityLevel.LIGHT)
        val goal = runCatching { GoalType.valueOf(profile.goal) }.getOrDefault(GoalType.MAINTAIN)
        val pace = runCatching { PaceOption.valueOf(profile.pace) }.getOrDefault(PaceOption.SLOW)
        val birth = LocalDate.ofEpochDay(profile.birthDateEpochDay)
        val age = Period.between(birth, LocalDate.now()).years.coerceAtLeast(1)
        val weight = currentWeightKg ?: profile.weightKg
        val bmr = CalculationEngine.calculateBmr(sex, weight, profile.heightCm, age)
        val tdee = CalculationEngine.calculateTdee(bmr, activity)
        val ideal = CalculationEngine.idealWeightDevineKg(sex, profile.heightCm)
        val kgToGoal = abs(profile.targetWeightKg - weight)
        val weeks = CalculationEngine.weeksToGoal(weight, profile.targetWeightKg, goal, pace)

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            NumberBlock(
                title = "БМ",
                body = "Базовият ти метаболизъм е ${"%.1f".format(bmr)} kcal. Това са необходимите калории за поддържане на основните телесни функции, без да се включва физическа активност."
            )
            NumberBlock(
                title = "TDEE",
                body = "Дневният ти енергоразход (Total Daily Energy Expenditure) е ${"%.1f".format(tdee)} kcal. Това са изразходваните калории на ден спрямо физическата активност."
            )
            NumberBlock(
                title = "Метрика",
                body = "За изчисляване на БМ и TDEE е използвана формула на Мифин-Сейнт Жур.\nМъже БМ = (10 × тегло) + (6,25 × ръст) - (5 × години) + 5\nЖени БМ = (10 × тегло) + (6,25 × ръст) - (5 × години) - 161"
            )
            NumberBlock(
                title = "Идеално тегло",
                body = "Идеалното ти тегло е ${ideal.roundToInt()} kg.\nЗа изчисляване на идеално тегло е използвана формула на BJ Devine."
            )
            NumberBlock(
                title = "Килограми до целта",
                body = "${BgFormat.decimalOne(kgToGoal)} kg"
            )
            NumberBlock(
                title = "Седмици до целта",
                body = weeks?.let { "$it седмици" } ?: "—"
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NumberBlock(title: String, body: String) {
    Text(title, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(top = 16.dp))
    Spacer(Modifier.height(6.dp))
    Text(body, color = TextSilver, fontSize = 14.sp, lineHeight = 21.sp)
}

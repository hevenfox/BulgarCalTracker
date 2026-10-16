package com.bulgarcal.tracker.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulgarcal.tracker.data.ActivityLevel
import com.bulgarcal.tracker.data.GoalType
import com.bulgarcal.tracker.data.PaceOption
import com.bulgarcal.tracker.data.Sex
import com.bulgarcal.tracker.data.UserProfileEntity
import com.bulgarcal.tracker.ui.components.FormScreenInsets
import com.bulgarcal.tracker.ui.components.ScreenTopBar
import com.bulgarcal.tracker.ui.components.TealButton
import com.bulgarcal.tracker.ui.theme.BorderGray
import com.bulgarcal.tracker.ui.theme.EmeraldTeal
import com.bulgarcal.tracker.ui.theme.InputGray
import com.bulgarcal.tracker.ui.theme.TextSilver
import com.bulgarcal.tracker.ui.theme.TextWhite
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    profile: UserProfileEntity?,
    onBack: () -> Unit,
    onSave: (Sex, Float, Float, Float, ActivityLevel, GoalType, PaceOption, LocalDate) -> Unit
) {
    if (profile == null) {
        FormScreenInsets {
            ScreenTopBar("Профил", onBack = onBack)
            Text("Няма запазен профил.", color = TextSilver, modifier = Modifier.padding(16.dp))
        }
        return
    }

    var sex by remember { mutableStateOf(runCatching { Sex.valueOf(profile.sex) }.getOrDefault(Sex.MALE)) }
    var height by remember { mutableStateOf(profile.heightCm.toInt().toString()) }
    var weight by remember { mutableStateOf(profile.weightKg.toString()) }
    var target by remember { mutableStateOf(profile.targetWeightKg.toString()) }
    var activity by remember {
        mutableStateOf(runCatching { ActivityLevel.valueOf(profile.activityLevel) }.getOrDefault(ActivityLevel.LIGHT))
    }
    var goal by remember { mutableStateOf(runCatching { GoalType.valueOf(profile.goal) }.getOrDefault(GoalType.MAINTAIN)) }
    var pace by remember { mutableStateOf(runCatching { PaceOption.valueOf(profile.pace) }.getOrDefault(PaceOption.SLOW)) }
    var birthDate by remember { mutableStateOf(LocalDate.ofEpochDay(profile.birthDateEpochDay)) }
    var showSex by remember { mutableStateOf(false) }
    var showActivity by remember { mutableStateOf(false) }
    var showGoal by remember { mutableStateOf(false) }

    FormScreenInsets {
        ScreenTopBar("Профил", onBack = onBack)
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            ProfileRow("Пол", if (sex == Sex.MALE) "Мъж" else "Жена") { showSex = true }
            OutlinedTextField(height, { height = it }, label = { Text("Ръст (см)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = fieldColors())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(weight, { weight = it }, label = { Text("Тегло (кг)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = fieldColors())
            OutlinedTextField(target, { target = it }, label = { Text("Желано тегло (кг)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = fieldColors())
            ProfileRow("Активност", activity.label) { showActivity = true }
            ProfileRow("Цел", goalLabel(goal)) { showGoal = true }
            Spacer(Modifier.height(16.dp))
            TealButton("Запази") {
                onSave(
                    sex,
                    height.toFloatOrNull() ?: profile.heightCm,
                    weight.toFloatOrNull() ?: profile.weightKg,
                    target.toFloatOrNull() ?: profile.targetWeightKg,
                    activity,
                    goal,
                    pace,
                    birthDate
                )
            }
        }
    }

    if (showSex) {
        AlertDialog(onDismissRequest = { showSex = false }, title = { Text("Пол") }, text = {
            Column {
                listOf(Sex.MALE to "Мъж", Sex.FEMALE to "Жена").forEach { (s, l) ->
                    Text(l, Modifier.fillMaxWidth().clickable { sex = s; showSex = false }.padding(12.dp))
                }
            }
        }, confirmButton = {})
    }
    if (showActivity) {
        val st = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { showActivity = false }, sheetState = st) {
            Column(Modifier.padding(24.dp)) {
                ActivityLevel.entries.forEach { a ->
                    Text(
                        a.label,
                        color = TextWhite,
                        modifier = Modifier.fillMaxWidth().clickable { activity = a; showActivity = false }.padding(12.dp)
                    )
                }
            }
        }
    }
    if (showGoal) {
        AlertDialog(
            onDismissRequest = { showGoal = false },
            title = { Text("Цел") },
            text = {
                Column {
                    listOf(
                        GoalType.LOSE_WEIGHT to "Отслабване",
                        GoalType.MAINTAIN to "Поддържане на тегло",
                        GoalType.GAIN_WEIGHT to "Покачване на мускулна маса"
                    ).forEach { (g, l) ->
                        Text(
                            l,
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    goal = g
                                    showGoal = false
                                }
                                .padding(12.dp),
                            color = TextWhite
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}

private fun goalLabel(g: GoalType) = when (g) {
    GoalType.LOSE_WEIGHT -> "Отслабване"
    GoalType.MAINTAIN -> "Поддържане на тегло"
    GoalType.GAIN_WEIGHT -> "Покачване на мускулна маса"
}

@Composable
private fun ProfileRow(label: String, value: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp)) {
        Text(label, color = TextSilver, fontSize = 13.sp)
        Text(value, color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.Medium)
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

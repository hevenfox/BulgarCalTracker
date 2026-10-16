package com.bulgarcal.tracker.ui.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulgarcal.tracker.data.ActivityLevel
import com.bulgarcal.tracker.data.GoalType
import com.bulgarcal.tracker.data.PaceOption
import com.bulgarcal.tracker.data.Sex
import com.bulgarcal.tracker.ui.components.HollowGoalButton
import com.bulgarcal.tracker.ui.components.TealButton
import com.bulgarcal.tracker.ui.theme.BorderGray
import com.bulgarcal.tracker.ui.theme.EmeraldTeal
import com.bulgarcal.tracker.ui.theme.InputGray
import com.bulgarcal.tracker.ui.theme.TextSilver
import com.bulgarcal.tracker.ui.theme.TextWhite

@Composable
private fun OnboardingScreenLayout(
    scrollable: Boolean = true,
    bottomBar: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollMod = if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp, bottom = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = bottomBar == null)
                .then(scrollMod)
        ) {
            content()
        }
        bottomBar?.invoke()
    }
}

@Composable
fun GoalSelectionScreen(
    draft: OnboardingDraftState,
    onNext: () -> Unit,
    onExistingAccount: () -> Unit
) {
    val goal by draft::goal

    OnboardingScreenLayout(
        bottomBar = {
            TealButton("НАПРЕД", enabled = goal != null, onClick = onNext)
        }
    ) {
        Text(
            "Каква цел искаш да постигнеш?",
            color = TextWhite,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(24.dp))
        HollowGoalButton("Отслабване", goal == GoalType.LOSE_WEIGHT) {
            draft.goal = GoalType.LOSE_WEIGHT
        }
        Spacer(Modifier.height(12.dp))
        HollowGoalButton("Поддържане на тегло", goal == GoalType.MAINTAIN) {
            draft.goal = GoalType.MAINTAIN
        }
        Spacer(Modifier.height(12.dp))
        HollowGoalButton("Качване на тегло", goal == GoalType.GAIN_WEIGHT) {
            draft.goal = GoalType.GAIN_WEIGHT
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Вече имам акаунт.",
            color = EmeraldTeal,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onExistingAccount)
                .semantics { role = Role.Button }
                .padding(vertical = 12.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Тази информация се използва за да се изчисли калорийният ти прием и да се персонализира приложението.",
            color = TextSilver,
            fontSize = 13.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    draft: OnboardingDraftState,
    showActivitySheet: Boolean,
    onShowActivity: (Boolean) -> Unit,
    onNext: () -> Unit
) {
    var showSexDialog by remember { mutableStateOf(false) }
    val sex by draft::sex
    val activity by draft::activity
    val heightCm by draft::heightCm
    val weightKg by draft::weightKg
    val targetKg by draft::targetKg

    OnboardingScreenLayout(
        bottomBar = { TealButton("НАПРЕД", onClick = onNext) }
    ) {
        Text("Моят профил", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Моля въведете вашите характеристики с цел персонализиране на индивидуалния ви план и изчисляване на дневния ви калориен прием.",
            color = TextSilver,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(20.dp))
        ProfileTapField("Пол", if (sex == Sex.MALE) "Мъж" else "Жена") { showSexDialog = true }
        ProfileTapField("Ниво на активност", activity.label) { onShowActivity(true) }
        ProfileInputField("Ръст (см)", heightCm) { draft.heightCm = it }
        ProfileInputField("Тегло (кг)", weightKg) { draft.weightKg = it }
        ProfileInputField("Желано тегло (кг)", targetKg) { draft.targetKg = it }
    }

    if (showSexDialog) {
        AlertDialog(
            onDismissRequest = { showSexDialog = false },
            title = { Text("Пол") },
            text = {
                Column {
                    listOf(Sex.MALE to "Мъж", Sex.FEMALE to "Жена").forEach { (s, l) ->
                        Text(
                            l,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    draft.sex = s
                                    showSexDialog = false
                                }
                                .padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
    if (showActivitySheet) {
        ActivityLevelSheet(onDismiss = { onShowActivity(false) }) {
            draft.activity = it
            onShowActivity(false)
        }
    }
}

@Composable
private fun ProfileTapField(label: String, value: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .padding(vertical = 12.dp)
    ) {
        Text(label, color = TextSilver, fontSize = 13.sp)
        Text(value, color = TextWhite, fontSize = 18.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun ProfileInputField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, color = TextSilver) },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = InputGray,
            unfocusedContainerColor = InputGray,
            focusedBorderColor = BorderGray,
            unfocusedBorderColor = BorderGray,
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLevelSheet(onDismiss: () -> Unit, onSelect: (ActivityLevel) -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(24.dp).navigationBarsPadding()) {
            Text("Ниво на активност", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(16.dp))
            ActivityLevel.entries.forEach { level ->
                RowSelectable(level.label) { onSelect(level) }
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("ОТКАЗ", color = EmeraldTeal, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun RowSelectable(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = TextWhite,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .padding(vertical = 14.dp)
    )
}

@Composable
fun IntakeScreen(
    calories: Int,
    weeksText: String?,
    goal: GoalType,
    pace: PaceOption,
    onPaceChange: (PaceOption) -> Unit,
    onNext: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val paceOptions = if (goal == GoalType.MAINTAIN) emptyList() else listOf(PaceOption.SLOW, PaceOption.MEDIUM)

    OnboardingScreenLayout(
        scrollable = false,
        bottomBar = { TealButton("НАПРЕД", onClick = onNext) }
    ) {
        Text("Твоят калориен прием", color = TextWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Калорийният ти прием е изчислен на база въведените характеристики.", color = TextSilver)
        Spacer(Modifier.height(32.dp))
        Text(
            "Твоят дневен калориен прием е",
            color = TextSilver,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "$calories kcal",
            color = TextWhite,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        weeksText?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = EmeraldTeal, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        if (paceOptions.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            ProfileTapField("Темпо", pace.displayForGoal(goal)) { expanded = !expanded }
            if (expanded) {
                paceOptions.forEach { p ->
                    RowSelectable(p.displayForGoal(goal)) {
                        onPaceChange(p)
                        expanded = false
                    }
                }
            }
        }
    }
}

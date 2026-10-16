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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulgarcal.tracker.data.ActivityLevel
import com.bulgarcal.tracker.data.GoalType
import com.bulgarcal.tracker.data.PaceOption
import com.bulgarcal.tracker.data.Sex
import com.bulgarcal.tracker.data.UserProfileEntity
import com.bulgarcal.tracker.ui.components.BgFormat
import com.bulgarcal.tracker.ui.components.FormScreenInsets
import com.bulgarcal.tracker.ui.components.ScreenTopBar
import com.bulgarcal.tracker.ui.components.TealButton
import com.bulgarcal.tracker.ui.theme.TextSilver
import com.bulgarcal.tracker.ui.theme.TextWhite
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ProfileReadOnlyScreen(
    profile: UserProfileEntity?,
    displayName: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onSaveName: (String) -> Unit
) {
    var showNameDialog by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf(displayName) }

    FormScreenInsets {
        ScreenTopBar(title = "Профил", onBack = onBack)
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
        val birthFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy 'г.'", Locale("bg"))
        val goalText = pace.profileLabel(goal)

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            ProfileField("Име", displayName, onClick = {
                nameDraft = displayName
                showNameDialog = true
            })
            ProfileField("Пол", if (sex == Sex.MALE) "Мъж" else "Жена")
            ProfileField("Дата на раждане", birth.format(birthFmt))
            ProfileField("Ръст", "${BgFormat.decimalOne(profile.heightCm)} cm")
            ProfileField("Тегло", "${BgFormat.decimalOne(profile.weightKg)} kg")
            ProfileField("Ниво на активност", activity.label)
            ProfileField("Цел", goalText)
            ProfileField("Желано тегло", "${BgFormat.decimalOne(profile.targetWeightKg)} kg")
            Spacer(Modifier.height(16.dp))
            TealButton("Редактиране", onClick = onEdit)
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Име", color = TextWhite) },
            text = {
                OutlinedTextField(
                    value = nameDraft,
                    onValueChange = { nameDraft = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSaveName(nameDraft.ifBlank { "Потребител" })
                    showNameDialog = false
                }) { Text("Запиши") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("Отказ") }
            }
        )
    }
}

@Composable
private fun ProfileField(label: String, value: String, onClick: (() -> Unit)? = null) {
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(label, color = TextSilver, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = TextWhite, fontSize = 17.sp, fontWeight = FontWeight.Medium)
    }
}

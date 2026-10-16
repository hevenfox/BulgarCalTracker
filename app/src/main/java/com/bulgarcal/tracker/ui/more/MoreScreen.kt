package com.bulgarcal.tracker.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulgarcal.tracker.ui.components.SettingsDivider
import com.bulgarcal.tracker.ui.components.SettingsGroup
import com.bulgarcal.tracker.ui.components.SettingsRow
import com.bulgarcal.tracker.ui.components.SettingsSectionLabel
import com.bulgarcal.tracker.ui.components.UserProfileCard
import com.bulgarcal.tracker.ui.theme.TextWhite
import com.bulgarcal.tracker.ui.theme.ThemeMode
import kotlinx.coroutines.launch

@Composable
fun MoreScreen(
    displayName: String,
    dailyCalories: Int,
    bmr: Int,
    tdee: Int,
    remindersEnabled: Boolean,
    onProfile: () -> Unit,
    onYourNumbers: () -> Unit,
    onReminders: () -> Unit,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val themeLabel = when (themeMode) {
        ThemeMode.DARK -> "Тъмна"
        ThemeMode.LIGHT -> "Светла"
        ThemeMode.SYSTEM -> "Автоматично (Системна)"
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))
            Text("Още", color = TextWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            UserProfileCard(displayName = displayName, onClick = onProfile)

            SettingsSectionLabel("Вашият план")
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Default.Person,
                    title = "Профил",
                    subtitle = "$dailyCalories kcal на ден",
                    onClick = onProfile
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.Info,
                    title = "Вашите числа",
                    subtitle = "BMR $bmr · TDEE $tdee kcal",
                    onClick = onYourNumbers
                )
            }

            SettingsSectionLabel("Храни и данни")
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Default.Share,
                    title = "Споделени с мен",
                    enabled = false,
                    showChevron = false
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.PictureAsPdf,
                    title = "Експортирай PDF отчет",
                    subtitle = "Очаквайте скоро",
                    onClick = {
                        scope.launch { snackbar.showSnackbar("Очаквайте скоро") }
                    }
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.Link,
                    title = "Свържи с приложения",
                    subtitle = "Health Connect · изключено",
                    enabled = false,
                    showChevron = false
                )
            }

            SettingsSectionLabel("Приложение")
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Default.Notifications,
                    title = "Напомняния",
                    subtitle = if (remindersEnabled) "Включени" else "Изключени",
                    onClick = onReminders
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.Language,
                    title = "Език",
                    subtitle = "Български",
                    onClick = { showLanguageDialog = true }
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.DarkMode,
                    title = "Тема",
                    subtitle = themeLabel,
                    onClick = { showThemeDialog = true }
                )
            }

            SettingsSectionLabel("Помощ")
            SettingsGroup {
                SettingsRow(
                    icon = Icons.Default.MenuBook,
                    title = "Ръководство",
                    enabled = false,
                    showChevron = true
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.Email,
                    title = "Изпрати обратна връзка",
                    enabled = false,
                    showChevron = true
                )
                SettingsDivider()
                SettingsRow(
                    icon = Icons.Default.BugReport,
                    title = "Докладвай проблем",
                    enabled = false,
                    showChevron = true
                )
            }
            Spacer(Modifier.height(24.dp))
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Тема") },
            text = {
                Column {
                    listOf(
                        ThemeMode.DARK to "Тъмна",
                        ThemeMode.LIGHT to "Светла",
                        ThemeMode.SYSTEM to "Автоматично (Системна)"
                    ).forEach { (mode, label) ->
                        Text(
                            label,
                            color = TextWhite,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onThemeChange(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Език") },
            text = { Text("Приложението е на български.", color = TextWhite) },
            confirmButton = {
                Text(
                    "OK",
                    modifier = Modifier
                        .clickable { showLanguageDialog = false }
                        .padding(8.dp)
                )
            }
        )
    }
}

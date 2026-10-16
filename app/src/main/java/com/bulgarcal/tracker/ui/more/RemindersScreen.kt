package com.bulgarcal.tracker.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bulgarcal.tracker.reminders.MealReminderType
import com.bulgarcal.tracker.reminders.NotificationPermission
import com.bulgarcal.tracker.reminders.ReminderPreferences
import com.bulgarcal.tracker.reminders.ReminderScheduler
import com.bulgarcal.tracker.reminders.ReminderSettings
import com.bulgarcal.tracker.ui.components.ScreenTopBar
import com.bulgarcal.tracker.ui.theme.EmeraldTeal
import com.bulgarcal.tracker.ui.theme.TextSilver
import com.bulgarcal.tracker.ui.theme.TextWhite
import kotlinx.coroutines.launch

@Composable
fun RemindersScreen(
    settings: ReminderSettings,
    onBack: () -> Unit,
    onRequestPermission: (onGranted: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { ReminderPreferences(context) }

    var enabled by remember(settings.enabled) { mutableStateOf(settings.enabled) }
    var breakfastH by remember(settings.breakfastHour) { mutableIntStateOf(settings.breakfastHour) }
    var breakfastM by remember(settings.breakfastMinute) { mutableIntStateOf(settings.breakfastMinute) }
    var lunchH by remember(settings.lunchHour) { mutableIntStateOf(settings.lunchHour) }
    var lunchM by remember(settings.lunchMinute) { mutableIntStateOf(settings.lunchMinute) }
    var dinnerH by remember(settings.dinnerHour) { mutableIntStateOf(settings.dinnerHour) }
    var dinnerM by remember(settings.dinnerMinute) { mutableIntStateOf(settings.dinnerMinute) }

    fun persistAndSchedule() {
        scope.launch {
            try {
                prefs.setEnabled(enabled)
                prefs.setTime(MealReminderType.BREAKFAST, breakfastH, breakfastM)
                prefs.setTime(MealReminderType.LUNCH, lunchH, lunchM)
                prefs.setTime(MealReminderType.DINNER, dinnerH, dinnerM)
                ReminderScheduler.rescheduleAll(context)
            } catch (_: Exception) {
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar("Напомняния", onBack = onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Активни напомняния", color = TextWhite, fontWeight = FontWeight.Bold)
                    Text(
                        "Закуска, обяд и вечеря — всеки ден",
                        color = TextSilver,
                        fontSize = 13.sp
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { checked ->
                        if (checked && !NotificationPermission.hasPermission(context)) {
                            onRequestPermission {
                                enabled = true
                                persistAndSchedule()
                            }
                        } else {
                            enabled = checked
                            persistAndSchedule()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextWhite,
                        checkedTrackColor = EmeraldTeal
                    )
                )
            }
            Spacer(Modifier.height(24.dp))
            MealTimeRow("Закуска", breakfastH, breakfastM) { h, m ->
                breakfastH = h; breakfastM = m; persistAndSchedule()
            }
            MealTimeRow("Обяд", lunchH, lunchM) { h, m ->
                lunchH = h; lunchM = m; persistAndSchedule()
            }
            MealTimeRow("Вечеря", dinnerH, dinnerM) { h, m ->
                dinnerH = h; dinnerM = m; persistAndSchedule()
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "При натискане на известието се отваря екранът за добавяне на храна.",
                color = TextSilver,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun MealTimeRow(
    label: String,
    hour: Int,
    minute: Int,
    onChange: (Int, Int) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(label, color = TextWhite, fontWeight = FontWeight.Medium)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeAdjust("-", hour, minute, true, false, onChange)
            Text(
                "%02d:%02d".format(hour, minute),
                color = EmeraldTeal,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            TimeAdjust("+", hour, minute, true, true, onChange)
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeAdjust("−15м", hour, minute, false, false, onChange)
            Spacer(Modifier.padding(horizontal = 16.dp))
            TimeAdjust("+15м", hour, minute, false, true, onChange)
        }
    }
}

@Composable
private fun TimeAdjust(
    label: String,
    hour: Int,
    minute: Int,
    adjustHour: Boolean,
    increment: Boolean,
    onChange: (Int, Int) -> Unit
) {
    IconButton(onClick = {
        var h = hour
        var m = minute
        if (adjustHour) {
            h = if (increment) (h + 1) % 24 else if (h == 0) 23 else h - 1
        } else {
            val total = h * 60 + m + if (increment) 15 else -15
            val wrapped = (total + 24 * 60) % (24 * 60)
            h = wrapped / 60
            m = wrapped % 60
        }
        onChange(h, m)
    }) {
        if (label.length == 1) {
            Icon(
                if (increment) Icons.Default.Add else Icons.Default.Remove,
                contentDescription = null,
                tint = TextWhite
            )
        } else {
            Text(label, color = TextSilver, fontSize = 12.sp)
        }
    }
}

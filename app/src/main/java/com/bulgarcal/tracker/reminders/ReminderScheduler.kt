package com.bulgarcal.tracker.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.runBlocking

object ReminderScheduler {

    fun rescheduleAll(context: Context) {
        try {
            val prefs = ReminderPreferences(context)
            val settings = runBlocking { prefs.current() }
            MealReminderType.entries.forEach { cancel(context, it) }
            if (!settings.enabled) return
            MealReminderType.entries.forEach { type ->
                val (h, m) = prefs.hourMinute(settings, type)
                schedule(context, type, h, m)
            }
        } catch (_: Exception) {
            // Avoid crashing app if alarms cannot be scheduled
        }
    }

    fun schedule(context: Context, type: MealReminderType, hour: Int, minute: Int) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val triggerAt = nextTriggerMillis(hour, minute)
            val pending = pendingIntent(context, type)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }
    }

    fun cancel(context: Context, type: MealReminderType) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pending = pendingIntent(context, type)
            alarmManager.cancel(pending)
            pending.cancel()
        } catch (_: Exception) {
        }
    }

    fun cancelAll(context: Context) {
        MealReminderType.entries.forEach { cancel(context, it) }
    }

    private fun pendingIntent(context: Context, type: MealReminderType): PendingIntent {
        val intent = Intent(context, MealReminderReceiver::class.java).apply {
            action = MealReminderReceiver.ACTION_MEAL_REMINDER
            putExtra(MealReminderReceiver.EXTRA_MEAL_TYPE, type.name)
        }
        return PendingIntent.getBroadcast(
            context,
            type.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }
}

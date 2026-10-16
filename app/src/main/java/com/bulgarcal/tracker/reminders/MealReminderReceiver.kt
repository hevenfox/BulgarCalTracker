package com.bulgarcal.tracker.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.runBlocking

class MealReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val async = goAsync()
        try {
            if (intent?.action != ACTION_MEAL_REMINDER) return
            val typeName = intent.getStringExtra(EXTRA_MEAL_TYPE) ?: return
            val type = runCatching { MealReminderType.valueOf(typeName) }.getOrNull() ?: return

            ReminderNotifier.show(context, type)

            val prefs = ReminderPreferences(context)
            val settings = runBlocking { prefs.current() }
            if (settings.enabled) {
                val (h, m) = prefs.hourMinute(settings, type)
                ReminderScheduler.schedule(context, type, h, m)
            }
        } catch (_: Exception) {
        } finally {
            async.finish()
        }
    }

    companion object {
        const val ACTION_MEAL_REMINDER = "com.bulgarcal.tracker.MEAL_REMINDER"
        const val EXTRA_MEAL_TYPE = "meal_type"
    }
}

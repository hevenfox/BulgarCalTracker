package com.bulgarcal.tracker.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-schedule meal alarms after device reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        try {
            ReminderScheduler.rescheduleAll(context)
        } catch (_: Exception) {
        }
    }
}

package com.bulgarcal.tracker

import android.app.Application
import com.bulgarcal.tracker.reminders.ReminderNotifier
import com.bulgarcal.tracker.reminders.ReminderScheduler

class BulgarCalApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            ReminderNotifier.ensureChannel(this)
            ReminderScheduler.rescheduleAll(this)
        } catch (_: Exception) {
            // Never crash cold start because of reminder scheduling
        }
    }
}

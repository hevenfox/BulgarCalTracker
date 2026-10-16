package com.bulgarcal.tracker.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bulgarcal.tracker.MainActivity
import com.bulgarcal.tracker.R

object ReminderNotifier {
    const val CHANNEL_ID = "meal_reminders"
    private var channelCreated = false

    fun ensureChannel(context: Context) {
        if (channelCreated) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Напомняния за хранене",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Напомняния за закуска, обяд и вечеря"
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
        channelCreated = true
    }

    fun show(context: Context, type: MealReminderType) {
        ensureChannel(context)
        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_FOOD, true)
            putExtra(EXTRA_MEAL_TYPE, type.name)
        }
        val pending = PendingIntent.getActivity(
            context,
            type.requestCode,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentTitle(type.titleBg)
            .setContentText(type.messageBg)
            .setStyle(NotificationCompat.BigTextStyle().bigText(type.messageBg))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(type.requestCode, notification)
    }

    const val EXTRA_OPEN_FOOD = "open_food_search"
    const val EXTRA_MEAL_TYPE = "meal_type"
}

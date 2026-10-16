package com.bulgarcal.tracker.reminders

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

object NotificationPermission {

    fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun registerLauncher(
        activity: ComponentActivity,
        onResult: (Boolean) -> Unit
    ) = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> onResult(granted) }
}

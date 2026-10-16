package com.bulgarcal.tracker

import android.Manifest
import android.graphics.Color
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.bulgarcal.tracker.navigation.BulgarCalApp
import com.bulgarcal.tracker.reminders.NotificationPermission
import com.bulgarcal.tracker.reminders.ReminderNotifier
import com.bulgarcal.tracker.ui.theme.BulgarCalTheme
import com.bulgarcal.tracker.ui.theme.PitchBlack
import com.bulgarcal.tracker.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {

    private var permissionCallback: ((Boolean) -> Unit)? = null
    private var openFoodSearch by mutableStateOf(false)
    private var themeMode by mutableStateOf(ThemeMode.SYSTEM)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionCallback?.invoke(granted)
        permissionCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // dark-only UI: keep status/nav bar icons light even when the phone itself is in light mode
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        ReminderNotifier.ensureChannel(this)
        readOpenFoodIntent(intent)
        setContent {
            BulgarCalTheme(themeMode) {
                Surface(Modifier.fillMaxSize(), color = PitchBlack) {
                    BulgarCalApp(
                        openFoodSearch = openFoodSearch,
                        onFoodSearchConsumed = { openFoodSearch = false },
                        onRequestNotificationPermission = ::requestNotificationPermission,
                        themeMode = themeMode,
                        onThemeChange = { themeMode = it }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readOpenFoodIntent(intent)
    }

    private fun readOpenFoodIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(ReminderNotifier.EXTRA_OPEN_FOOD, false) == true) {
            openFoodSearch = true
        }
    }

    fun requestNotificationPermission(onResult: (Boolean) -> Unit) {
        if (NotificationPermission.hasPermission(this)) {
            onResult(true)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionCallback = onResult
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onResult(true)
        }
    }
}

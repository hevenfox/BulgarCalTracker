package com.bulgarcal.tracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

enum class ThemeMode {
    DARK, LIGHT, SYSTEM
}

private val DarkScheme = darkColorScheme(
    primary = EmeraldTeal,
    onPrimary = TextWhite,
    secondary = EmeraldTealDim,
    background = PitchBlack,
    surface = SurfaceGray,
    onBackground = TextWhite,
    onSurface = TextWhite,
    outline = BorderGray
)

private val LightScheme = lightColorScheme(
    primary = EmeraldTeal,
    onPrimary = TextWhite,
    secondary = EmeraldTealDim,
    background = TextWhite,
    surface = SurfaceGray,
    onBackground = PitchBlack,
    onSurface = TextWhite,
    outline = BorderGray
)

/** The app is dark-only for now. Set to false to bring back Light / System switching. */
private const val FORCE_DARK = true

@Composable
fun BulgarCalTheme(
    mode: ThemeMode,
    content: @Composable () -> Unit
) {
    val dark = FORCE_DARK || when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val scheme = if (dark) DarkScheme else LightScheme
    MaterialTheme(
        colorScheme = scheme,
        content = content
    )
}

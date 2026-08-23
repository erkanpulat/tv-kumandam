package com.erkanpulat.tvkumandam.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B47DC),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E5FF),
    onPrimaryContainer = Color(0xFF312374),
    secondary = Color(0xFF625B71),
    secondaryContainer = Color(0xFFE9E1F5),
    onSecondaryContainer = Color(0xFF332D40),
    tertiary = Color(0xFF187A56),
    error = Color(0xFFC43D4C),
    errorContainer = Color(0xFFFFDADD),
    onErrorContainer = Color(0xFF41000B),
    background = Color(0xFFEEF1F5),
    surface = Color(0xFFFBFCFE),
    surfaceVariant = Color(0xFFEDF1F6),
    surfaceContainer = Color(0xFFFBFCFE),
    surfaceContainerLow = Color(0xFFF4F6F9),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    outlineVariant = Color(0xFFD9DEE7),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9C8CFF),
    onPrimary = Color(0xFF1F145E),
    primaryContainer = Color(0xFF292442),
    onPrimaryContainer = Color(0xFFF0EDFF),
    secondary = Color(0xFFC9C2D8),
    secondaryContainer = Color(0xFF34303F),
    onSecondaryContainer = Color(0xFFECE5F7),
    tertiary = Color(0xFF57D7A2),
    error = Color(0xFFFF6F7D),
    errorContainer = Color(0xFF4B2028),
    onErrorContainer = Color(0xFFFFDADD),
    background = Color(0xFF080B12),
    surface = Color(0xFF111620),
    surfaceVariant = Color(0xFF202838),
    surfaceContainer = Color(0xFF111620),
    surfaceContainerLow = Color(0xFF0D1119),
    surfaceContainerHigh = Color(0xFF1A2130),
    outlineVariant = Color(0xFF30394A),
)

@Composable
fun TvKumandamTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
            dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}

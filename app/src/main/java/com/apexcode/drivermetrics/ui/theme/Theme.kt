package com.apexcode.drivermetrics.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = RouteBlue80,
    secondary = SlateGrey80,
    tertiary = Amber80,
)

private val LightColorScheme = lightColorScheme(
    primary = RouteBlue40,
    secondary = SlateGrey40,
    tertiary = Amber40,
)

/**
 * Only wraps MainActivity's screens (onboarding + settings) — the overlay's ComposeView never
 * sets this as a MaterialTheme ancestor, so it stays on Material3's baseline scheme regardless of
 * anything changed here. That's deliberate: the overlay's look isn't part of this theme's scope.
 */
@Composable
fun DriverMetricsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
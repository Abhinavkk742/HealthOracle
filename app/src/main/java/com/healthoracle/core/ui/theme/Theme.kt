package com.healthoracle.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Light colour scheme ───────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary             = PrimaryTeal,
    onPrimary           = androidx.compose.ui.graphics.Color.White,
    primaryContainer    = LightPrimaryContainer,
    onPrimaryContainer  = LightOnPrimaryContainer,
    tertiary            = TertiaryIndigo,
    onTertiary          = androidx.compose.ui.graphics.Color.White,
    background          = LightBackground,
    onBackground        = androidx.compose.ui.graphics.Color(0xFF0E1115),
    surface             = LightSurface,
    onSurface           = androidx.compose.ui.graphics.Color(0xFF1A1F27),
    surfaceVariant      = LightSurfaceVariant,
    onSurfaceVariant    = androidx.compose.ui.graphics.Color(0xFF4E5A6A),
    outline             = androidx.compose.ui.graphics.Color(0xFFBCC4CF),
    outlineVariant      = androidx.compose.ui.graphics.Color(0xFFD8DDE6),
    error               = AccentRose
)

// ── Dark colour scheme ────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary             = PrimaryTealLight,
    onPrimary           = androidx.compose.ui.graphics.Color(0xFF002924),
    primaryContainer    = DarkPrimaryContainer,
    onPrimaryContainer  = DarkOnPrimaryContainer,
    tertiary            = TertiaryIndigoDark,
    onTertiary          = androidx.compose.ui.graphics.Color.White,
    background          = DarkBackground,
    onBackground        = androidx.compose.ui.graphics.Color(0xFFE4E8EF),
    surface             = DarkSurface,
    onSurface           = androidx.compose.ui.graphics.Color(0xFFD4D8DF),
    surfaceVariant      = DarkSurfaceVariant,
    onSurfaceVariant    = androidx.compose.ui.graphics.Color(0xFF8E98A8),
    outline             = androidx.compose.ui.graphics.Color(0xFF3E4757),
    outlineVariant      = androidx.compose.ui.graphics.Color(0xFF2E3542),
    error               = AccentRose
)

// ── Theme ─────────────────────────────────────────────────────────────────────
@Composable
fun HealthOracleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled to preserve brand palette
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = HealthOracleTypography,
        content     = content
    )
}

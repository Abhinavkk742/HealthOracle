package com.healthoracle.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Dark Color Scheme ─────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary              = PrimaryLight,
    onPrimary            = DarkOnPrimary,
    primaryContainer     = PrimaryContainer,
    onPrimaryContainer   = Color(0xFFC7D2FE),   // indigo-200

    secondary            = Teal,
    onSecondary          = Color(0xFF003344),
    secondaryContainer   = TealContainer,
    onSecondaryContainer = Color(0xFF67E8F9),

    tertiary             = AccentViolet,
    onTertiary           = Color(0xFF1A0050),
    tertiaryContainer    = Color(0xFF2D1B69),
    onTertiaryContainer  = Color(0xFFDDD6FE),

    error                = AccentRose,
    onError              = Color(0xFF4A0010),
    errorContainer       = Color(0xFF4C0519),
    onErrorContainer     = Color(0xFFFDA4AF),

    background           = DarkBackground,
    onBackground         = DarkOnBg,

    surface              = DarkSurface,
    onSurface            = DarkOnSurface,
    surfaceVariant       = DarkSurfaceVar,
    onSurfaceVariant     = DarkOnSurfaceVar,

    outline              = DarkOutline,
    outlineVariant       = DarkOutlineVar,

    inverseSurface       = DarkOnBg,
    inverseOnSurface     = DarkSurface,
    inversePrimary       = Primary
)

// ── Light Color Scheme ────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary              = Primary,
    onPrimary            = LightOnPrimary,
    primaryContainer     = LightSurface2,
    onPrimaryContainer   = Color(0xFF1E1B4B),

    secondary            = TealDark,
    onSecondary          = Color(0xFFFFFFFF),
    secondaryContainer   = Color(0xFFE0F7FA),
    onSecondaryContainer = Color(0xFF003344),

    tertiary             = Color(0xFF7C3AED),
    onTertiary           = Color(0xFFFFFFFF),
    tertiaryContainer    = Color(0xFFEDE9FE),
    onTertiaryContainer  = Color(0xFF2D1B69),

    error                = Color(0xFFE11D48),
    onError              = Color(0xFFFFFFFF),
    errorContainer       = Color(0xFFFFE4E6),
    onErrorContainer     = Color(0xFF4C0519),

    background           = LightBackground,
    onBackground         = LightOnBg,

    surface              = LightSurface,
    onSurface            = LightOnSurface,
    surfaceVariant       = LightSurfaceVar,
    onSurfaceVariant     = LightOnSurfaceVar,

    outline              = LightOutline,
    outlineVariant       = LightOutlineVar,

    inverseSurface       = LightOnBg,
    inverseOnSurface     = LightSurface,
    inversePrimary       = PrimaryLight
)

// ── HealthOracle Theme ────────────────────────────────────────────────────────
@Composable
fun HealthOracleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,          // keep brand palette
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            // Dynamic color supported but disabled to preserve brand
            if (darkTheme) DarkColorScheme else LightColorScheme
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Edge-to-edge: system bar color = transparent, content draws behind
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = HealthOracleTypography,
        shapes      = HealthOracleShapes,
        content     = content
    )
}

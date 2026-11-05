package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.local.AppThemeMode

private val LightColors = lightColorScheme(
    primary = DarkBlue,
    onPrimary = Color.White,
    primaryContainer = LightTurquoise.copy(alpha = 0.35f),
    onPrimaryContainer = DeepNavy,
    secondary = Turquoise,
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = DeepNavy,
    tertiary = SuccessGreen,
    background = LightBackground,
    onBackground = DeepNavy,
    surface = LightSurface,
    onSurface = DeepNavy,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = DarkBlue.copy(alpha = 0.8f),
    error = ErrorCoral,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Turquoise,
    onPrimary = DeepNavy,
    primaryContainer = DarkBlue,
    onPrimaryContainer = LightTurquoise,
    secondary = LightTurquoise,
    onSecondary = DeepNavy,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = Color.White,
    tertiary = SuccessGreen,
    background = DarkBackground,
    onBackground = Color.White,
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = LightTurquoise.copy(alpha = 0.85f),
    error = ErrorCoral,
    onError = Color.White
)

val YadetShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun YadetTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = if (isDark) DarkColors else LightColors,
            typography = Typography,
            shapes = YadetShapes,
            content = content
        )
    }
}

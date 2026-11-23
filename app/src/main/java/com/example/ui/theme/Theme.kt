package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val WorldOrderColorScheme = darkColorScheme(
    primary = AccentCyan,
    onPrimary = OledBlack,
    primaryContainer = OledElevated,
    onPrimaryContainer = AccentCyan,
    secondary = AccentGold,
    onSecondary = OledBlack,
    secondaryContainer = OledElevated,
    onSecondaryContainer = AccentGold,
    tertiary = AccentGreen,
    onTertiary = OledBlack,
    background = OledBlack,
    onBackground = TextPrimary,
    surface = OledCard,
    onSurface = TextPrimary,
    surfaceVariant = OledElevated,
    onSurfaceVariant = TextSecondary,
    outline = OledBorder,
    outlineVariant = OledBorderHighlight,
    error = AccentRed,
    onError = TextPrimary
)

val WorldOrderShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

@Composable
fun WorldOrderTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = WorldOrderColorScheme,
        typography = Typography,
        shapes = WorldOrderShapes,
        content = content
    )
}

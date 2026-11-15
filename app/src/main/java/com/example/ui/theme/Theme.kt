package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val BreezeColorScheme = lightColorScheme(
    primary = BreezePrimary,
    onPrimary = BreezeOnPrimary,
    primaryContainer = BreezePrimarySoft,
    onPrimaryContainer = BreezeInk,
    secondary = BreezeSecondary,
    onSecondary = BreezeOnPrimary,
    secondaryContainer = BreezeSurface2,
    onSecondaryContainer = BreezeInk,
    tertiary = BreezeSuccess,
    onTertiary = BreezeOnPrimary,
    background = BreezeBackground,
    onBackground = BreezeInk,
    surface = BreezeBackground,
    onSurface = BreezeInk,
    surfaceVariant = BreezeSurface2,
    onSurfaceVariant = BreezeInk2,
    outline = BreezeTertiary,
    outlineVariant = BreezeLine,
    error = BreezeDanger,
    onError = BreezeOnPrimary,
    errorContainer = BreezeDangerSoft,
    onErrorContainer = BreezeDanger,
    inverseSurface = BreezeInverse,
    inverseOnSurface = BreezeBackground,
)

private val BreezeShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun FinTrackerTheme(
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = BreezeColorScheme,
        typography = BreezeTypography,
        shapes = BreezeShapes,
        content = content,
    )
}

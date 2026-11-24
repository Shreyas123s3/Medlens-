package com.medlens.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = MedLensPrimary,
    onPrimary = MedLensCard,
    secondary = MedLensSecondary,
    onSecondary = MedLensNavy,
    background = MedLensBackground,
    onBackground = MedLensText,
    surface = MedLensCard,
    onSurface = MedLensText,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE8F1F8),
    onSurfaceVariant = MedLensSecondaryText,
    error = MedLensEmergency,
)

private val DarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF77B7EC),
    onPrimary = MedLensDarkBackground,
    secondary = androidx.compose.ui.graphics.Color(0xFF61D7C6),
    onSecondary = MedLensDarkBackground,
    background = MedLensDarkBackground,
    onBackground = androidx.compose.ui.graphics.Color(0xFFEAF2F8),
    surface = MedLensDarkSurface,
    onSurface = androidx.compose.ui.graphics.Color(0xFFEAF2F8),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF26445F),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFB4C8D9),
    error = androidx.compose.ui.graphics.Color(0xFFFF8585),
)

@Composable
fun MedLensTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MedLensTypography,
        content = content,
    )
}

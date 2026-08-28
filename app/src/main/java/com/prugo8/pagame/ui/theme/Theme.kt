package com.prugo8.pagame.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = DeepCoralPrimary,
    primaryContainer = DeepCoralContainer,
    onPrimary = OnPrimary,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = SoftAmberSecondary,
    secondaryContainer = SoftAmberContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = MutedTealTertiary,
    tertiaryContainer = MutedTealContainer,
    onTertiaryContainer = OnTertiaryContainer,
    background = SurfaceLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceContainerLow,
    onSurface = OnSurfaceNavy,
    onSurfaceVariant = OnSurfaceVariant,
    outlineVariant = OutlineVariant
)

@Composable
fun PagameTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}

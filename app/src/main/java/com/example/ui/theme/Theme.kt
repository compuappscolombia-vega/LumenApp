package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LumenDarkColorScheme = darkColorScheme(
    primary = AmberGold,
    onPrimary = ObsidianBg,
    primaryContainer = AmberContainer,
    onPrimaryContainer = IvoryPrimary,
    secondary = TealAtmos,
    onSecondary = ObsidianBg,
    secondaryContainer = TealContainer,
    onSecondaryContainer = IvoryPrimary,
    tertiary = AmberGoldBright,
    background = ObsidianBg,
    onBackground = IvoryPrimary,
    surface = StudioSurface,
    onSurface = IvoryPrimary,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = WarmSilver,
    outline = StudioBorder,
    outlineVariant = SubtleSlate,
    error = ErrorCrimson,
    onError = IvoryPrimary,
    errorContainer = ErrorContainerDark,
    onErrorContainer = ErrorLightText
)

private val LumenShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

@Composable
fun LumenReelTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LumenDarkColorScheme,
        typography = LumenTypography,
        shapes = LumenShapes,
        content = content
    )
}

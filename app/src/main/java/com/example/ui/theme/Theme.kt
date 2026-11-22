package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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

private val LumenLightColorScheme = lightColorScheme(
    primary = Color(0xFFC27E15),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFCE9C7),
    onPrimaryContainer = Color(0xFF382302),
    secondary = Color(0xFF0D9488),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF042F2E),
    tertiary = Color(0xFFD97706),
    background = Color(0xFFF8F7F4),
    onBackground = Color(0xFF1C1B18),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B18),
    surfaceVariant = Color(0xFFF1EFEA),
    onSurfaceVariant = Color(0xFF48453E),
    outline = Color(0x1F000000),
    outlineVariant = Color(0xFFD6D3CE),
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B)
)

private val LumenShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

@Composable
fun LumenReelTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) LumenDarkColorScheme else LumenLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = LumenTypography,
        shapes = LumenShapes,
        content = content
    )
}

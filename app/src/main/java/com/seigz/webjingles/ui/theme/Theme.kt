package com.seigz.webjingles.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GamingDarkScheme = darkColorScheme(
    primary = BrandRed,
    onPrimary = Color.Black,
    primaryContainer = BrandRed.copy(alpha = 0.12f),
    onPrimaryContainer = BrandRed,
    secondary = AccentPurple,
    onSecondary = Color.Black,
    secondaryContainer = AccentPurple.copy(alpha = 0.12f),
    onSecondaryContainer = AccentPurple,
    tertiary = AccentGreen,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor,
    outlineVariant = DividerColor,
    error = AccentRed,
    onError = Color.White
)

@Composable
fun WebJinglesTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GamingDarkScheme,
        typography = Typography,
        content = content
    )
}
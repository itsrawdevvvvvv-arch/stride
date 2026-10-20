package com.epicmems.stride.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StrideColors = darkColorScheme(
    primary = Lime,
    onPrimary = OnPastel,
    secondary = Pink,
    onSecondary = OnPastel,
    tertiary = Peri,
    onTertiary = OnPastel,
    background = Bg,
    onBackground = TextPrimary,
    surface = Surface1,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextMuted2,
    outline = Line,
    outlineVariant = Surface3
)

@Composable
fun StrideTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StrideColors,
        typography = StrideTypography,
        shapes = StrideShapes,
        content = content
    )
}

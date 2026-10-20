package com.epicmems.stride.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val StrideColors = lightColorScheme(
    primary = Ink,
    onPrimary = CardWhite,
    secondary = Accent,
    onSecondary = CardWhite,
    background = Paper,
    onBackground = Ink,
    surface = CardWhite,
    onSurface = Ink,
    surfaceVariant = PaperDeep,
    onSurfaceVariant = InkSoft,
    outline = InkLine,
    outlineVariant = TrackGray
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

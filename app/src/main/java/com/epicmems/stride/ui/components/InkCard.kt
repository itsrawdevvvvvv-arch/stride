package com.epicmems.stride.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.epicmems.stride.ui.theme.CardWhite
import com.epicmems.stride.ui.theme.Ink
import com.epicmems.stride.ui.theme.InkLine

/**
 * The signature card of the whole app: a rounded, ink-outlined panel with a soft shadow.
 * Set [inverted] for the dark "hero" variant (light text on ink).
 */
@Composable
fun InkCard(
    modifier: Modifier = Modifier,
    inverted: Boolean = false,
    corner: Dp = 22.dp,
    borderWidth: Dp = 1.5.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(corner)
    val background = if (inverted) Ink else CardWhite
    val foreground = if (inverted) CardWhite else Ink
    val stroke = if (inverted) Ink else InkLine

    Box(
        modifier = modifier
            .shadow(5.dp, shape, clip = false)
            .clip(shape)
            .background(background)
            .border(borderWidth, stroke, shape)
    ) {
        CompositionLocalProvider(LocalContentColor provides foreground) {
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    }
}

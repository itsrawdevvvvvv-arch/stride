package com.epicmems.stride.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.epicmems.stride.ui.theme.Lime
import com.epicmems.stride.ui.theme.OnPastel
import com.epicmems.stride.ui.theme.Surface1
import com.epicmems.stride.ui.theme.Surface2
import com.epicmems.stride.ui.theme.Surface3
import com.epicmems.stride.ui.theme.TextMuted
import com.epicmems.stride.ui.theme.TextPrimary

/** A rounded card with a pastel fill — the accent surface used across the app. */
@Composable
fun PastelCard(
    color: Color,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(color)
            .padding(contentPadding),
        content = content
    )
}

/** The neutral dark card. */
@Composable
fun DarkCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Surface1)
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = TextMuted,
        style = MaterialTheme.typography.labelMedium
    )
}

/** The rounded selectable row from the design: circular leading icon, label, trailing hint. */
@Composable
fun PillRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val background = if (selected) Lime else Surface2
    val foreground = if (selected) OnPastel else TextPrimary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(background)
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        leading?.invoke()
        Text(
            text = label,
            color = foreground,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

/** Full-width lime pill button. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(if (enabled) Lime else Surface3)
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading?.invoke()
        if (leading != null) Spacer(Modifier.width(9.dp))
        Text(
            text = text,
            color = if (enabled) OnPastel else TextMuted,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/** Full-width neutral pill button. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(Surface2)
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading?.invoke()
        if (leading != null) Spacer(Modifier.width(9.dp))
        Text(text = text, color = TextPrimary, style = MaterialTheme.typography.labelLarge)
    }
}

/** Small pill used for inline stats. */
@Composable
fun StatChip(text: String, modifier: Modifier = Modifier, background: Color = Surface2) {
    Text(
        text = text,
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        color = TextPrimary,
        style = MaterialTheme.typography.labelMedium
    )
}

/**
 * Rounded progress bar. The fill width is read inside the Canvas draw lambda, so animating it
 * only triggers a redraw — never a recomposition or a layout pass.
 */
@Composable
fun ProgressPill(
    progress: Float,
    track: Color,
    fill: Color,
    modifier: Modifier = Modifier,
    height: Dp = 12.dp
) {
    val target = progress.coerceIn(0f, 1f)
    val animated = animateFloatAsState(
        targetValue = target,
        animationSpec = tween(Motion.BASE, easing = FastOutSlowInEasing),
        label = "progressPill"
    )
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val radius = size.height / 2f
        drawRoundRect(
            color = track,
            cornerRadius = CornerRadius(radius, radius)
        )
        val width = size.width * animated.value
        if (width > 0.5f) {
            drawRoundRect(
                color = fill,
                size = Size(width, size.height),
                cornerRadius = CornerRadius(radius, radius)
            )
        }
    }
}

/** Circular progress ring whose sweep eases to the new value (draw-phase only). */
@Composable
fun StepRing(
    progress: Float,
    trackColor: Color,
    ringColor: Color,
    modifier: Modifier = Modifier,
    ringSize: Dp = 96.dp,
    stroke: Dp = 10.dp
) {
    val target = progress.coerceIn(0f, 1f)
    val animated = animateFloatAsState(
        targetValue = target,
        animationSpec = tween(Motion.SLOW, easing = FastOutSlowInEasing),
        label = "ringProgress"
    )
    Canvas(modifier = modifier.size(ringSize)) {
        val strokePx = stroke.toPx()
        val radius = (this.size.minDimension - strokePx) / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        drawCircle(color = trackColor, radius = radius, center = center)
        drawArc(
            color = ringColor,
            startAngle = -90f,
            sweepAngle = 360f * animated.value,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokePx, cap = StrokeCap.Round)
        )
    }
}

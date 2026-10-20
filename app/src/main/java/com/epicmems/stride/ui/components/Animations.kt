package com.epicmems.stride.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.epicmems.stride.util.Format
import kotlinx.coroutines.delay

/** One set of timings for the whole app, so every motion feels like one system. */
object Motion {
    /** Selection changes, small state flips. */
    const val FAST = 180

    /** Default for values rolling to a new number. */
    const val BASE = 420

    /** Longer reveals: chart growth, screen entrances. */
    const val SLOW = 700
}

/**
 * Scale-on-press feedback.
 *
 * The scale is read inside `graphicsLayer`, so the animation runs entirely on the draw phase —
 * pressing a button never causes a recomposition.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.96f
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pressScale"
    )
    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/**
 * A step count that rolls to its new value instead of jumping. Because the engine flushes in
 * small chunks, this reads as a continuously ticking counter.
 */
@Composable
fun AnimatedSteps(
    value: Int,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    durationMillis: Int = Motion.SLOW
) {
    val animated by animateIntAsState(
        targetValue = value,
        animationSpec = tween(durationMillis, easing = FastOutSlowInEasing),
        label = "animatedSteps"
    )
    Text(text = Format.steps(animated), style = style, color = color, modifier = modifier)
}

/** Distance readout that eases to its new value as GPS fixes arrive. */
@Composable
fun AnimatedDistance(
    meters: Double,
    unit: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = meters.toFloat(),
        animationSpec = tween(Motion.BASE, easing = FastOutSlowInEasing),
        label = "animatedDistance"
    )
    Text(
        text = Format.distance(animated.toDouble(), unit),
        style = style,
        color = color,
        modifier = modifier
    )
}

/** A dot that breathes while a route is being recorded. */
@Composable
fun RecordingPulse(
    color: Color,
    modifier: Modifier = Modifier,
    dotSize: Dp = 10.dp
) {
    var bright by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(900)
            bright = !bright
        }
    }
    val alphaState = animateFloatAsState(
        targetValue = if (bright) 1f else 0.25f,
        animationSpec = tween(900, easing = LinearEasing),
        label = "pulseAlpha"
    )
    Box(
        modifier = modifier
            .size(dotSize)
            .graphicsLayer { alpha = alphaState.value }
            .clip(CircleShape)
            .background(color)
    )
}

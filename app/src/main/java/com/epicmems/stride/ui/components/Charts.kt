package com.epicmems.stride.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.epicmems.stride.domain.DayStat
import com.epicmems.stride.domain.Geo
import com.epicmems.stride.domain.LatLng
import com.epicmems.stride.ui.theme.Lime

/**
 * Bars for the last N days with a dashed goal line. The bars grow in once when the chart
 * first has data; afterwards the reveal value is read inside the draw lambda, so live data
 * changes only trigger a redraw.
 */
@Composable
fun WeekBars(
    days: List<DayStat>,
    goal: Int,
    modifier: Modifier = Modifier,
    barColor: Color,
    trackColor: Color,
    accentColor: Color = Lime
) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(days.size) {
        if (days.isNotEmpty()) {
            reveal.snapTo(0f)
            reveal.animateTo(1f, tween(Motion.SLOW, easing = FastOutSlowInEasing))
        }
    }

    Canvas(modifier) {
        if (days.isEmpty()) return@Canvas
        val count = days.size
        val gap = size.width * 0.02f
        val barWidth = (size.width - gap * (count - 1)) / count
        val maxValue = maxOf(days.maxOf { it.steps }, goal, 1).toFloat()
        val grow = reveal.value

        days.forEachIndexed { index, day ->
            val x = index * (barWidth + gap)
            val barHeight = (day.steps / maxValue) * size.height * grow
            val radius = CornerRadius(barWidth / 2f, barWidth / 2f)

            drawRoundRect(
                color = trackColor,
                topLeft = Offset(x, 0f),
                size = Size(barWidth, size.height),
                cornerRadius = radius
            )
            if (barHeight > 0.5f) {
                drawRoundRect(
                    color = if (day.goalHit) accentColor else barColor,
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = radius
                )
            }
        }

        val goalY = size.height * (1f - (goal / maxValue)).coerceIn(0f, size.height)
        drawLine(
            color = barColor.copy(alpha = 0.45f),
            start = Offset(0f, goalY),
            end = Offset(size.width, goalY),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
        )
    }
}

/** 24 hour cells laid out 12 x 2, shaded by intensity. */
@Composable
fun HourHeat(
    hours: List<Int>,
    modifier: Modifier = Modifier,
    color: Color
) {
    Canvas(modifier) {
        val columns = 12
        val rows = 2
        val gap = size.width * 0.014f
        val cellWidth = (size.width - gap * (columns - 1)) / columns
        val cellHeight = (size.height - gap * (rows - 1)) / rows
        val maxValue = (hours.maxOrNull() ?: 0).coerceAtLeast(1)

        hours.forEachIndexed { index, value ->
            val row = index / columns
            val column = index % columns
            val alpha = 0.12f + 0.88f * (value.toFloat() / maxValue)
            drawRoundRect(
                color = color.copy(alpha = alpha),
                topLeft = Offset(column * (cellWidth + gap), row * (cellHeight + gap)),
                size = Size(cellWidth, cellHeight),
                cornerRadius = CornerRadius(cellWidth * 0.28f, cellWidth * 0.28f)
            )
        }
    }
}

/**
 * Draws a GPS route as a polyline — the "Strava drawing". Points are normalised into the
 * canvas with the aspect ratio preserved, so the shape stays recognisable, and this is the
 * same composable used for the live drawing while a route is being recorded.
 */
@Composable
fun RouteCanvas(
    points: List<LatLng>,
    modifier: Modifier = Modifier,
    color: Color = Lime,
    strokeWidth: Dp = 3.dp
) {
    Canvas(modifier) {
        if (points.size < 2) return@Canvas
        val normalised = Geo.normalise(points)
        if (normalised.isEmpty()) return@Canvas

        val pad = strokeWidth.toPx() * 2f
        val w = (size.width - pad * 2f).coerceAtLeast(1f)
        val h = (size.height - pad * 2f).coerceAtLeast(1f)

        val path = Path()
        normalised.forEachIndexed { index, point ->
            val px = pad + point.first * w
            val py = pad + point.second * h
            if (index == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        val start = normalised.first()
        val end = normalised.last()
        drawCircle(
            color = color,
            radius = strokeWidth.toPx() * 1.7f,
            center = Offset(pad + start.first * w, pad + start.second * h)
        )
        drawCircle(
            color = Color.White,
            radius = strokeWidth.toPx() * 0.9f,
            center = Offset(pad + end.first * w, pad + end.second * h)
        )
    }
}

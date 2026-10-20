package com.epicmems.stride.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.epicmems.stride.domain.DayStat
import com.epicmems.stride.ui.theme.Accent
import com.epicmems.stride.ui.theme.Ink
import com.epicmems.stride.ui.theme.InkSoft
import com.epicmems.stride.ui.theme.TrackGray
import java.time.LocalDate

/** Vertical bars for the last N days, with a dashed goal line. Goal-hit days use the accent. */
@Composable
fun WeeklyBars(
    stats: List<DayStat>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        if (stats.isEmpty()) return@Canvas
        val count = stats.size
        val gap = size.width * 0.025f
        val barWidth = (size.width - gap * (count - 1)) / count
        val maxValue = stats.maxOf { it.steps }.coerceAtLeast(1).toFloat()
        val goal = stats.first().goal.toFloat()

        stats.forEachIndexed { index, stat ->
            val x = index * (barWidth + gap)
            val barHeight = (stat.steps / maxValue) * size.height
            drawRoundRect(
                color = TrackGray,
                topLeft = Offset(x, 0f),
                size = Size(barWidth, size.height),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
            if (barHeight > 0f) {
                drawRoundRect(
                    color = if (stat.goalHit) Accent else Ink,
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }

        val goalY = size.height * (1f - (goal / maxValue)).coerceIn(0f, size.height)
        drawLine(
            color = InkSoft.copy(alpha = 0.45f),
            start = Offset(0f, goalY),
            end = Offset(size.width, goalY),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))
        )
    }
}

/** 24 hour-cells laid out 12 x 2, shaded by intensity. */
@Composable
fun HourHeat(
    hours: List<Int>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        val columns = 12
        val rows = 2
        val gap = size.width * 0.015f
        val cellWidth = (size.width - gap * (columns - 1)) / columns
        val cellHeight = (size.height - gap * (rows - 1)) / rows
        val maxValue = (hours.maxOrNull() ?: 0).coerceAtLeast(1)

        hours.forEachIndexed { index, value ->
            val row = index / columns
            val column = index % columns
            val alpha = 0.10f + 0.90f * (value.toFloat() / maxValue)
            drawRoundRect(
                color = Ink.copy(alpha = alpha),
                topLeft = Offset(column * (cellWidth + gap), row * (cellHeight + gap)),
                size = Size(cellWidth, cellHeight),
                cornerRadius = CornerRadius(cellWidth * 0.25f, cellWidth * 0.25f)
            )
        }
    }
}

/** Calendar heatmap for the current month: one cell per day, shaded by goal progress. */
@Composable
fun MonthHeat(
    days: List<DayStat>,
    month: LocalDate,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        if (days.isEmpty()) return@Canvas
        val columns = 7
        val firstDow = month.withDayOfMonth(1).dayOfWeek.value % 7 // Sunday = 0
        val rows = (firstDow + days.size + 6) / 7
        val gap = size.width * 0.015f
        val cellWidth = (size.width - gap * (columns - 1)) / columns
        val cellHeight = (size.height - gap * (rows - 1)) / rows

        days.forEachIndexed { index, stat ->
            val cell = firstDow + index
            val row = cell / columns
            val column = cell % columns
            val color = when {
                stat.steps == 0 -> TrackGray.copy(alpha = 0.35f)
                stat.goalHit -> Accent
                else -> Ink.copy(alpha = 0.28f + 0.55f * stat.progress)
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(column * (cellWidth + gap), row * (cellHeight + gap)),
                size = Size(cellWidth, cellHeight),
                cornerRadius = CornerRadius(cellWidth * 0.22f, cellWidth * 0.22f)
            )
        }
    }
}

/** 30-day sparkline. */
@Composable
fun Sparkline(
    stats: List<DayStat>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier) {
        if (stats.size < 2) return@Canvas
        val maxValue = stats.maxOf { it.steps }.coerceAtLeast(1).toFloat()
        val stepX = size.width / (stats.size - 1)
        val path = Path()
        stats.forEachIndexed { index, stat ->
            val x = index * stepX
            val y = size.height * (1f - (stat.steps / maxValue))
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = Ink,
            style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

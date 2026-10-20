package com.epicmems.stride.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.epicmems.stride.ui.theme.Ink

/** Simple line-art walking figure, drawn to match the sketchy reference style. */
@Composable
fun WalkDoodle(
    modifier: Modifier = Modifier,
    color: Color = Ink
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = (w * 0.07f).coerceAtLeast(2f)

        drawCircle(
            color = color,
            radius = w * 0.11f,
            center = Offset(w * 0.56f, h * 0.16f),
            style = Stroke(width = stroke)
        )
        // torso
        drawLine(color, Offset(w * 0.53f, h * 0.28f), Offset(w * 0.45f, h * 0.60f), strokeWidth = stroke, cap = StrokeCap.Round)
        // arms
        drawLine(color, Offset(w * 0.53f, h * 0.38f), Offset(w * 0.30f, h * 0.45f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.53f, h * 0.38f), Offset(w * 0.76f, h * 0.30f), strokeWidth = stroke, cap = StrokeCap.Round)
        // legs
        drawLine(color, Offset(w * 0.45f, h * 0.60f), Offset(w * 0.26f, h * 0.88f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.45f, h * 0.60f), Offset(w * 0.67f, h * 0.90f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

/** Scattered falling leaves, echoing the reference's drifting foliage. */
@Composable
fun LeavesDoodle(
    modifier: Modifier = Modifier,
    color: Color = Ink
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawLeaf(Offset(w * 0.18f, h * 0.30f), h * 0.16f, -25f, color)
        drawLeaf(Offset(w * 0.42f, h * 0.72f), h * 0.13f, 35f, color)
        drawLeaf(Offset(w * 0.70f, h * 0.22f), h * 0.11f, 15f, color)
        drawLeaf(Offset(w * 0.88f, h * 0.62f), h * 0.15f, -40f, color)
        drawLeaf(Offset(w * 0.10f, h * 0.82f), h * 0.10f, 50f, color)
        // a few little motion swirls
        drawCircle(color.copy(alpha = 0.6f), radius = 1.5f, center = Offset(w * 0.55f, h * 0.45f))
        drawCircle(color.copy(alpha = 0.6f), radius = 1.5f, center = Offset(w * 0.32f, h * 0.15f))
    }
}

private fun DrawScope.drawLeaf(center: Offset, length: Float, angleDeg: Float, color: Color) {
    rotate(angleDeg, pivot = center) {
        val half = length / 2f
        val path = Path().apply {
            moveTo(center.x, center.y - half)
            cubicTo(
                center.x + length * 0.38f, center.y - length * 0.14f,
                center.x + length * 0.38f, center.y + length * 0.14f,
                center.x, center.y + half
            )
            cubicTo(
                center.x - length * 0.38f, center.y + length * 0.14f,
                center.x - length * 0.38f, center.y - length * 0.14f,
                center.x, center.y - half
            )
            close()
        }
        drawPath(path, color, style = Stroke(width = (length * 0.07f).coerceAtLeast(1.2f)))
        drawLine(
            color,
            Offset(center.x, center.y - half),
            Offset(center.x, center.y + half),
            strokeWidth = (length * 0.05f).coerceAtLeast(1f)
        )
    }
}

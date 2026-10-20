package com.epicmems.stride.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.epicmems.stride.domain.DayStat
import com.epicmems.stride.domain.StrideUiState
import com.epicmems.stride.ui.components.DarkCard
import com.epicmems.stride.ui.theme.Lime
import com.epicmems.stride.ui.theme.OnPastel
import com.epicmems.stride.ui.theme.Surface2
import com.epicmems.stride.ui.theme.Surface3
import com.epicmems.stride.ui.theme.TextMuted
import com.epicmems.stride.ui.theme.TextPrimary
import com.epicmems.stride.util.Dates

@Composable
fun CalendarScreen(
    state: StrideUiState,
    onToggleDay: (java.time.LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("Calendar", color = TextPrimary, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${state.completedThisMonth} days completed this month",
            color = TextMuted,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(18.dp))

        DarkCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MonthButton(Icons.Rounded.ChevronLeft, "Previous month", onPreviousMonth)
                Text(
                    text = "${Dates.monthName(state.month)} ${state.month.year}",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleLarge
                )
                MonthButton(Icons.Rounded.ChevronRight, "Next month", onNextMonth)
            }

            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth()) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        color = TextMuted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            val firstOfMonth = state.month.withDayOfMonth(1)
            val offset = firstOfMonth.dayOfWeek.value % 7 // Sunday-first
            val totalCells = offset + state.monthDays.size
            val rows = (totalCells + 6) / 7

            for (row in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (column in 0 until 7) {
                        val cellIndex = row * 7 + column
                        val dayIndex = cellIndex - offset
                        if (dayIndex in state.monthDays.indices) {
                            val day = state.monthDays[dayIndex]
                            DayCell(
                                day = day,
                                modifier = Modifier.weight(1f),
                                onClick = { onToggleDay(day.date) }
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot(Lime, "Goal reached")
                LegendDot(Surface2, "Marked by hand")
            }
        }

        Spacer(Modifier.height(14.dp))

        DarkCard(modifier = Modifier.fillMaxWidth()) {
            Text("Mark your days", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Tap any date to cross it off by hand — useful when you walked but the " +
                    "phone was charging, or you simply want to keep your own streak.",
                color = TextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun MonthButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Surface2)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = TextPrimary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun DayCell(day: DayStat, modifier: Modifier, onClick: () -> Unit) {
    val background = when {
        day.goalHit -> Lime
        day.marked -> Surface2
        day.steps > 0 -> Surface3.copy(alpha = 0.6f)
        else -> Surface3.copy(alpha = 0.28f)
    }
    val textColor = if (day.goalHit) OnPastel else TextPrimary
    val crossColor = if (day.goalHit) OnPastel else Lime

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${day.date.dayOfMonth}",
            color = textColor,
            style = MaterialTheme.typography.labelMedium
        )
        if (day.marked) {
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .padding(9.dp)
            ) {
                drawLine(
                    color = crossColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = crossColor,
                    start = Offset(size.width, 0f),
                    end = Offset(0f, size.height),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Text(label, color = TextMuted, style = MaterialTheme.typography.labelSmall)
    }
}

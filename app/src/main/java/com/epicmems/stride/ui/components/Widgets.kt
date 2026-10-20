package com.epicmems.stride.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.epicmems.stride.ui.theme.Accent
import com.epicmems.stride.ui.theme.CardWhite
import com.epicmems.stride.ui.theme.DotFont
import com.epicmems.stride.ui.theme.HandFont
import com.epicmems.stride.ui.theme.Ink
import com.epicmems.stride.ui.theme.InkSoft
import com.epicmems.stride.ui.theme.MarkerFont
import com.epicmems.stride.ui.theme.TrackGray
import com.epicmems.stride.util.Dates
import java.time.LocalDate

/** Big dot-matrix numeral / text. */
@Composable
fun DotText(
    text: String,
    size: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontFamily = DotFont,
        fontSize = size,
        lineHeight = size * 1.05f
    )
}

/** Handwritten accent text. */
@Composable
fun HandText(
    text: String,
    size: TextUnit = 20.sp,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    weight: FontWeight = FontWeight.Normal
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontFamily = HandFont,
        fontSize = size,
        lineHeight = size * 1.15f,
        fontWeight = weight
    )
}

/** Marker-style label text. */
@Composable
fun MarkerText(
    text: String,
    size: TextUnit = 14.sp,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontFamily = MarkerFont,
        fontSize = size,
        lineHeight = size * 1.2f
    )
}

/** Rounded pill progress bar. */
@Composable
fun PillBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 18.dp,
    track: Color = TrackGray,
    fill: Color = Ink
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .height(height)
            .clip(CircleShape)
            .background(track)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(clamped)
                .clip(CircleShape)
                .background(fill)
        )
    }
}

/** The Sunday-first day strip from the reference, with today highlighted. */
@Composable
fun WeekStrip(
    days: List<LocalDate>,
    today: LocalDate,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Ink)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        days.forEach { day ->
            val isToday = day == today
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .then(if (isToday) Modifier.background(CardWhite) else Modifier)
                    .padding(vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                MarkerText(
                    text = Dates.weekdayShort(day),
                    size = 10.sp,
                    color = if (isToday) Ink else CardWhite
                )
            }
        }
    }
}

/** A small stat tile: label + big dot value, optionally inverted. */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    inverted: Boolean = false,
    icon: (@Composable () -> Unit)? = null
) {
    InkCard(
        modifier = modifier,
        inverted = inverted,
        contentPadding = PaddingValues(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (icon != null) {
                Box(Modifier.size(26.dp)) { icon() }
            }
            Column {
                MarkerText(
                    text = label,
                    size = 13.sp,
                    color = if (inverted) CardWhite.copy(alpha = 0.85f) else InkSoft
                )
                DotText(
                    text = value,
                    size = 24.sp,
                    color = if (inverted) CardWhite else Ink
                )
            }
        }
    }
}

/** Handwritten section heading. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    HandText(text = text, size = 26.sp, modifier = modifier, color = Ink)
}

/** Small outlined chip marking that demo data is present. */
@Composable
fun DemoChip(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .border(1.dp, Accent, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        MarkerText(text = "demo data", size = 12.sp, color = Accent)
    }
}

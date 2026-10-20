package com.epicmems.stride.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.epicmems.stride.domain.StrideUiState
import com.epicmems.stride.ui.components.DemoChip
import com.epicmems.stride.ui.components.DotText
import com.epicmems.stride.ui.components.HandText
import com.epicmems.stride.ui.components.InkCard
import com.epicmems.stride.ui.components.LeavesDoodle
import com.epicmems.stride.ui.components.MarkerText
import com.epicmems.stride.ui.components.PillBar
import com.epicmems.stride.ui.components.StatTile
import com.epicmems.stride.ui.components.WalkDoodle
import com.epicmems.stride.ui.components.WeekStrip
import com.epicmems.stride.ui.theme.Accent
import com.epicmems.stride.ui.theme.CardWhite
import com.epicmems.stride.ui.theme.Ink
import com.epicmems.stride.ui.theme.InkSoft
import com.epicmems.stride.util.Dates

@Composable
fun TodayScreen(state: StrideUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            CalendarCard(state, Modifier.weight(1.25f))
            QuoteCard(Modifier.weight(1f))
        }
        HeroStepsCard(state)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            StatTile("7-day avg", "%,d".format(state.avg7), Modifier.weight(1f))
            StatTile("Streak", "${state.streak} d", Modifier.weight(1f), inverted = true)
        }
        RemainingCard(state)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            StatTile("30-day avg", "%,d".format(state.avg30), Modifier.weight(1f))
            StatTile("Best day", "%,d".format(state.best?.steps ?: 0), Modifier.weight(1f))
        }
        DoodleScene()
        if (state.demoActive) {
            DemoChip(Modifier.padding(start = 2.dp))
        }
    }
}

@Composable
private fun CalendarCard(state: StrideUiState, modifier: Modifier) {
    InkCard(modifier) {
        WeekStrip(
            days = Dates.weekOf(state.today),
            today = state.today,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Ink)
                    .padding(horizontal = 10.dp, vertical = 2.dp)
            ) {
                DotText(state.today.dayOfMonth.toString(), 38.sp, color = CardWhite)
            }
            Column {
                MarkerText(Dates.prettyDate(state.today), 15.sp, color = InkSoft)
                MarkerText(Dates.prettyWeekday(state.today), 15.sp, color = InkSoft)
            }
        }
    }
}

@Composable
private fun QuoteCard(modifier: Modifier) {
    InkCard(modifier) {
        HandText("Every step\ncounts — even\nthe small\nones.", 22.sp, color = Ink)
    }
}

@Composable
private fun HeroStepsCard(state: StrideUiState) {
    InkCard(
        modifier = Modifier.fillMaxWidth(),
        inverted = true,
        contentPadding = PaddingValues(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            WalkDoodle(Modifier.size(54.dp), color = CardWhite)
            Spacer(Modifier.width(12.dp))
            MarkerText("Today steps", 16.sp, color = CardWhite.copy(alpha = 0.85f))
            Spacer(Modifier.weight(1f))
            MarkerText("${state.goal / 1000}k goal", 13.sp, color = CardWhite.copy(alpha = 0.75f))
        }
        Spacer(Modifier.height(6.dp))
        DotText("%,d".format(state.todaySteps), 44.sp, color = CardWhite)
        Spacer(Modifier.height(14.dp))
        PillBar(
            progress = state.todaySteps.toFloat() / state.goal.coerceAtLeast(1),
            height = 16.dp,
            track = CardWhite.copy(alpha = 0.22f),
            fill = Accent
        )
    }
}

@Composable
private fun RemainingCard(state: StrideUiState) {
    InkCard(Modifier.fillMaxWidth()) {
        PillBar(
            progress = state.today.dayOfMonth.toFloat() / state.today.lengthOfMonth().toFloat(),
            fill = Ink
        )
        Spacer(Modifier.height(6.dp))
        MarkerText("This month left %02d days".format(Dates.daysLeftInMonth(state.today)), 15.sp, color = Ink)
        Spacer(Modifier.height(14.dp))
        PillBar(
            progress = state.today.dayOfYear.toFloat() / state.today.lengthOfYear().toFloat(),
            fill = Ink
        )
        Spacer(Modifier.height(6.dp))
        MarkerText("This year left %02d days".format(Dates.daysLeftInYear(state.today)), 15.sp, color = Ink)
    }
}

@Composable
private fun DoodleScene() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
    ) {
        LeavesDoodle(Modifier.fillMaxSize())
        WalkDoodle(
            Modifier
                .align(Alignment.CenterEnd)
                .size(130.dp)
                .padding(8.dp)
        )
    }
}

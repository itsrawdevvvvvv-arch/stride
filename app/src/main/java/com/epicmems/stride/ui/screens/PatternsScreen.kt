package com.epicmems.stride.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.epicmems.stride.domain.StrideUiState
import com.epicmems.stride.ui.components.DemoChip
import com.epicmems.stride.ui.components.HourHeat
import com.epicmems.stride.ui.components.InkCard
import com.epicmems.stride.ui.components.MarkerText
import com.epicmems.stride.ui.components.MonthHeat
import com.epicmems.stride.ui.components.SectionTitle
import com.epicmems.stride.ui.components.Sparkline
import com.epicmems.stride.ui.components.StatTile
import com.epicmems.stride.ui.components.WeeklyBars
import com.epicmems.stride.ui.theme.InkSoft
import com.epicmems.stride.util.Dates

@Composable
fun PatternsScreen(state: StrideUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionTitle("Step patterns")

        InkCard(Modifier.fillMaxWidth()) {
            MarkerText("Last 7 days", 15.sp, color = InkSoft)
            Spacer(Modifier.height(10.dp))
            WeeklyBars(state.last7, Modifier.fillMaxWidth().height(140.dp))
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                state.last7.forEach { stat ->
                    MarkerText(Dates.weekdayShort(stat.date).take(1), 11.sp, color = InkSoft)
                }
            }
        }

        InkCard(Modifier.fillMaxWidth()) {
            MarkerText("Today, hour by hour", 15.sp, color = InkSoft)
            Spacer(Modifier.height(10.dp))
            HourHeat(state.todayHours, Modifier.fillMaxWidth().height(88.dp))
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MarkerText("12a", 10.sp, color = InkSoft)
                MarkerText("6a", 10.sp, color = InkSoft)
                MarkerText("12p", 10.sp, color = InkSoft)
                MarkerText("6p", 10.sp, color = InkSoft)
                MarkerText("12a", 10.sp, color = InkSoft)
            }
        }

        InkCard(Modifier.fillMaxWidth()) {
            MarkerText("This month", 15.sp, color = InkSoft)
            Spacer(Modifier.height(10.dp))
            MonthHeat(state.monthDays, state.today, Modifier.fillMaxWidth().height(170.dp))
        }

        InkCard(Modifier.fillMaxWidth()) {
            MarkerText("30-day trend", 15.sp, color = InkSoft)
            Spacer(Modifier.height(10.dp))
            Sparkline(state.last30, Modifier.fillMaxWidth().height(90.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            StatTile("7-day avg", "%,d".format(state.avg7), Modifier.weight(1f))
            StatTile("30-day avg", "%,d".format(state.avg30), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            StatTile("Goal hit", "${state.goalHitRate30}%", Modifier.weight(1f), inverted = true)
            StatTile("Streak", "${state.streak} d", Modifier.weight(1f))
        }

        if (state.demoActive) {
            DemoChip(Modifier.padding(start = 2.dp))
        }
        Spacer(Modifier.height(8.dp))
    }
}

package com.epicmems.stride.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.epicmems.stride.domain.DayStat
import com.epicmems.stride.domain.StrideUiState
import com.epicmems.stride.ui.components.DotText
import com.epicmems.stride.ui.components.InkCard
import com.epicmems.stride.ui.components.MarkerText
import com.epicmems.stride.ui.components.PillBar
import com.epicmems.stride.ui.components.SectionTitle
import com.epicmems.stride.ui.theme.Accent
import com.epicmems.stride.ui.theme.Ink
import com.epicmems.stride.ui.theme.InkSoft
import com.epicmems.stride.util.Dates

@Composable
fun HistoryScreen(state: StrideUiState) {
    val days = state.last30.reversed()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { SectionTitle("History") }
        items(days, key = { it.date.toString() }) { day ->
            HistoryRow(day)
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun HistoryRow(day: DayStat) {
    InkCard(Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                MarkerText(Dates.prettyWeekday(day.date), 16.sp, color = Ink)
                MarkerText(Dates.prettyDate(day.date), 13.sp, color = InkSoft)
            }
            DotText("%,d".format(day.steps), 22.sp, color = Ink)
        }
        Spacer(Modifier.height(10.dp))
        PillBar(
            progress = day.progress,
            height = 12.dp,
            fill = if (day.goalHit) Accent else Ink
        )
    }
}

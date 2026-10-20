package com.epicmems.stride.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.epicmems.stride.ui.screens.HistoryScreen
import com.epicmems.stride.ui.screens.PatternsScreen
import com.epicmems.stride.ui.screens.SettingsScreen
import com.epicmems.stride.ui.screens.TodayScreen
import com.epicmems.stride.ui.theme.CardWhite
import com.epicmems.stride.ui.theme.Ink
import com.epicmems.stride.ui.theme.InkLine
import com.epicmems.stride.ui.theme.InkSoft
import com.epicmems.stride.ui.theme.MarkerFont
import com.epicmems.stride.ui.theme.Paper

private data class Tab(val label: String, val icon: ImageVector)

@Composable
fun StrideRoot(
    viewModel: StrideViewModel,
    onRequestPermission: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableIntStateOf(0) }

    val tabs = listOf(
        Tab("Today", Icons.Filled.Home),
        Tab("Patterns", Icons.Filled.BarChart),
        Tab("History", Icons.Filled.List),
        Tab("Settings", Icons.Filled.Settings)
    )

    Scaffold(
        containerColor = Paper,
        bottomBar = {
            StrideBottomBar(tabs = tabs, selected = selected, onSelect = { selected = it })
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            when (selected) {
                0 -> TodayScreen(state)
                1 -> PatternsScreen(state)
                2 -> HistoryScreen(state)
                else -> SettingsScreen(state, viewModel, onRequestPermission)
            }
        }
    }
}

@Composable
private fun StrideBottomBar(
    tabs: List<Tab>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Paper)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, tab ->
            val active = index == selected
            Surface(
                onClick = { onSelect(index) },
                shape = CircleShape,
                color = if (active) Ink else CardWhite,
                border = BorderStroke(1.5.dp, InkLine),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (active) CardWhite else Ink,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = tab.label,
                        fontFamily = MarkerFont,
                        fontSize = 11.sp,
                        color = if (active) CardWhite else InkSoft
                    )
                }
            }
        }
    }
}

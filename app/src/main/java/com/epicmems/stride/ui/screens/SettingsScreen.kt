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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.epicmems.stride.domain.StrideUiState
import com.epicmems.stride.ui.StrideViewModel
import com.epicmems.stride.ui.components.DotText
import com.epicmems.stride.ui.components.HandText
import com.epicmems.stride.ui.components.InkCard
import com.epicmems.stride.ui.components.MarkerText
import com.epicmems.stride.ui.components.SectionTitle
import com.epicmems.stride.ui.theme.CardWhite
import com.epicmems.stride.ui.theme.Ink
import com.epicmems.stride.ui.theme.InkSoft

@Composable
fun SettingsScreen(
    state: StrideUiState,
    viewModel: StrideViewModel,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionTitle("Settings")

        InkCard(Modifier.fillMaxWidth()) {
            MarkerText("Daily goal", 16.sp, color = Ink)
            Spacer(Modifier.height(4.dp))
            DotText("%,d".format(state.goal), 32.sp, color = Ink)
            Spacer(Modifier.height(6.dp))
            var localGoal by remember(state.goal) { mutableFloatStateOf(state.goal.toFloat()) }
            Slider(
                value = localGoal,
                onValueChange = { localGoal = it },
                onValueChangeFinished = { viewModel.setGoal(localGoal.toInt()) },
                valueRange = 1_000f..30_000f,
                steps = 28
            )
        }

        InkCard(Modifier.fillMaxWidth()) {
            MarkerText("Step sensor", 16.sp, color = Ink)
            Spacer(Modifier.height(6.dp))
            MarkerText(
                text = if (state.hasSensor) {
                    "Step counter detected — counting is active."
                } else {
                    "This device has no hardware step counter."
                },
                14.sp,
                color = InkSoft
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onRequestPermission) {
                Text("Grant activity permission")
            }
        }

        InkCard(Modifier.fillMaxWidth()) {
            MarkerText("Demo data", 16.sp, color = Ink)
            Spacer(Modifier.height(6.dp))
            MarkerText(
                "Sample history so the charts and heatmaps look alive before your own data builds up.",
                13.sp,
                color = InkSoft
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { viewModel.regenerateDemo() }) {
                    Text("Regenerate")
                }
                OutlinedButton(onClick = { viewModel.clearDemo() }) {
                    Text("Clear demo")
                }
            }
        }

        InkCard(Modifier.fillMaxWidth(), inverted = true) {
            HandText("Stride", 28.sp, color = CardWhite)
            Spacer(Modifier.height(4.dp))
            MarkerText(
                "An open-source step tracker. Everything stays on your device — no account, no network.",
                13.sp,
                color = CardWhite.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(8.dp))
            MarkerText(
                "Fonts: DotGothic16, Caveat, Patrick Hand (SIL Open Font License).",
                12.sp,
                color = CardWhite.copy(alpha = 0.7f)
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

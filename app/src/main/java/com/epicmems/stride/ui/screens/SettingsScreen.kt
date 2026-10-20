package com.epicmems.stride.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.epicmems.stride.data.ExportWriter
import com.epicmems.stride.domain.StrideUiState
import com.epicmems.stride.ui.StrideViewModel
import com.epicmems.stride.ui.components.DarkCard
import com.epicmems.stride.ui.components.GhostButton
import com.epicmems.stride.ui.components.PastelCard
import com.epicmems.stride.ui.theme.Lime
import com.epicmems.stride.ui.theme.OnPastel
import com.epicmems.stride.ui.theme.Surface2
import com.epicmems.stride.ui.theme.Surface3
import com.epicmems.stride.ui.theme.TextMuted
import com.epicmems.stride.ui.theme.TextPrimary
import com.epicmems.stride.util.BatteryOptimization
import com.epicmems.stride.util.Format
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(state: StrideUiState, viewModel: StrideViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) scope.launch { ExportWriter.write(context, uri, viewModel.exportCsv()) }
    }
    val jsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) scope.launch { ExportWriter.write(context, uri, viewModel.exportJson()) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("Settings", color = TextPrimary, style = MaterialTheme.typography.headlineLarge)

        // Pick up a battery-exemption change made in the system dialog.
        LaunchedEffect(Unit) { viewModel.refreshBatteryState() }

        Spacer(Modifier.height(18.dp))

        // ---- daily goal ----
        DarkCard(modifier = Modifier.fillMaxWidth()) {
            Text("Daily goal", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))

            // The readout follows the thumb *live* while dragging; the value is only persisted
            // when the drag ends, so we don't hammer DataStore on every frame.
            var localGoal by remember(state.goal) { mutableFloatStateOf(state.goal.toFloat()) }
            Text(
                text = Format.steps(localGoal.roundToInt()),
                color = Lime,
                style = MaterialTheme.typography.displayMedium
            )
            Slider(
                value = localGoal,
                onValueChange = { localGoal = it },
                onValueChangeFinished = { viewModel.setGoal(localGoal.roundToInt()) },
                valueRange = 1_000f..30_000f,
                steps = 28,
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = Lime,
                    activeTrackColor = Lime,
                    inactiveTrackColor = Surface3
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("1,000", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                Text("30,000", color = TextMuted, style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---- units ----
        DarkCard(modifier = Modifier.fillMaxWidth()) {
            Text("Units", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                UnitButton("km", state.unit == "km", Modifier.weight(1f)) { viewModel.setUnit("km") }
                UnitButton("mi", state.unit == "mi", Modifier.weight(1f)) { viewModel.setUnit("mi") }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---- notifications ----
        DarkCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Ongoing notification", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Shows today's step count while tracking runs in the background.",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(Modifier.size(12.dp))
                Switch(
                    checked = state.notificationsEnabled,
                    onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OnPastel,
                        checkedTrackColor = Lime,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = Surface3
                    )
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---- always-on tracking ----
        DarkCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Always-on tracking",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Keeps counting when Stride is closed or swiped out of Recents.",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(Modifier.size(12.dp))
                Switch(
                    checked = state.trackingEnabled,
                    onCheckedChange = { viewModel.setTrackingEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OnPastel,
                        checkedTrackColor = Lime,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = Surface3
                    )
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---- battery optimisation ----
        DarkCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Battery optimisation",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (state.batteryExempt) {
                    "Stride is exempt — the system won't put counting to sleep."
                } else {
                    "Many phones pause background apps. Exempting Stride keeps counting reliable " +
                        "even after you close the app."
                },
                color = TextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(14.dp))
            if (state.batteryExempt) {
                Text("Active", color = Lime, style = MaterialTheme.typography.labelLarge)
            } else {
                GhostButton(
                    text = "Allow background running",
                    onClick = {
                        runCatching {
                            context.startActivity(BatteryOptimization.requestIntent(context))
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---- export ----
        DarkCard(modifier = Modifier.fillMaxWidth()) {
            Text("Export your data", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Everything stays on this device. Export whenever you like — you own the data.",
                color = TextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton(
                    text = "Steps CSV",
                    modifier = Modifier.weight(1f),
                    onClick = { csvLauncher.launch("stride-steps.csv") },
                    leading = { Icon(Icons.Rounded.FileDownload, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp)) }
                )
                GhostButton(
                    text = "All JSON",
                    modifier = Modifier.weight(1f),
                    onClick = { jsonLauncher.launch("stride-export.json") },
                    leading = { Icon(Icons.Rounded.FileDownload, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp)) }
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---- data ----
        DarkCard(modifier = Modifier.fillMaxWidth()) {
            Text("Data", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${state.last30.count { it.steps > 0 }} active days · ${state.routes.size} routes stored",
                color = TextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(14.dp))
            GhostButton(
                text = "Load sample data",
                onClick = { viewModel.loadSampleData() },
                leading = { Icon(Icons.Rounded.Science, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp)) }
            )
            Spacer(Modifier.height(10.dp))
            GhostButton(
                text = "Clear all data",
                onClick = { viewModel.clearAllData() },
                leading = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp)) }
            )
        }

        Spacer(Modifier.height(14.dp))

        // ---- widget hint ----
        PastelCard(color = Lime, modifier = Modifier.fillMaxWidth()) {
            Text("Home-screen widget", color = OnPastel, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Long-press your home screen → Widgets → Stride to pin today's steps. " +
                    "It refreshes while tracking is running.",
                color = OnPastel.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(14.dp))

        DarkCard(modifier = Modifier.fillMaxWidth()) {
            Text("About Stride", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "An open-source step and route tracker. No account, no cloud, no ads. " +
                    "Steps come from the hardware counter; routes from GPS.",
                color = TextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Poppins typeface · SIL Open Font License · MIT licensed",
                color = TextMuted,
                style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun UnitButton(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(CircleShape)
            .background(if (selected) Lime else Surface2)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) OnPastel else TextPrimary,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

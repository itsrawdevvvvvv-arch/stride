package com.epicmems.stride.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.epicmems.stride.domain.StrideUiState
import com.epicmems.stride.ui.components.DarkCard
import com.epicmems.stride.ui.components.GhostButton
import com.epicmems.stride.ui.components.PastelCard
import com.epicmems.stride.ui.components.PrimaryButton
import com.epicmems.stride.ui.components.StepRing
import com.epicmems.stride.ui.components.WeekBars
import com.epicmems.stride.ui.theme.Lime
import com.epicmems.stride.ui.theme.OnPastel
import com.epicmems.stride.ui.theme.Pink
import com.epicmems.stride.ui.theme.Surface3
import com.epicmems.stride.ui.theme.TextMuted
import com.epicmems.stride.ui.theme.TextPrimary
import com.epicmems.stride.util.Format

@Composable
fun TodayScreen(
    state: StrideUiState,
    onRequestTrackingPermission: () -> Unit,
    onStartRoute: () -> Unit,
    onStopRoute: () -> Unit
) {
    val progress = if (state.goal <= 0) 0f else state.todaySteps.toFloat() / state.goal

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // ---- header ----
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Pink),
                contentAlignment = Alignment.Center
            ) {
                Text("EM", color = OnPastel, style = MaterialTheme.typography.titleMedium)
            }
            Text("Stride", color = TextPrimary, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.weight(1f))
            InlineStat(Icons.Rounded.LocalFireDepartment, "${state.streak}", Lime)
            Spacer(Modifier.width(12.dp))
            InlineStat(Icons.Rounded.Bolt, Format.steps(state.todaySteps), Pink)
        }

        Spacer(Modifier.height(26.dp))
        Text(
            text = "How many steps\ntoday?",
            color = TextPrimary,
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(Modifier.height(18.dp))

        // ---- hero ----
        PastelCard(color = Pink, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = Format.steps(state.todaySteps),
                        color = OnPastel,
                        style = MaterialTheme.typography.displayLarge
                    )
                    Text(
                        text = "of ${Format.steps(state.goal)} steps",
                        color = OnPastel.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                StepRing(
                    progress = progress,
                    trackColor = OnPastel.copy(alpha = 0.15f),
                    ringColor = OnPastel,
                    ringSize = 78.dp,
                    stroke = 9.dp
                )
            }
            Spacer(Modifier.height(16.dp))
            ProgressPill(progress = progress, track = OnPastel.copy(alpha = 0.15f), fill = OnPastel)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PastelChip("${(progress * 100).toInt()}% of goal")
                PastelChip(Format.distance(state.todayDistanceMeters, state.unit))
            }
        }

        Spacer(Modifier.height(14.dp))

        // ---- week ----
        DarkCard(modifier = Modifier.fillMaxWidth()) {
            Text("This week", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${state.last7.count { it.goalHit }} of 7 days cleared the goal",
                color = TextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            WeekBars(
                days = state.last7,
                goal = state.goal,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                barColor = TextPrimary,
                trackColor = Surface3,
                accentColor = Lime
            )
        }

        Spacer(Modifier.height(14.dp))

        // ---- route control ----
        if (state.recording.recording) {
            PrimaryButton(
                text = "Stop route  ·  ${Format.distance(state.recording.distanceMeters, state.unit)}",
                onClick = onStopRoute,
                leading = { Icon(Icons.Rounded.Stop, contentDescription = null, tint = OnPastel, modifier = Modifier.size(20.dp)) }
            )
        } else {
            PrimaryButton(
                text = "Start a route",
                onClick = onStartRoute,
                leading = { Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = OnPastel, modifier = Modifier.size(20.dp)) }
            )
        }

        Spacer(Modifier.height(10.dp))

        if (!state.hasStepSensor) {
            DarkCard(modifier = Modifier.fillMaxWidth()) {
                Text("No step sensor found", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "This device has no hardware step counter. Stride can still record GPS routes, " +
                        "but automatic step counting will stay at zero.",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(14.dp))
                GhostButton(text = "Grant activity permission", onClick = onRequestTrackingPermission)
            }
        } else if (state.todaySteps == 0) {
            DarkCard(modifier = Modifier.fillMaxWidth()) {
                Text("Start walking", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Put the phone in your pocket and walk. Stride counts steps in the " +
                        "background using the hardware step counter — no GPS needed for counting.",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun InlineStat(icon: ImageVector, value: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
        Text(value, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun PastelChip(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .clip(CircleShape)
            .background(OnPastel.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        color = OnPastel,
        style = MaterialTheme.typography.labelMedium
    )
}

@Composable
fun ProgressPill(
    progress: Float,
    track: Color,
    fill: Color,
    height: Dp = 12.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(track)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(CircleShape)
                .background(fill)
        )
    }
}

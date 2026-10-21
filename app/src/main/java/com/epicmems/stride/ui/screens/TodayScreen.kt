package com.epicmems.stride.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.epicmems.stride.domain.RecordingStatus
import com.epicmems.stride.domain.StrideUiState
import com.epicmems.stride.ui.components.AnimatedDistance
import com.epicmems.stride.ui.components.AnimatedSteps
import com.epicmems.stride.ui.components.DarkCard
import com.epicmems.stride.ui.components.GhostButton
import com.epicmems.stride.ui.components.PastelCard
import com.epicmems.stride.ui.components.ProgressPill
import com.epicmems.stride.ui.components.RecordingPulse
import com.epicmems.stride.ui.components.RouteCanvas
import com.epicmems.stride.ui.components.StatChip
import com.epicmems.stride.ui.components.StepRing
import com.epicmems.stride.ui.components.WeekBars
import com.epicmems.stride.ui.components.recordingStatusHint
import com.epicmems.stride.ui.components.recordingStatusLabel
import com.epicmems.stride.ui.theme.Lime
import com.epicmems.stride.ui.theme.OnPastel
import com.epicmems.stride.ui.theme.Pink
import com.epicmems.stride.ui.theme.Surface3
import com.epicmems.stride.ui.theme.TextMuted
import com.epicmems.stride.ui.theme.TextPrimary
import com.epicmems.stride.ui.theme.profileColor
import com.epicmems.stride.util.Format

@Composable
fun TodayScreen(
    state: StrideUiState,
    onRequestTrackingPermission: () -> Unit
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
                    .background(profileColor(state.profileColorIndex)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Format.initials(state.profileName),
                    color = OnPastel,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(state.profileName, color = TextPrimary, style = MaterialTheme.typography.titleLarge)
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

        // ---- hero: the counter rolls, the ring and bar ease ----
        PastelCard(color = Pink, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    AnimatedSteps(
                        value = state.todaySteps,
                        style = MaterialTheme.typography.displayLarge,
                        color = OnPastel
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
            ProgressPill(
                progress = progress,
                track = OnPastel.copy(alpha = 0.15f),
                fill = OnPastel
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PastelChip("${(progress * 100).toInt()}% of goal")
                PastelChip(Format.distance(state.todayDistanceMeters, state.unit))
            }
        }

        // ---- the route being drawn right now (fully automatic) ----
        if (state.recording.recording) {
            Spacer(Modifier.height(14.dp))
            LiveRouteCard(state = state)
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

        // ---- how recording works now ----
        DarkCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (state.autoRecordRoutes) "Recording on its own" else "Auto-recording is off",
                color = TextPrimary,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (state.autoRecordRoutes) {
                    "There is nothing to press. Stride notices when you start moving, draws the " +
                        "path, and saves it against the day. Indoors, where GPS can't reach, the " +
                        "path is estimated from your steps and direction."
                } else {
                    "Turn auto-recording back on in Settings if you want your walks saved again."
                },
                color = TextMuted,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(Modifier.height(10.dp))

        if (!state.hasStepSensor) {
            DarkCard(modifier = Modifier.fillMaxWidth()) {
                Text("No step sensor found", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "This device has no hardware step counter, so step counting and the " +
                        "indoor path estimate will stay empty.",
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
                        "background and records your route by itself — indoors it estimates the " +
                        "path from your steps and direction.",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

/** The route drawing, updating live as you move. */
@Composable
private fun LiveRouteCard(state: StrideUiState) {
    val recording = state.recording
    val hint = recordingStatusHint(recording.status)
    val label = if (recording.estimated) {
        "Estimated path (no GPS)"
    } else {
        recordingStatusLabel(recording.status)
    }

    DarkCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RecordingPulse(
                color = if (recording.status == RecordingStatus.TRACKING) Lime else Pink
            )
            Text(label, color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Text(
                text = "${recording.points.size} pts",
                color = TextMuted,
                style = MaterialTheme.typography.labelMedium
            )
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Lime.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            if (recording.points.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = recordingStatusLabel(recording.status),
                        color = TextMuted,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    if (hint != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = hint,
                            color = TextMuted.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                RouteCanvas(
                    points = recording.points,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    color = Lime,
                    strokeWidth = 3.dp
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        AnimatedDistance(
            meters = recording.distanceMeters,
            unit = state.unit,
            style = MaterialTheme.typography.displayMedium,
            color = Lime
        )

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip(Format.duration(recording.durationMillis))
            StatChip(if (recording.estimated) "estimated" else "GPS")
        }

        if (recording.estimated) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "GPS needs a clear view of the sky, so indoors the shape is rebuilt from " +
                    "your steps and compass heading. It's a good record of the walk, not a survey.",
                color = TextMuted.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
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

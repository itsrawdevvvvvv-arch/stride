package com.epicmems.stride.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.epicmems.stride.domain.StrideUiState
import com.epicmems.stride.ui.components.AnimatedDistance
import com.epicmems.stride.ui.components.AnimatedSteps
import com.epicmems.stride.ui.components.DarkCard
import com.epicmems.stride.ui.components.GhostButton
import com.epicmems.stride.ui.components.Motion
import com.epicmems.stride.ui.components.PastelCard
import com.epicmems.stride.ui.components.PrimaryButton
import com.epicmems.stride.ui.components.ProgressPill
import com.epicmems.stride.ui.components.RecordingPulse
import com.epicmems.stride.ui.components.RouteCanvas
import com.epicmems.stride.ui.components.StatChip
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

    // Screen entrance: fade + a short rise, driven from the draw phase so it costs no
    // recomposition while it plays.
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val enterAlpha = animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(Motion.BASE, easing = FastOutSlowInEasing),
        label = "enterAlpha"
    )
    val enterOffsetY = animateFloatAsState(
        targetValue = if (appeared) 0f else 26f,
        animationSpec = tween(Motion.BASE, easing = FastOutSlowInEasing),
        label = "enterOffset"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .graphicsLayer {
                alpha = enterAlpha.value
                translationY = enterOffsetY.value
            }
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

        // ---- live route drawing while recording ----
        if (state.recording.recording) {
            Spacer(Modifier.height(14.dp))
            LiveRouteCard(state = state, onStopRoute = onStopRoute)
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
                text = "Stop route",
                onClick = onStopRoute,
                leading = {
                    Icon(
                        Icons.Rounded.Stop,
                        contentDescription = null,
                        tint = OnPastel,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
        } else {
            PrimaryButton(
                text = "Start a route",
                onClick = onStartRoute,
                leading = {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = OnPastel,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
        }

        Spacer(Modifier.height(10.dp))

        if (!state.hasStepSensor) {
            DarkCard(modifier = Modifier.fillMaxWidth()) {
                Text("No step sensor found", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "This device has no hardware step counter. Stride can still record GPS " +
                        "routes, but automatic step counting will stay at zero.",
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

/** The route drawing, updating live as GPS fixes arrive. */
@Composable
private fun LiveRouteCard(state: StrideUiState, onStopRoute: () -> Unit) {
    DarkCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RecordingPulse(color = Lime)
            Text("Drawing your route", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Text(
                text = "${state.recording.points.size} pts",
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
            if (state.recording.points.size < 2) {
                Text(
                    text = "Waiting for a GPS fix…",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                RouteCanvas(
                    points = state.recording.points,
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
            meters = state.recording.distanceMeters,
            unit = state.unit,
            style = MaterialTheme.typography.displayMedium,
            color = Lime
        )

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip(Format.duration(state.recording.durationMillis))
            StatChip(if (state.recording.points.size < 2) "acquiring GPS" else "recording")
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

package com.epicmems.stride.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.epicmems.stride.data.local.RoutePointEntity
import com.epicmems.stride.domain.LatLng
import com.epicmems.stride.domain.RouteSummary
import com.epicmems.stride.domain.StrideUiState
import com.epicmems.stride.ui.components.AnimatedDistance
import com.epicmems.stride.ui.components.DarkCard
import com.epicmems.stride.ui.components.GhostButton
import com.epicmems.stride.ui.components.Motion
import com.epicmems.stride.ui.components.PastelCard
import com.epicmems.stride.ui.components.PrimaryButton
import com.epicmems.stride.ui.components.RecordingPulse
import com.epicmems.stride.ui.components.RouteCanvas
import com.epicmems.stride.ui.components.StatChip
import com.epicmems.stride.ui.theme.Lime
import com.epicmems.stride.ui.theme.OnPastel
import com.epicmems.stride.ui.theme.Peri
import com.epicmems.stride.ui.theme.TextMuted
import com.epicmems.stride.ui.theme.TextPrimary
import com.epicmems.stride.util.Dates
import com.epicmems.stride.util.Format
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RoutesScreen(
    state: StrideUiState,
    routePoints: (Long) -> Flow<List<RoutePointEntity>>,
    onOpenRoute: (Long) -> Unit,
    onStartRoute: () -> Unit,
    onStopRoute: () -> Unit
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val enterAlpha = animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(Motion.BASE, easing = FastOutSlowInEasing),
        label = "enterAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .graphicsLayer { alpha = enterAlpha.value }
            .padding(20.dp)
    ) {
        Text("Your routes", color = TextPrimary, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Walk with the phone in your pocket and Stride draws the path as you go.",
            color = TextMuted,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(18.dp))

        if (state.recording.recording) {
            PastelCard(color = Lime, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    RecordingPulse(color = OnPastel)
                    Text("Recording", color = OnPastel, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "${state.recording.points.size} pts",
                        color = OnPastel.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Spacer(Modifier.height(10.dp))

                AnimatedDistance(
                    meters = state.recording.distanceMeters,
                    unit = state.unit,
                    style = MaterialTheme.typography.displayMedium,
                    color = OnPastel
                )
                Text(
                    text = Format.duration(state.recording.durationMillis),
                    color = OnPastel.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(OnPastel.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.recording.points.size < 2) {
                        Text(
                            text = "Waiting for a GPS fix…",
                            color = OnPastel.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        RouteCanvas(
                            points = state.recording.points,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            color = OnPastel,
                            strokeWidth = 3.dp
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                GhostButton(
                    text = "Stop route",
                    onClick = onStopRoute,
                    leading = {
                        Icon(
                            Icons.Rounded.Stop,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }
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

        Spacer(Modifier.height(18.dp))

        if (state.routes.isEmpty()) {
            DarkCard(modifier = Modifier.fillMaxWidth()) {
                Text("No routes yet", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Tap the green button and start walking — your path is recorded and " +
                        "drawn here. You can export any route as a GPX file.",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            state.routes.forEach { route ->
                RouteRow(route = route, routePoints = routePoints, onClick = { onOpenRoute(route.id) })
                Spacer(Modifier.height(12.dp))
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun RouteRow(
    route: RouteSummary,
    routePoints: (Long) -> Flow<List<RoutePointEntity>>,
    onClick: () -> Unit
) {
    val points by routePoints(route.id).collectAsStateWithLifecycle(initialValue = emptyList())
    DarkCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = Dates.weekdayLong(route.startTime.toLocalDate()),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(route.startTime)),
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Peri.copy(alpha = 0.16f))
        ) {
            RouteCanvas(
                points = points.map { LatLng(it.lat, it.lng) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                color = Lime,
                strokeWidth = 2.5.dp
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip(Format.distance(route.distanceMeters, "km"))
            StatChip(Format.duration(route.durationMillis))
        }
    }
}

private fun Long.toLocalDate(): java.time.LocalDate =
    java.time.Instant.ofEpochMilli(this).atZone(java.time.ZoneId.systemDefault()).toLocalDate()

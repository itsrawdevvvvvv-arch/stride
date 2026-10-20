package com.epicmems.stride.ui.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.epicmems.stride.data.local.RoutePointEntity
import com.epicmems.stride.domain.LatLng
import com.epicmems.stride.domain.RouteSummary
import com.epicmems.stride.domain.StrideUiState
import com.epicmems.stride.ui.components.DarkCard
import com.epicmems.stride.ui.components.PastelCard
import com.epicmems.stride.ui.components.PrimaryButton
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("Your routes", color = TextPrimary, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Walk with the phone in your pocket and Stride draws the path.",
            color = TextMuted,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(18.dp))

        if (state.recording.recording) {
            PastelCard(color = Lime, modifier = Modifier.fillMaxWidth()) {
                Text("Recording", color = OnPastel, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiveChip(Format.distance(state.recording.distanceMeters, state.unit))
                    LiveChip(Format.duration(state.recording.durationMillis))
                    LiveChip("${state.recording.points.size} pts")
                }
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(OnPastel.copy(alpha = 0.08f))
                ) {
                    RouteCanvas(
                        points = state.recording.points,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        color = OnPastel
                    )
                }
                Spacer(Modifier.height(14.dp))
                PrimaryButton(
                    text = "Stop route",
                    onClick = onStopRoute,
                    leading = { Icon(Icons.Rounded.Stop, contentDescription = null, tint = OnPastel, modifier = Modifier.size(20.dp)) }
                )
            }
        } else {
            PrimaryButton(
                text = "Start a route",
                onClick = onStartRoute,
                leading = { Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = OnPastel, modifier = Modifier.size(20.dp)) }
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
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(22.dp))
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

@Composable
private fun LiveChip(text: String) {
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

private fun Long.toLocalDate(): java.time.LocalDate =
    java.time.Instant.ofEpochMilli(this).atZone(java.time.ZoneId.systemDefault()).toLocalDate()

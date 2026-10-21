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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.epicmems.stride.data.local.RoutePointEntity
import com.epicmems.stride.domain.LatLng
import com.epicmems.stride.domain.RouteSummary
import com.epicmems.stride.domain.StrideUiState
import com.epicmems.stride.ui.components.AnimatedDistance
import com.epicmems.stride.ui.components.DarkCard
import com.epicmems.stride.ui.components.PastelCard
import com.epicmems.stride.ui.components.RecordingPulse
import com.epicmems.stride.ui.components.RouteCanvas
import com.epicmems.stride.ui.components.StatChip
import com.epicmems.stride.ui.components.recordingStatusHint
import com.epicmems.stride.ui.components.recordingStatusLabel
import com.epicmems.stride.ui.theme.Lime
import com.epicmems.stride.ui.theme.OnPastel
import com.epicmems.stride.ui.theme.Peri
import com.epicmems.stride.ui.theme.TextMuted
import com.epicmems.stride.ui.theme.TextPrimary
import com.epicmems.stride.util.Dates
import com.epicmems.stride.util.Format
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

@Composable
fun RoutesScreen(
    state: StrideUiState,
    routePoints: (Long) -> Flow<List<RoutePointEntity>>,
    onOpenRoute: (Long) -> Unit
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
            text = "Recorded automatically as you walk — there's nothing to start or stop.",
            color = TextMuted,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(18.dp))

        if (state.recording.recording) {
            LiveSegmentCard(state)
            Spacer(Modifier.height(16.dp))
        }

        if (state.routes.isEmpty()) {
            DarkCard(modifier = Modifier.fillMaxWidth()) {
                Text("Nothing recorded yet", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Walk around for a minute and the path appears here on its own, " +
                        "saved against today's date. You can export any walk as a GPX file.",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            state.routes
                .groupBy { dayKeyOf(it.startTime) }
                .forEach { (day, routes) ->
                    DaySection(
                        day = day,
                        routes = routes,
                        routePoints = routePoints,
                        onOpenRoute = onOpenRoute
                    )
                    Spacer(Modifier.height(18.dp))
                }
        }

        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun LiveSegmentCard(state: StrideUiState) {
    val recording = state.recording
    val hint = recordingStatusHint(recording.status)

    PastelCard(color = Lime, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            RecordingPulse(color = OnPastel)
            Text(
                text = if (recording.estimated) "Estimating (indoors)" else recordingStatusLabel(recording.status),
                color = OnPastel,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${recording.points.size} pts",
                color = OnPastel.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelMedium
            )
        }

        Spacer(Modifier.height(10.dp))

        AnimatedDistance(
            meters = recording.distanceMeters,
            unit = state.unit,
            style = MaterialTheme.typography.displayMedium,
            color = OnPastel
        )
        Text(
            text = Format.duration(recording.durationMillis),
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
            if (recording.points.isEmpty()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = recordingStatusLabel(recording.status),
                        color = OnPastel.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    if (hint != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = hint,
                            color = OnPastel.copy(alpha = 0.6f),
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
                    color = OnPastel,
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

@Composable
private fun DaySection(
    day: String,
    routes: List<RouteSummary>,
    routePoints: (Long) -> Flow<List<RoutePointEntity>>,
    onOpenRoute: (Long) -> Unit
) {
    val totalMeters = routes.sumOf { it.distanceMeters }
    val totalMillis = routes.sumOf { it.durationMillis }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = dayLabel(day),
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = "${routes.size} ${if (routes.size == 1) "walk" else "walks"} · " +
                Format.distance(totalMeters, "km") + " · " + Format.duration(totalMillis),
            color = TextMuted,
            style = MaterialTheme.typography.labelMedium
        )
    }

    Spacer(Modifier.height(10.dp))

    routes.forEach { route ->
        RouteRow(route = route, routePoints = routePoints, onClick = { onOpenRoute(route.id) })
        Spacer(Modifier.height(12.dp))
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
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(route.startTime)),
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
                if (route.estimated) {
                    Text(
                        text = "estimated path (no GPS)",
                        color = Peri,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
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
            if (route.estimated) StatChip("estimated")
        }
    }
}

private fun dayKeyOf(startTime: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(startTime))

private fun dayLabel(key: String): String {
    val today = java.time.LocalDate.now()
    return when (key) {
        Dates.key(today) -> "Today"
        Dates.key(today.minusDays(1)) -> "Yesterday"
        else -> runCatching {
            Dates.parse(key).format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))
        }.getOrDefault(key)
    }
}

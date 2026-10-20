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
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.epicmems.stride.data.ExportWriter
import com.epicmems.stride.domain.LatLng
import com.epicmems.stride.ui.StrideViewModel
import com.epicmems.stride.ui.components.DarkCard
import com.epicmems.stride.ui.components.GhostButton
import com.epicmems.stride.ui.components.PrimaryButton
import com.epicmems.stride.ui.components.RouteCanvas
import com.epicmems.stride.ui.components.StatChip
import com.epicmems.stride.ui.theme.Lime
import com.epicmems.stride.ui.theme.OnPastel
import com.epicmems.stride.ui.theme.Peri
import com.epicmems.stride.ui.theme.TextMuted
import com.epicmems.stride.ui.theme.TextPrimary
import com.epicmems.stride.util.Format
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RouteDetailScreen(
    routeId: Long,
    viewModel: StrideViewModel,
    onBack: () -> Unit
) {
    val points by viewModel.routePoints(routeId).collectAsStateWithLifecycle(initialValue = emptyList())
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val route = state.routes.firstOrNull { it.id == routeId }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gpxLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/gpx+xml")
    ) { uri ->
        if (uri != null) {
            scope.launch { ExportWriter.write(context, uri, viewModel.exportGpx(routeId)) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(com.epicmems.stride.ui.theme.Surface2),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onBack)
                )
            }
            Text("Route", color = TextPrimary, style = MaterialTheme.typography.headlineMedium)
        }

        Spacer(Modifier.height(18.dp))

        DarkCard(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Peri.copy(alpha = 0.14f))
            ) {
                RouteCanvas(
                    points = points.map { LatLng(it.lat, it.lng) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    color = Lime,
                    strokeWidth = 3.dp
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip(Format.distance(route?.distanceMeters ?: 0.0, state.unit))
                StatChip(Format.duration(route?.durationMillis ?: 0L))
                StatChip("${points.size} points")
            }
            Spacer(Modifier.height(10.dp))
            route?.let {
                Text(
                    text = SimpleDateFormat("EEEE, MMM d · HH:mm", Locale.getDefault()).format(Date(it.startTime)),
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        PrimaryButton(
            text = "Export GPX",
            onClick = { gpxLauncher.launch("stride-route-$routeId.gpx") },
            leading = { Icon(Icons.Rounded.FileDownload, contentDescription = null, tint = OnPastel, modifier = Modifier.size(20.dp)) }
        )

        Spacer(Modifier.height(10.dp))

        GhostButton(
            text = "Delete route",
            onClick = {
                viewModel.deleteRoute(routeId)
                onBack()
            },
            leading = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp)) }
        )

        Spacer(Modifier.height(20.dp))
    }
}

package com.epicmems.stride.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.epicmems.stride.ui.components.Motion
import com.epicmems.stride.ui.components.ProfileDialog
import com.epicmems.stride.ui.components.pressScale
import com.epicmems.stride.ui.screens.CalendarScreen
import com.epicmems.stride.ui.screens.RouteDetailScreen
import com.epicmems.stride.ui.screens.RoutesScreen
import com.epicmems.stride.ui.screens.SettingsScreen
import com.epicmems.stride.ui.screens.TodayScreen
import com.epicmems.stride.ui.theme.Bg
import com.epicmems.stride.ui.theme.Lime
import com.epicmems.stride.ui.theme.OnPastel
import com.epicmems.stride.ui.theme.TextMuted
import com.epicmems.stride.ui.theme.profileColor
import com.epicmems.stride.util.Format

private data class Tab(val label: String, val icon: ImageVector)

@Composable
fun StrideRoot(
    viewModel: StrideViewModel,
    onRequestTrackingPermission: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(0) }
    var detailRouteId by rememberSaveable { mutableStateOf(-1L) }
    var showProfile by rememberSaveable { mutableStateOf(false) }

    if (showProfile) {
        ProfileDialog(
            name = state.profileName,
            colorIndex = state.profileColorIndex,
            onCommit = { name, color ->
                viewModel.setProfileName(name)
                viewModel.setProfileColorIndex(color)
            },
            onDismiss = { showProfile = false }
        )
    }

    val tabs = listOf(
        Tab("Today", Icons.Rounded.Home),
        Tab("Routes", Icons.Rounded.Map),
        Tab("Calendar", Icons.Rounded.CalendarMonth),
        Tab("Settings", Icons.Rounded.Settings)
    )

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            if (detailRouteId < 0L) {
                StrideBottomBar(
                    tabs = tabs,
                    selected = tab,
                    profileName = state.profileName,
                    profileColorIndex = state.profileColorIndex,
                    onSelect = { tab = it },
                    onProfile = { showProfile = true }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (detailRouteId >= 0L) {
                RouteDetailScreen(
                    routeId = detailRouteId,
                    viewModel = viewModel,
                    onBack = { detailRouteId = -1L }
                )
            } else {
                // Slide + fade between tabs instead of swapping instantly.
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        (
                            fadeIn(animationSpec = tween(Motion.FAST)) +
                                slideInVertically(
                                    animationSpec = tween(Motion.BASE),
                                    initialOffsetY = { fullHeight -> fullHeight / 26 }
                                )
                            ) togetherWith fadeOut(animationSpec = tween(Motion.FAST / 2))
                    },
                    label = "tabContent"
                ) { current ->
                    when (current) {
                        0 -> TodayScreen(
                            state = state,
                            onRequestTrackingPermission = onRequestTrackingPermission
                        )

                        1 -> RoutesScreen(
                            state = state,
                            routePoints = viewModel::routePoints,
                            onOpenRoute = { detailRouteId = it }
                        )

                        2 -> CalendarScreen(
                            state = state,
                            onToggleDay = { viewModel.toggleMarked(it) },
                            onPreviousMonth = { viewModel.previousMonth() },
                            onNextMonth = { viewModel.nextMonth() }
                        )

                        else -> SettingsScreen(
                            state = state,
                            viewModel = viewModel,
                            onEditProfile = { showProfile = true }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StrideBottomBar(
    tabs: List<Tab>,
    selected: Int,
    profileName: String,
    profileColorIndex: Int,
    onSelect: (Int) -> Unit,
    onProfile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Bg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        NavItem(tabs[0], selected == 0) { onSelect(0) }
        NavItem(tabs[1], selected == 1) { onSelect(1) }

        // The floating button is now your profile — tap it to edit your name and colour.
        val fabColor by animateColorAsState(
            targetValue = profileColor(profileColorIndex),
            animationSpec = tween(Motion.BASE),
            label = "fabColor"
        )
        val fabInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .offset(y = (-14).dp)
                .size(58.dp)
                .clip(CircleShape)
                .background(fabColor)
                .pressScale(fabInteraction, pressedScale = 0.92f)
                .clickable(
                    interactionSource = fabInteraction,
                    indication = null,
                    onClick = onProfile
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = Format.initials(profileName),
                color = OnPastel,
                style = MaterialTheme.typography.titleMedium
            )
        }

        NavItem(tabs[2], selected == 2) { onSelect(2) }
        NavItem(tabs[3], selected == 3) { onSelect(3) }
    }
}

@Composable
private fun NavItem(tab: Tab, active: Boolean, onClick: () -> Unit) {
    val tint by animateColorAsState(
        targetValue = if (active) Lime else TextMuted,
        animationSpec = tween(Motion.FAST),
        label = "navTint"
    )
    Column(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = tab.label,
            color = tint,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

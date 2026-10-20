package com.epicmems.stride.ui.components

import com.epicmems.stride.domain.RecordingStatus

/**
 * Human-readable explanation of what the route recorder is doing, so a failure is never
 * silent — the user sees *why* nothing is being drawn instead of an endless "waiting".
 */
fun recordingStatusLabel(status: RecordingStatus): String = when (status) {
    RecordingStatus.IDLE -> "Starting…"
    RecordingStatus.ACQUIRING -> "Searching for satellites"
    RecordingStatus.TRACKING -> "Drawing your route"
    RecordingStatus.LOCATION_OFF -> "Location is turned off"
    RecordingStatus.PERMISSION_MISSING -> "Location permission needed"
    RecordingStatus.UNAVAILABLE -> "Location unavailable"
}

/** A one-line remedy for the states the user can act on. */
fun recordingStatusHint(status: RecordingStatus): String? = when (status) {
    RecordingStatus.ACQUIRING -> "Move to open sky — the first fix can take a few seconds."
    RecordingStatus.LOCATION_OFF -> "Turn on Location in quick settings, then start the route again."
    RecordingStatus.PERMISSION_MISSING -> "Grant location permission to Stride, then start the route again."
    RecordingStatus.UNAVAILABLE -> "This device reports no usable location provider."
    else -> null
}

package com.epicmems.stride.track

import com.epicmems.stride.domain.LatLng
import com.epicmems.stride.domain.RecordingStatus
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory state of the route recorder, so the UI can draw the route as it is being walked
 * without re-reading the database on every point. The ViewModel combines these flows into the
 * state the screens render.
 */
object TrackingState {

    val recording = MutableStateFlow(false)
    val distanceMeters = MutableStateFlow(0.0)
    val durationMillis = MutableStateFlow(0L)
    val points = MutableStateFlow<List<LatLng>>(emptyList())
    val status = MutableStateFlow(RecordingStatus.IDLE)

    /** True while the path is being reconstructed from motion sensors (indoors, no GPS). */
    val estimated = MutableStateFlow(false)

    fun reset() {
        recording.value = false
        distanceMeters.value = 0.0
        durationMillis.value = 0L
        points.value = emptyList()
        status.value = RecordingStatus.IDLE
        estimated.value = false
    }
}

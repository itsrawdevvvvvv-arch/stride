package com.epicmems.stride.track

import com.epicmems.stride.domain.LatLng
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory state of the route recorder, so the UI can draw the route as it is being walked
 * without re-reading the database on every GPS fix. The ViewModel combines these flows into
 * the state the screens render.
 */
object TrackingState {

    val recording = MutableStateFlow(false)
    val distanceMeters = MutableStateFlow(0.0)
    val durationMillis = MutableStateFlow(0L)
    val points = MutableStateFlow<List<LatLng>>(emptyList())

    fun reset() {
        recording.value = false
        distanceMeters.value = 0.0
        durationMillis.value = 0L
        points.value = emptyList()
    }
}

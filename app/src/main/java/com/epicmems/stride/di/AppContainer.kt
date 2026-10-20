package com.epicmems.stride.di

import android.content.Context
import com.epicmems.stride.data.RouteRepository
import com.epicmems.stride.data.SettingsRepository
import com.epicmems.stride.data.StepRepository
import com.epicmems.stride.data.local.StrideDatabase
import com.epicmems.stride.track.StepSensorEngine

/**
 * Tiny hand-rolled dependency container — one instance lives on
 * [com.epicmems.stride.StrideApp]. Keeps the app free of a DI framework while staying
 * testable: repositories take their DAOs, and nothing reaches for a global singleton.
 */
class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    private val database = StrideDatabase.get(appContext)

    val settingsRepository = SettingsRepository(appContext)
    val stepRepository = StepRepository(database.stepDao(), settingsRepository)
    val routeRepository = RouteRepository(database.routeDao())
    val stepSensorEngine = StepSensorEngine(appContext, stepRepository, settingsRepository)
}

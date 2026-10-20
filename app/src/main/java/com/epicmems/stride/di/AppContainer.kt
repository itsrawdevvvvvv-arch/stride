package com.epicmems.stride.di

import android.content.Context
import com.epicmems.stride.data.SettingsRepository
import com.epicmems.stride.data.StepRepository
import com.epicmems.stride.data.local.StepDatabase
import com.epicmems.stride.step.StepSensorEngine

/**
 * Tiny hand-rolled dependency container. One instance lives on [com.epicmems.stride.StrideApp].
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val database = StepDatabase.get(appContext)

    val settingsRepository = SettingsRepository(appContext)
    val stepRepository = StepRepository(database.stepDao(), settingsRepository)
    val stepSensorEngine = StepSensorEngine(appContext, stepRepository, settingsRepository)
}

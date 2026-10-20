package com.epicmems.stride.widget

import android.content.Context
import com.epicmems.stride.data.SettingsRepository
import com.epicmems.stride.data.local.StrideDatabase
import com.epicmems.stride.util.Dates

/** Reads today's numbers once and pushes them to every widget instance. */
object WidgetUpdater {

    suspend fun updateAll(context: Context) {
        val dao = StrideDatabase.get(context).stepDao()
        val today = dao.day(Dates.key(Dates.today()))
        val steps = today?.steps ?: 0
        val goal = today?.goal ?: SettingsRepository(context).currentGoal()
        StepWidgetProvider.refresh(context, steps, goal)
    }
}

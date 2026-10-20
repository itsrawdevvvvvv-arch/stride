package com.epicmems.stride.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.epicmems.stride.MainActivity
import com.epicmems.stride.R
import com.epicmems.stride.data.SettingsRepository
import com.epicmems.stride.data.local.StrideDatabase
import com.epicmems.stride.util.Dates
import com.epicmems.stride.util.Format
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Home-screen widget showing today's steps and goal progress.
 * Uses classic RemoteViews so it works on every launcher with no extra dependency.
 */
class StepWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = StrideDatabase.get(context).stepDao()
                val settings = SettingsRepository(context)
                val today = dao.day(Dates.key(Dates.today()))
                val steps = today?.steps ?: 0
                val goal = today?.goal ?: settings.currentGoal()
                appWidgetIds.forEach { id ->
                    appWidgetManager.updateAppWidget(id, buildViews(context, steps, goal))
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {

        fun buildViews(context: Context, steps: Int, goal: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_steps)
            views.setTextViewText(R.id.widget_steps, Format.steps(steps))
            views.setTextViewText(R.id.widget_goal, "of " + Format.steps(goal) + " steps")
            val percent = ((steps.toFloat() / goal.coerceAtLeast(1)) * 100f)
                .toInt().coerceIn(0, 100)
            views.setProgressBar(R.id.widget_bar, 100, percent, false)

            val openApp = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_root, openApp)
            return views
        }

        /** Refreshes every placed widget. Called by the tracking service. */
        fun refresh(context: Context, steps: Int, goal: Int) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, StepWidgetProvider::class.java)
            )
            ids.forEach { id -> manager.updateAppWidget(id, buildViews(context, steps, goal)) }
        }
    }
}

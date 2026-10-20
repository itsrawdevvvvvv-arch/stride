package com.epicmems.stride.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "stride_settings")

/** User preferences + the persisted raw sensor baseline. All on-device. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val UNIT = stringPreferencesKey("unit")
        val DEMO_SEEDED = booleanPreferencesKey("demo_seeded")
        val LAST_RAW = floatPreferencesKey("last_raw_counter")
    }

    val dailyGoal: Flow<Int> = context.dataStore.data.map { it[Keys.DAILY_GOAL] ?: DEFAULT_GOAL }
    val unit: Flow<String> = context.dataStore.data.map { it[Keys.UNIT] ?: "km" }
    val demoSeeded: Flow<Boolean> = context.dataStore.data.map { it[Keys.DEMO_SEEDED] ?: false }

    suspend fun currentGoal(): Int = dailyGoal.first()

    suspend fun setDailyGoal(value: Int) {
        context.dataStore.edit { it[Keys.DAILY_GOAL] = value.coerceIn(1_000, 50_000) }
    }

    suspend fun setUnit(value: String) {
        context.dataStore.edit { it[Keys.UNIT] = value }
    }

    suspend fun setDemoSeeded(value: Boolean) {
        context.dataStore.edit { it[Keys.DEMO_SEEDED] = value }
    }

    suspend fun lastRaw(): Float =
        context.dataStore.data.first()[Keys.LAST_RAW] ?: 0f

    suspend fun setLastRaw(value: Float) {
        context.dataStore.edit { it[Keys.LAST_RAW] = value }
    }

    companion object {
        const val DEFAULT_GOAL = 8000
    }
}

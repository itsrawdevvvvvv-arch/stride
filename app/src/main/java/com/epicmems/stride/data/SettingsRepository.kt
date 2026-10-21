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

/** User preferences plus the persisted raw sensor baseline. All on-device. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val UNIT = stringPreferencesKey("unit")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val TRACKING_ENABLED = booleanPreferencesKey("tracking_enabled")
        val AUTO_ROUTES = booleanPreferencesKey("auto_record_routes")
        val PROFILE_NAME = stringPreferencesKey("profile_name")
        val PROFILE_COLOR = intPreferencesKey("profile_color")
        val LAST_RAW = floatPreferencesKey("last_raw_counter")
    }

    val dailyGoal: Flow<Int> = context.dataStore.data.map { it[Keys.DAILY_GOAL] ?: DEFAULT_GOAL }
    val unit: Flow<String> = context.dataStore.data.map { it[Keys.UNIT] ?: "km" }
    val notificationsEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.NOTIFICATIONS] ?: true }

    /** Always-on tracking. On by default — the whole point of the app. */
    val trackingEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.TRACKING_ENABLED] ?: true }

    /** Record routes automatically as you move, with no start/stop. */
    val autoRecordRoutes: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.AUTO_ROUTES] ?: true }

    val profileName: Flow<String> =
        context.dataStore.data.map { it[Keys.PROFILE_NAME] ?: DEFAULT_NAME }

    val profileColorIndex: Flow<Int> =
        context.dataStore.data.map { (it[Keys.PROFILE_COLOR] ?: 0).coerceIn(0, 3) }

    suspend fun currentGoal(): Int = dailyGoal.first()

    suspend fun isTrackingEnabled(): Boolean = trackingEnabled.first()

    suspend fun isAutoRecordEnabled(): Boolean = autoRecordRoutes.first()

    suspend fun setDailyGoal(value: Int) {
        context.dataStore.edit { it[Keys.DAILY_GOAL] = value.coerceIn(1_000, 50_000) }
    }

    suspend fun setUnit(value: String) {
        context.dataStore.edit { it[Keys.UNIT] = value }
    }

    suspend fun setNotificationsEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS] = value }
    }

    suspend fun setTrackingEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.TRACKING_ENABLED] = value }
    }

    suspend fun setAutoRecordRoutes(value: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_ROUTES] = value }
    }

    suspend fun setProfileName(value: String) {
        context.dataStore.edit { it[Keys.PROFILE_NAME] = value.trim().take(24) }
    }

    suspend fun setProfileColorIndex(value: Int) {
        context.dataStore.edit { it[Keys.PROFILE_COLOR] = value.coerceIn(0, 3) }
    }

    suspend fun lastRaw(): Float = context.dataStore.data.first()[Keys.LAST_RAW] ?: 0f

    suspend fun setLastRaw(value: Float) {
        context.dataStore.edit { it[Keys.LAST_RAW] = value }
    }

    companion object {
        const val DEFAULT_GOAL = 8000
        const val DEFAULT_NAME = "Stride"
    }
}

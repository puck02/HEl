package com.heldairy.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Data class for daily report settings
 */
data class DailyReportSettings(
    val reminderEnabled: Boolean = true  // Default to enabled
)

private val Context.dailyReportDataStore: DataStore<Preferences> by preferencesDataStore(name = "daily_report_settings")

/**
 * Preferences store for daily report settings
 */
class DailyReportPreferencesStore(private val context: Context) {
    
    private object PreferenceKeys {
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
    }
    
    /**
     * Flow of daily report settings
     */
    val settingsFlow: Flow<DailyReportSettings> = context.dailyReportDataStore.data.map { preferences ->
        DailyReportSettings(
            reminderEnabled = preferences[PreferenceKeys.REMINDER_ENABLED] ?: true
        )
    }
    
    /**
     * Flow of just the reminder enabled state
     */
    val reminderEnabledFlow: Flow<Boolean> = context.dailyReportDataStore.data.map { preferences ->
        preferences[PreferenceKeys.REMINDER_ENABLED] ?: true
    }
    
    /**
     * Update the reminder enabled state
     */
    suspend fun updateReminderEnabled(enabled: Boolean) {
        context.dailyReportDataStore.edit { preferences ->
            preferences[PreferenceKeys.REMINDER_ENABLED] = enabled
        }
    }
}

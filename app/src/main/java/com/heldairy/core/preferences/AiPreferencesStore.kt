package com.heldairy.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.aiSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_settings")

class AiPreferencesStore(context: Context) {
    private val dataStore = context.aiSettingsDataStore

    val settingsFlow: Flow<AiSettings> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { preferences ->
            AiSettings(
                apiKey = preferences[API_KEY] ?: "",
                aiEnabled = preferences[AI_ENABLED] ?: true,
                themeDark = preferences[THEME_DARK] ?: false
            )
        }

    suspend fun currentSettings(): AiSettings = settingsFlow.first()

    suspend fun updateApiKey(value: String) {
        dataStore.edit { prefs ->
            if (value.isBlank()) {
                prefs.remove(API_KEY)
            } else {
                prefs[API_KEY] = value.trim()
            }
        }
    }

    suspend fun updateEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[AI_ENABLED] = enabled
        }
    }

    suspend fun updateThemeDark(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[THEME_DARK] = enabled
        }
    }

    companion object {
        private val API_KEY = stringPreferencesKey("deepseek_api_key")
        private val AI_ENABLED = booleanPreferencesKey("deepseek_enabled")
        private val THEME_DARK = booleanPreferencesKey("theme_dark")
    }
}

data class AiSettings(
    val apiKey: String,
    val aiEnabled: Boolean,
    val themeDark: Boolean
)

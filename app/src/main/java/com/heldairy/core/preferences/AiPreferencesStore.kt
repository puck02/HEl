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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.aiSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_settings")

class AiPreferencesStore(
    context: Context,
    private val secureStore: SecurePreferencesStore = SecurePreferencesStore(context)
) {
    private val dataStore = context.aiSettingsDataStore

    /**
     * 合并加密存储的 API Key 和 DataStore 的其他设置
     */
    val settingsFlow: Flow<AiSettings> = combine(
        dataStore.data.catch { emit(emptyPreferences()) },
        secureStore.apiKeyFlow
    ) { preferences, secureApiKey ->
        // 如果 DataStore 中还有旧的明文 API Key，迁移到加密存储
        val oldApiKey = preferences[API_KEY_LEGACY]
        if (!oldApiKey.isNullOrBlank() && secureApiKey.isBlank()) {
            secureStore.saveApiKey(oldApiKey)
            // 清除旧的明文存储
            dataStore.edit { it.remove(API_KEY_LEGACY) }
        }
        
        AiSettings(
            apiKey = secureApiKey.ifBlank { oldApiKey ?: "" },
            aiEnabled = preferences[AI_ENABLED] ?: true,
            themeDark = preferences[THEME_DARK] ?: false
        )
    }

    suspend fun currentSettings(): AiSettings = settingsFlow.first()

    suspend fun updateApiKey(value: String) {
        // 使用加密存储保存 API Key
        secureStore.saveApiKey(value)
        // 清除旧的明文存储（如果存在）
        dataStore.edit { prefs ->
            prefs.remove(API_KEY_LEGACY)
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
        @Deprecated("Use SecurePreferencesStore instead")
        private val API_KEY_LEGACY = stringPreferencesKey("deepseek_api_key")
        private val AI_ENABLED = booleanPreferencesKey("deepseek_enabled")
        private val THEME_DARK = booleanPreferencesKey("theme_dark")
    }
}

data class AiSettings(
    val apiKey: String,
    val aiEnabled: Boolean,
    val themeDark: Boolean
)

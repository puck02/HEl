package com.heldairy.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.agentDataStore: DataStore<Preferences> by preferencesDataStore(name = "agent_settings")

/**
 * Agent 服务器连接与同步偏好存储
 *
 * 存储内容：
 * - 服务器地址
 * - JWT access/refresh token
 * - 同步开关 & 最后同步时间戳
 * - 已登录用户名
 */
class AgentPreferencesStore(context: Context) {

    private val dataStore = context.agentDataStore

    val settingsFlow: Flow<AgentSettings> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            AgentSettings(
                serverUrl = prefs[SERVER_URL] ?: "",
                accessToken = prefs[ACCESS_TOKEN] ?: "",
                refreshToken = prefs[REFRESH_TOKEN] ?: "",
                syncEnabled = prefs[SYNC_ENABLED] ?: false,
                lastSyncTimestamp = prefs[LAST_SYNC_TIMESTAMP] ?: 0L,
                loggedInUsername = prefs[LOGGED_IN_USERNAME] ?: "",
                agentEnabled = prefs[AGENT_ENABLED] ?: false
            )
        }

    suspend fun currentSettings(): AgentSettings = settingsFlow.first()

    /** 保存服务器地址（自动去除末尾斜杠） */
    suspend fun updateServerUrl(url: String) {
        dataStore.edit { it[SERVER_URL] = url.trimEnd('/') }
    }

    /** 登录成功后保存 token 和用户名 */
    suspend fun saveLoginResult(
        accessToken: String,
        refreshToken: String,
        username: String
    ) {
        dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = accessToken
            prefs[REFRESH_TOKEN] = refreshToken
            prefs[LOGGED_IN_USERNAME] = username
            prefs[AGENT_ENABLED] = true
        }
    }

    /** Token 刷新后更新 */
    suspend fun updateTokens(accessToken: String, refreshToken: String) {
        dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = accessToken
            prefs[REFRESH_TOKEN] = refreshToken
        }
    }

    /** 退出登录，清除 token 和用户名 */
    suspend fun clearLogin() {
        dataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN)
            prefs.remove(REFRESH_TOKEN)
            prefs.remove(LOGGED_IN_USERNAME)
            prefs[AGENT_ENABLED] = false
            prefs[SYNC_ENABLED] = false
        }
    }

    suspend fun updateSyncEnabled(enabled: Boolean) {
        dataStore.edit { it[SYNC_ENABLED] = enabled }
    }

    suspend fun updateLastSyncTimestamp(timestamp: Long) {
        dataStore.edit { it[LAST_SYNC_TIMESTAMP] = timestamp }
    }

    suspend fun updateAgentEnabled(enabled: Boolean) {
        dataStore.edit { it[AGENT_ENABLED] = enabled }
    }

    companion object {
        private val SERVER_URL = stringPreferencesKey("agent_server_url")
        private val ACCESS_TOKEN = stringPreferencesKey("agent_access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("agent_refresh_token")
        private val SYNC_ENABLED = booleanPreferencesKey("agent_sync_enabled")
        private val LAST_SYNC_TIMESTAMP = longPreferencesKey("agent_last_sync_ts")
        private val LOGGED_IN_USERNAME = stringPreferencesKey("agent_username")
        private val AGENT_ENABLED = booleanPreferencesKey("agent_enabled")
    }
}

data class AgentSettings(
    val serverUrl: String,
    val accessToken: String,
    val refreshToken: String,
    val syncEnabled: Boolean,
    val lastSyncTimestamp: Long,
    val loggedInUsername: String,
    val agentEnabled: Boolean
) {
    val isLoggedIn: Boolean get() = accessToken.isNotBlank()
    val isServerConfigured: Boolean get() = serverUrl.isNotBlank()
    val isReady: Boolean get() = isServerConfigured && isLoggedIn && agentEnabled
}

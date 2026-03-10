package com.heldairy.feature.settings

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.heldairy.HElDairyApplication
import com.heldairy.core.data.BackupManager
import com.heldairy.core.di.AppContainerImpl
import com.heldairy.core.network.agent.AgentClient
import com.heldairy.core.preferences.AgentPreferencesStore
import com.heldairy.core.preferences.AiPreferencesStore
import com.heldairy.core.preferences.DailyReportPreferencesStore
import com.heldairy.core.preferences.UserProfileStore
import com.heldairy.core.worker.DataSyncWorker
import com.heldairy.feature.report.reminder.DailyReportReminderScheduler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class SettingsViewModel(
    private val context: Context,
    private val preferencesStore: AiPreferencesStore,
    private val userProfileStore: UserProfileStore,
    private val dailyReportPreferencesStore: DailyReportPreferencesStore,
    private val backupManager: BackupManager,
    private val agentPreferencesStore: AgentPreferencesStore,
    private val appContainerImpl: AppContainerImpl?   // 需要调用 rebuildAgentClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            preferencesStore.settingsFlow.collectLatest { settings ->
                _uiState.update { current ->
                    val isDirty = current.apiKeyInput != current.lastSavedApiKey
                    current.copy(
                        lastSavedApiKey = settings.apiKey,
                        apiKeyInput = if (isDirty) current.apiKeyInput else settings.apiKey,
                        aiEnabled = settings.aiEnabled
                    )
                }
            }
        }
        viewModelScope.launch {
            userProfileStore.profileFlow.collectLatest { profile ->
                _uiState.update { it.copy(userName = profile.userName, avatarUri = profile.avatarUri) }
            }
        }
        viewModelScope.launch {
            dailyReportPreferencesStore.settingsFlow.collectLatest { settings ->
                _uiState.update { it.copy(dailyReminderEnabled = settings.reminderEnabled) }
            }
        }
        // ── Agent settings flow ──
        viewModelScope.launch {
            agentPreferencesStore.settingsFlow.collectLatest { agentSettings ->
                _uiState.update {
                    it.copy(
                        agentServerUrl = agentSettings.serverUrl,
                        agentLoggedInUsername = agentSettings.loggedInUsername,
                        agentIsLoggedIn = agentSettings.isLoggedIn,
                        agentSyncEnabled = agentSettings.syncEnabled,
                        agentEnabled = agentSettings.agentEnabled,
                        agentLastSyncTimestamp = agentSettings.lastSyncTimestamp
                    )
                }
            }
        }
    }

    fun onApiKeyChanged(value: String) {
        _uiState.update { it.copy(apiKeyInput = value) }
    }

    fun onAiEnabledChanged(enabled: Boolean) {
        _uiState.update { it.copy(aiEnabled = enabled) }
        viewModelScope.launch {
            preferencesStore.updateEnabled(enabled)
        }
    }

    fun saveApiKey() {
        val input = _uiState.value.apiKeyInput
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            preferencesStore.updateApiKey(input)
            _uiState.update { it.copy(isSaving = false) }
            _events.emit(SettingsEvent.Snackbar("API Key 已更新"))
        }
    }

    fun clearApiKey() {
        viewModelScope.launch {
            preferencesStore.updateApiKey("")
            _events.emit(SettingsEvent.Snackbar("API Key 已清除"))
        }
    }
    
    fun onUserNameChanged(name: String) {
        _uiState.update { it.copy(userName = name) }
    }
    
    fun saveUserName() {
        val name = _uiState.value.userName
        viewModelScope.launch {
            userProfileStore.updateUserName(name)
            _events.emit(SettingsEvent.Snackbar("用户名已更新"))
        }
    }
    
    fun updateAvatar(uri: String?) {
        viewModelScope.launch {
            userProfileStore.updateAvatar(uri)
            _events.emit(SettingsEvent.Snackbar("头像已更新"))
        }
    }
    
    fun onDailyReminderEnabledChanged(enabled: Boolean) {
        _uiState.update { it.copy(dailyReminderEnabled = enabled) }
        viewModelScope.launch {
            dailyReportPreferencesStore.updateReminderEnabled(enabled)
            if (enabled) {
                DailyReportReminderScheduler.scheduleReminder(context)
                _events.emit(SettingsEvent.Snackbar("✨ Kitty小管家每晚20:00会来提醒你哦~"))
            } else {
                DailyReportReminderScheduler.cancelReminder(context)
                _events.emit(SettingsEvent.Snackbar("日报提醒已关闭"))
            }
        }
    }

    suspend fun exportJson(): Result<String> {
        return runCatching { backupManager.exportJson() }
    }

    suspend fun importJson(raw: String): Result<Unit> {
        return backupManager.importJson(raw)
    }

    fun clearAllData() {
        viewModelScope.launch {
            backupManager.clearAllData()
            _events.emit(SettingsEvent.Snackbar("所有数据已清空"))
        }
    }

    fun showMessage(message: String) {
        viewModelScope.launch {
            _events.emit(SettingsEvent.Snackbar(message))
        }
    }

    // ═══════════════════════════════════════════════════════
    // Agent 相关操作
    // ═══════════════════════════════════════════════════════

    fun onAgentServerUrlChanged(url: String) {
        _uiState.update { it.copy(agentServerUrl = url) }
    }

    fun saveAgentServerUrl() {
        val url = normalizeServerUrl(_uiState.value.agentServerUrl)
        viewModelScope.launch {
            if (url == null) {
                _events.emit(SettingsEvent.Snackbar("服务器地址格式错误，请使用 http://IP:端口"))
                return@launch
            }
            val host = Uri.parse(url).host.orEmpty()
            if (!isProbablyEmulator() && host == "10.0.2.2") {
                _events.emit(SettingsEvent.Snackbar("10.0.2.2 仅模拟器可用；真机请填写电脑局域网 IP"))
            }
            agentPreferencesStore.updateServerUrl(url)
            appContainerImpl?.rebuildAgentClient(url)
            _events.emit(SettingsEvent.Snackbar("Agent 服务器地址已保存"))
        }
    }

    fun onAgentUsernameChanged(value: String) {
        _uiState.update { it.copy(agentUsernameInput = value) }
    }

    fun onAgentEmailChanged(value: String) {
        _uiState.update { it.copy(agentEmailInput = value) }
    }

    fun onAgentPasswordChanged(value: String) {
        _uiState.update { it.copy(agentPasswordInput = value) }
    }

    fun agentLogin() {
        val username = _uiState.value.agentUsernameInput
        val password = _uiState.value.agentPasswordInput
        if (username.isBlank() || password.isBlank()) {
            viewModelScope.launch { _events.emit(SettingsEvent.Snackbar("请输入用户名和密码")) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(agentIsLoading = true) }
            val normalizedUrl = normalizeServerUrl(_uiState.value.agentServerUrl)
            if (normalizedUrl == null) {
                _events.emit(SettingsEvent.Snackbar("服务器地址格式错误，请使用 http://IP:端口"))
                _uiState.update { it.copy(agentIsLoading = false) }
                return@launch
            }
            agentPreferencesStore.updateServerUrl(normalizedUrl)
            appContainerImpl?.rebuildAgentClient(normalizedUrl)
            val client = appContainerImpl?.agentClient
            if (client == null) {
                _events.emit(SettingsEvent.Snackbar("请先配置 Agent 服务器地址"))
                _uiState.update { it.copy(agentIsLoading = false) }
                return@launch
            }
            client.login(username, password)
                .onSuccess { tokens ->
                    agentPreferencesStore.saveLoginResult(tokens.accessToken, tokens.refreshToken, username)
                    _uiState.update { it.copy(agentIsLoading = false, agentPasswordInput = "") }
                    _events.emit(SettingsEvent.Snackbar("登录成功 🎉"))
                }
                .onFailure { e ->
                    _uiState.update { it.copy(agentIsLoading = false) }
                    _events.emit(SettingsEvent.Snackbar("登录失败: ${e.message}"))
                }
        }
    }

    fun agentRegister() {
        val username = _uiState.value.agentUsernameInput
        val password = _uiState.value.agentPasswordInput
        val email = _uiState.value.agentEmailInput.trim()
        if (username.isBlank() || password.isBlank() || email.isBlank()) {
            viewModelScope.launch { _events.emit(SettingsEvent.Snackbar("请输入用户名、邮箱和密码")) }
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            viewModelScope.launch { _events.emit(SettingsEvent.Snackbar("请输入有效邮箱地址")) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(agentIsLoading = true) }
            val normalizedUrl = normalizeServerUrl(_uiState.value.agentServerUrl)
            if (normalizedUrl == null) {
                _events.emit(SettingsEvent.Snackbar("服务器地址格式错误，请使用 http://IP:端口"))
                _uiState.update { it.copy(agentIsLoading = false) }
                return@launch
            }
            val host = Uri.parse(normalizedUrl).host.orEmpty()
            if (!isProbablyEmulator() && host == "10.0.2.2") {
                _events.emit(SettingsEvent.Snackbar("当前像是真机：请把地址改成电脑局域网 IP（如 http://172.20.x.x:8011）"))
                _uiState.update { it.copy(agentIsLoading = false) }
                return@launch
            }
            agentPreferencesStore.updateServerUrl(normalizedUrl)
            appContainerImpl?.rebuildAgentClient(normalizedUrl)
            val client = appContainerImpl?.agentClient
            if (client == null) {
                _events.emit(SettingsEvent.Snackbar("请先配置 Agent 服务器地址"))
                _uiState.update { it.copy(agentIsLoading = false) }
                return@launch
            }
            client.register(username, password, email = email, displayName = _uiState.value.userName)
                .onSuccess { tokens ->
                    agentPreferencesStore.saveLoginResult(tokens.accessToken, tokens.refreshToken, username)
                    _uiState.update { it.copy(agentIsLoading = false, agentPasswordInput = "") }
                    _events.emit(SettingsEvent.Snackbar("注册成功，已自动登录 🎉"))
                }
                .onFailure { e ->
                    _uiState.update { it.copy(agentIsLoading = false) }
                    _events.emit(SettingsEvent.Snackbar("注册失败: ${e.message}"))
                }
        }
    }

    fun agentLogout() {
        viewModelScope.launch {
            agentPreferencesStore.clearLogin()
            // 取消同步 Worker
            WorkManager.getInstance(context).cancelUniqueWork(DataSyncWorker.WORK_NAME)
            _events.emit(SettingsEvent.Snackbar("已退出 Agent"))
        }
    }

    fun onAgentSyncEnabledChanged(enabled: Boolean) {
        viewModelScope.launch {
            agentPreferencesStore.updateSyncEnabled(enabled)
            if (enabled) {
                scheduleSyncWorker()
                _events.emit(SettingsEvent.Snackbar("数据同步已开启"))
            } else {
                WorkManager.getInstance(context).cancelUniqueWork(DataSyncWorker.WORK_NAME)
                _events.emit(SettingsEvent.Snackbar("数据同步已关闭"))
            }
        }
    }

    fun onAgentEnabledChanged(enabled: Boolean) {
        viewModelScope.launch {
            agentPreferencesStore.updateAgentEnabled(enabled)
        }
    }

    fun triggerSyncNow() {
        val request = OneTimeWorkRequestBuilder<DataSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueue(request)
        viewModelScope.launch {
            _events.emit(SettingsEvent.Snackbar("正在同步..."))
        }
    }

    fun forceFullSyncNow() {
        viewModelScope.launch {
            agentPreferencesStore.updateLastSyncTimestamp(0L)
        }
        val request = OneTimeWorkRequestBuilder<DataSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueue(request)
        viewModelScope.launch {
            _events.emit(SettingsEvent.Snackbar("正在全量同步..."))
        }
    }

    private fun scheduleSyncWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<DataSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DataSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HElDairyApplication)
                val container = app.appContainer
                SettingsViewModel(
                    context = app.applicationContext,
                    preferencesStore = container.aiPreferencesStore,
                    userProfileStore = container.userProfileStore,
                    dailyReportPreferencesStore = container.dailyReportPreferencesStore,
                    backupManager = container.backupManager,
                    agentPreferencesStore = container.agentPreferencesStore,
                    appContainerImpl = container as? AppContainerImpl
                )
            }
        }
    }

    private fun normalizeServerUrl(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "http://$trimmed"
        }
        val uri = runCatching { Uri.parse(withScheme) }.getOrNull() ?: return null
        val host = uri.host ?: return null
        val scheme = uri.scheme ?: return null
        if (scheme != "http" && scheme != "https") return null
        if (host.isBlank()) return null
        return withScheme.trimEnd('/')
    }

    private fun isProbablyEmulator(): Boolean {
        return Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.lowercase().contains("emulator") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for x86") ||
            Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic") ||
            Build.PRODUCT.contains("sdk")
    }
}

data class SettingsUiState(
    val apiKeyInput: String = "",
    val lastSavedApiKey: String = "",
    val aiEnabled: Boolean = true,
    val dailyReminderEnabled: Boolean = true,
    val userName: String = "Kitty宝贝",
    val avatarUri: String? = null,
    val isSaving: Boolean = false,
    // ── Agent ──
    val agentServerUrl: String = "",
    val agentUsernameInput: String = "",
    val agentEmailInput: String = "",
    val agentPasswordInput: String = "",
    val agentLoggedInUsername: String = "",
    val agentIsLoggedIn: Boolean = false,
    val agentSyncEnabled: Boolean = false,
    val agentEnabled: Boolean = false,
    val agentIsLoading: Boolean = false,
    val agentLastSyncTimestamp: Long = 0L
) {
    val isApiKeyDirty: Boolean get() = apiKeyInput != lastSavedApiKey
}

sealed interface SettingsEvent {
    data class Snackbar(val message: String) : SettingsEvent
}

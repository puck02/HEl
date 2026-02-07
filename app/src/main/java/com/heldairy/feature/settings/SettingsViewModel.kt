package com.heldairy.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.heldairy.HElDairyApplication
import com.heldairy.core.data.BackupManager
import com.heldairy.core.preferences.AiPreferencesStore
import com.heldairy.core.preferences.DailyReportPreferencesStore
import com.heldairy.core.preferences.UserProfileStore
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

class SettingsViewModel(
    private val context: Context,
    private val preferencesStore: AiPreferencesStore,
    private val userProfileStore: UserProfileStore,
    private val dailyReportPreferencesStore: DailyReportPreferencesStore,
    private val backupManager: BackupManager
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

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HElDairyApplication)
                SettingsViewModel(
                    context = app.applicationContext,
                    preferencesStore = app.appContainer.aiPreferencesStore,
                    userProfileStore = app.appContainer.userProfileStore,
                    dailyReportPreferencesStore = app.appContainer.dailyReportPreferencesStore,
                    backupManager = app.appContainer.backupManager
                )
            }
        }
    }
}

data class SettingsUiState(
    val apiKeyInput: String = "",
    val lastSavedApiKey: String = "",
    val aiEnabled: Boolean = true,
    val dailyReminderEnabled: Boolean = true,
    val userName: String = "Kitty宝贝",
    val avatarUri: String? = null,
    val isSaving: Boolean = false
) {
    val isApiKeyDirty: Boolean get() = apiKeyInput != lastSavedApiKey
}

sealed interface SettingsEvent {
    data class Snackbar(val message: String) : SettingsEvent
}

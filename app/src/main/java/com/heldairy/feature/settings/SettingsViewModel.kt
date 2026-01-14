package com.heldairy.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.heldairy.HElDairyApplication
import com.heldairy.core.data.BackupManager
import com.heldairy.core.preferences.AiPreferencesStore
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
    private val preferencesStore: AiPreferencesStore,
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
                        aiEnabled = settings.aiEnabled,
                        isDarkTheme = settings.themeDark
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

    fun onThemeChanged(enabled: Boolean) {
        _uiState.update { it.copy(isDarkTheme = enabled) }
        viewModelScope.launch {
            preferencesStore.updateThemeDark(enabled)
        }
    }

    fun clearApiKey() {
        viewModelScope.launch {
            preferencesStore.updateApiKey("")
            _events.emit(SettingsEvent.Snackbar("API Key 已清除"))
        }
    }

    suspend fun exportJson(): Result<String> {
        return runCatching { backupManager.exportJson() }
    }

    suspend fun exportCsv(): Result<String> {
        return runCatching { backupManager.exportCsv() }
    }

    suspend fun importJson(raw: String): Result<Unit> {
        return backupManager.importJson(raw)
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
                    preferencesStore = app.container.aiPreferencesStore,
                    backupManager = app.container.backupManager
                )
            }
        }
    }
}

data class SettingsUiState(
    val apiKeyInput: String = "",
    val lastSavedApiKey: String = "",
    val aiEnabled: Boolean = true,
    val isDarkTheme: Boolean = false,
    val isSaving: Boolean = false
) {
    val isApiKeyDirty: Boolean get() = apiKeyInput != lastSavedApiKey
}

sealed interface SettingsEvent {
    data class Snackbar(val message: String) : SettingsEvent
}

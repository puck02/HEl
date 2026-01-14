package com.heldairy.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.heldairy.HElDairyApplication
import com.heldairy.core.preferences.AiPreferencesStore
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ThemeViewModel(private val preferencesStore: AiPreferencesStore) : ViewModel() {

    val isDarkTheme: StateFlow<Boolean> = preferencesStore.settingsFlow
        .map { it.themeDark }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    fun toggleTheme() {
        val next = !isDarkTheme.value
        viewModelScope.launch {
            preferencesStore.updateThemeDark(next)
        }
    }

    fun setTheme(dark: Boolean) {
        viewModelScope.launch {
            preferencesStore.updateThemeDark(dark)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HElDairyApplication)
                ThemeViewModel(app.container.aiPreferencesStore)
            }
        }
    }
}

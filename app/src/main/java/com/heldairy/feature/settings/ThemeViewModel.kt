package com.heldairy.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.heldairy.HElDairyApplication
import com.heldairy.core.preferences.AiPreferencesStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ThemeViewModel(
	private val preferencesStore: AiPreferencesStore
) : ViewModel() {

	private val _isDarkTheme = MutableStateFlow(false)
	val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

	init {
		viewModelScope.launch {
			preferencesStore.settingsFlow.collectLatest { settings ->
				_isDarkTheme.value = settings.themeDark
			}
		}
	}

	fun toggleTheme() {
		viewModelScope.launch {
			val next = !_isDarkTheme.value
			_isDarkTheme.value = next
			preferencesStore.updateThemeDark(next)
		}
	}

	companion object {
		val Factory = viewModelFactory {
			initializer {
				val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HElDairyApplication)
				ThemeViewModel(preferencesStore = app.appContainer.aiPreferencesStore)
			}
		}
	}
}

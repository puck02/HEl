package com.heldairy.feature.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heldairy.feature.settings.SettingsEvent
import com.heldairy.feature.settings.SettingsUiState
import com.heldairy.feature.settings.SettingsViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SettingsRoute(
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SettingsEvent.Snackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        modifier = modifier.padding(paddingValues),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        SettingsScreen(
            state = uiState,
            onApiKeyChanged = viewModel::onApiKeyChanged,
            onSaveApiKey = viewModel::saveApiKey,
            onClearApiKey = viewModel::clearApiKey,
            onAiEnabledChanged = viewModel::onAiEnabledChanged,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onApiKeyChanged: (String) -> Unit,
    onSaveApiKey: () -> Unit,
    onClearApiKey: () -> Unit,
    onAiEnabledChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "DeepSeek 设置")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "启用 AI 管家")
            Switch(
                checked = state.aiEnabled,
                onCheckedChange = onAiEnabledChanged
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "API Key")
            OutlinedTextField(
                value = state.apiKeyInput,
                onValueChange = onApiKeyChanged,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                placeholder = { Text("sk-...") }
            )
            RowButtons(
                isDirty = state.isApiKeyDirty,
                isSaving = state.isSaving,
                onSave = onSaveApiKey,
                onClear = onClearApiKey
            )
        }
        Text(
            text = "API Key 仅保存在本地，可随时清除。关闭 AI 后仍可保留历史记录。"
        )
    }
}

@Composable
private fun RowButtons(
    isDirty: Boolean,
    isSaving: Boolean,
    onSave: () -> Unit,
    onClear: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onSave,
            enabled = isDirty && !isSaving
        ) {
            Text(text = if (isSaving) "保存中…" else "保存 API Key")
        }
        TextButton(onClick = onClear) {
            Text("清除 API Key")
        }
    }
}

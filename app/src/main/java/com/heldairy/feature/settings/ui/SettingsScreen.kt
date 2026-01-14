package com.heldairy.feature.settings.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heldairy.feature.settings.SettingsEvent
import com.heldairy.feature.settings.SettingsUiState
import com.heldairy.feature.settings.SettingsViewModel
import java.io.OutputStreamWriter
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.text.Charsets

@Composable
fun SettingsRoute(
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                viewModel.exportJson()
                    .onSuccess { content ->
                        val writeResult = writeTextToUri(context, uri, content)
                        if (writeResult.isSuccess) {
                            viewModel.showMessage("JSON 导出完成")
                        } else {
                            viewModel.showMessage(writeResult.exceptionOrNull()?.message ?: "导出失败")
                        }
                    }
                    .onFailure { viewModel.showMessage(it.message ?: "导出失败") }
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                viewModel.exportCsv()
                    .onSuccess { content ->
                        val writeResult = writeTextToUri(context, uri, content)
                        if (writeResult.isSuccess) {
                            viewModel.showMessage("CSV 导出完成")
                        } else {
                            viewModel.showMessage(writeResult.exceptionOrNull()?.message ?: "导出失败")
                        }
                    }
                    .onFailure { viewModel.showMessage(it.message ?: "导出失败") }
            }
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val readResult = readTextFromUri(context, uri)
                if (readResult.isSuccess) {
                    val importResult = viewModel.importJson(readResult.getOrThrow())
                    if (importResult.isSuccess) {
                        viewModel.showMessage("导入完成，已覆盖现有数据")
                    } else {
                        viewModel.showMessage(importResult.exceptionOrNull()?.message ?: "导入失败")
                    }
                } else {
                    viewModel.showMessage(readResult.exceptionOrNull()?.message ?: "读取文件失败")
                }
            }
        }
    }

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
            onThemeChanged = viewModel::onThemeChanged,
            onExportJson = { exportJsonLauncher.launch(defaultBackupFileName("json")) },
            onExportCsv = { exportCsvLauncher.launch(defaultBackupFileName("csv")) },
            onImportJson = { importJsonLauncher.launch("application/json") },
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
    onThemeChanged: (Boolean) -> Unit,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onImportJson: () -> Unit,
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
            Text(text = "夜间模式")
            Switch(
                checked = state.isDarkTheme,
                onCheckedChange = onThemeChanged
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
        HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "数据备份")
            Text(text = "导出包含所有日报、回答、建议与总结，导入会覆盖当前数据。")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onExportJson, modifier = Modifier.weight(1f)) {
                    Text(text = "导出 JSON")
                }
                Button(onClick = onExportCsv, modifier = Modifier.weight(1f)) {
                    Text(text = "导出 CSV")
                }
            }
            Button(onClick = onImportJson, modifier = Modifier.fillMaxWidth()) {
                Text(text = "导入 JSON（覆盖）")
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "请选择保存/导入路径，建议放入云盘或本地 Documents 目录。")
        }
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

private fun defaultBackupFileName(extension: String): String {
    val date = LocalDate.now().toString()
    val suffix = if (extension == "csv") "export" else "backup"
    return "heldairy-$suffix-$date.$extension"
}

private suspend fun writeTextToUri(
    context: Context,
    uri: Uri,
    content: String
): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            OutputStreamWriter(output, Charsets.UTF_8).use { writer ->
                writer.write(content)
            }
        } ?: error("无法写入文件")
    }
}

private suspend fun readTextFromUri(context: Context, uri: Uri): Result<String> =
    withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                reader.readText()
            } ?: error("无法读取文件")
        }
    }

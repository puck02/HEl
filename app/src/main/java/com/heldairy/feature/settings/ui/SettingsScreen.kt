package com.heldairy.feature.settings.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.heldairy.ui.theme.Spacing
import com.heldairy.ui.theme.CornerRadius
import com.heldairy.ui.theme.Elevation
import com.heldairy.ui.theme.KittyBackground
import com.heldairy.ui.theme.BackgroundTheme
import com.heldairy.ui.theme.StickerDecoration
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.res.painterResource
import com.heldairy.R
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
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
                            viewModel.showMessage(context.getString(R.string.settings_export_json) + " 完成")
                        } else {
                            viewModel.showMessage(writeResult.exceptionOrNull()?.message ?: context.getString(R.string.error_unknown))
                        }
                    }
                    .onFailure { viewModel.showMessage(it.message ?: context.getString(R.string.error_unknown)) }
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
                        viewModel.showMessage(context.getString(R.string.settings_import_json) + " 完成")
                    } else {
                        viewModel.showMessage(importResult.exceptionOrNull()?.message ?: context.getString(R.string.error_unknown))
                    }
                } else {
                    viewModel.showMessage(readResult.exceptionOrNull()?.message ?: context.getString(R.string.error_unknown))
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
        KittyBackground(backgroundRes = BackgroundTheme.SETTINGS) {
            SettingsScreen(
                state = uiState,
                onApiKeyChanged = viewModel::onApiKeyChanged,
                onSaveApiKey = viewModel::saveApiKey,
                onClearApiKey = viewModel::clearApiKey,
                onAiEnabledChanged = viewModel::onAiEnabledChanged,
                onDailyReminderEnabledChanged = viewModel::onDailyReminderEnabledChanged,
                onUserNameChanged = viewModel::onUserNameChanged,
                onSaveUserName = viewModel::saveUserName,
                onAvatarSelected = { uri -> viewModel.updateAvatar(uri?.toString()) },
                onExportJson = { exportJsonLauncher.launch(defaultBackupFileName("json")) },
                onImportJson = { importJsonLauncher.launch("application/json") },
                onClearAllData = { viewModel.clearAllData() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onApiKeyChanged: (String) -> Unit,
    onSaveApiKey: () -> Unit,
    onClearApiKey: () -> Unit,
    onAiEnabledChanged: (Boolean) -> Unit,
    onDailyReminderEnabledChanged: (Boolean) -> Unit,
    onUserNameChanged: (String) -> Unit,
    onSaveUserName: () -> Unit,
    onAvatarSelected: (Uri?) -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onClearAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                start = Spacing.M,
                end = Spacing.M,
                top = Spacing.L,
                bottom = Spacing.M
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.M)
    ) {
        // 用户信息卡片
        ProfileCard(
            userName = state.userName,
            avatarUri = state.avatarUri,
            onUserNameChanged = onUserNameChanged,
            onSaveUserName = onSaveUserName,
            onAvatarSelected = onAvatarSelected
        )
        
        AiToggleSection(
            aiEnabled = state.aiEnabled,
            onAiEnabledChanged = onAiEnabledChanged
        )
        
        // 日报提醒开关
        DailyReminderSection(
            enabled = state.dailyReminderEnabled,
            onChanged = onDailyReminderEnabledChanged
        )

        ApiKeySection(
            apiKeyInput = state.apiKeyInput,
            isApiKeyDirty = state.isApiKeyDirty,
            isSaving = state.isSaving,
            onApiKeyChanged = onApiKeyChanged,
            onSave = onSaveApiKey,
            onClear = onClearApiKey
        )

        BackupSection(
            onExportJson = onExportJson,
            onImportJson = onImportJson
        )

        DataManagementSection(onClearAllData = onClearAllData)
    }
}

@Composable
private fun AiToggleSection(
    aiEnabled: Boolean,
    onAiEnabledChanged: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(CornerRadius.Medium),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.None),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.M),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.S)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.settings_ai_enable), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.settings_ai_enable_description),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = aiEnabled, onCheckedChange = onAiEnabledChanged)
        }
    }
}

@Composable
private fun DailyReminderSection(
    enabled: Boolean,
    onChanged: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(CornerRadius.Medium),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.None),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.M),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.S)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.settings_daily_reminder), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.settings_daily_reminder_description),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = enabled, onCheckedChange = onChanged)
        }
    }
}

@Composable
private fun ApiKeySection(
    apiKeyInput: String,
    isApiKeyDirty: Boolean,
    isSaving: Boolean,
    onApiKeyChanged: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit
) {
    var showApiKey by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.S)) {
        Text(text = stringResource(R.string.settings_api_key_label), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(CornerRadius.Medium),
            elevation = CardDefaults.cardElevation(defaultElevation = Elevation.None),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = onApiKeyChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.S, vertical = Spacing.XS),
                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                placeholder = { Text(stringResource(R.string.settings_api_key_placeholder)) },
                trailingIcon = {
                    IconButton(onClick = { showApiKey = !showApiKey }) {
                        Icon(
                            imageVector = if (showApiKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (showApiKey) stringResource(R.string.cd_hide_password) else stringResource(R.string.cd_show_password),
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(CornerRadius.Medium),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    focusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    cursorColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                )
            )
            ActionRow(
                isDirty = isApiKeyDirty,
                isSaving = isSaving,
                onSave = onSave,
                onClear = onClear
            )
        }
        InfoCard(text = stringResource(R.string.settings_api_key_info))
    }
}

@Composable
private fun BackupSection(
    onExportJson: () -> Unit,
    onImportJson: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.S)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.XS)
        ) {
            Text(text = stringResource(R.string.settings_backup_section), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            StickerDecoration(
                drawableRes = R.drawable.strawberry,
                size = 32.dp,
                rotation = 12f,
                alpha = 0.55f,
                modifier = Modifier.offset(x = 4.dp, y = (-2).dp)
            )
        }
        Text(
            text = stringResource(R.string.settings_backup_description),
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        )
        StandardButton(
            text = stringResource(R.string.settings_export_json),
            icon = Icons.Outlined.CloudUpload,
            onClick = onExportJson,
            modifier = Modifier.fillMaxWidth()
        )
        StandardButton(
            text = stringResource(R.string.settings_import_json),
            icon = Icons.Outlined.CloudDownload,
            onClick = onImportJson,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DataManagementSection(onClearAllData: () -> Unit) {
    var showClearDataDialog by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.S)) {
        Text(text = stringResource(R.string.settings_data_section), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.settings_clear_data_warning),
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedActionButton(
            text = stringResource(R.string.settings_clear_all_data),
            icon = Icons.Outlined.DeleteSweep,
            onClick = { showClearDataDialog = true },
            modifier = Modifier.fillMaxWidth()
        )
    }
    if (showClearDataDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.DeleteSweep,
                    contentDescription = null,
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.settings_clear_data_confirm_title)) },
            text = { Text(stringResource(R.string.settings_clear_data_confirm_message)) },
            confirmButton = {
                Button(
                    onClick = { showClearDataDialog = false; onClearAllData() },
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun ActionRow(
    isDirty: Boolean,
    isSaving: Boolean,
    onSave: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.S, vertical = Spacing.XS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(onClick = onSave, enabled = isDirty && !isSaving) {
            Text(text = if (isSaving) stringResource(R.string.saving) else stringResource(R.string.settings_save_api_key))
        }
        TextButton(onClick = onClear) {
            Text(stringResource(R.string.settings_clear_api_key), color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun InfoCard(text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(CornerRadius.Medium),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.None),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(Spacing.M),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.XS)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color = androidx.compose.material3.MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Outlined.Info, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.secondary)
            }
            Text(
                text = text,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StandardButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(CornerRadius.Medium),
        colors = ButtonDefaults.buttonColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = Elevation.Low)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.XS)
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Text(text = text, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun OutlinedActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(CornerRadius.Medium),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
        ),
        border = ButtonDefaults.outlinedButtonBorder
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.XS)
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Text(text = text, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ProfileCard(
    userName: String,
    avatarUri: String?,
    onUserNameChanged: (String) -> Unit,
    onSaveUserName: () -> Unit,
    onAvatarSelected: (Uri?) -> Unit
) {
    var isEditingName by remember { mutableStateOf(false) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        onAvatarSelected(uri)
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.S)) {
        Text(text = stringResource(R.string.settings_user_profile), style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(CornerRadius.Medium),
            elevation = CardDefaults.cardElevation(defaultElevation = Elevation.None),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(Spacing.M),
                verticalArrangement = Arrangement.spacedBy(Spacing.M)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.M),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        androidx.compose.material3.MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                                    )
                                ),
                                shape = CircleShape
                            )
                            .clickable {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarUri != null) {
                            AsyncImage(
                                model = avatarUri,
                                contentDescription = stringResource(R.string.settings_user_profile),
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // 使用 Hello Kitty 作为默认头像
                            Image(
                                painter = painterResource(id = R.drawable.default_avatar_kitty),
                                contentDescription = stringResource(R.string.settings_user_profile),
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        if (isEditingName) {
                            OutlinedTextField(
                                value = userName,
                                onValueChange = onUserNameChanged,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = androidx.compose.material3.MaterialTheme.typography.titleMedium
                            )
                        } else {
                            Text(
                                text = userName,
                                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(onClick = { isEditingName = !isEditingName }) {
                            Text(if (isEditingName) stringResource(R.string.action_cancel) else stringResource(R.string.action_edit) + stringResource(R.string.settings_user_name_label))
                        }
                    }
                }
                
                if (isEditingName) {
                    Button(
                        onClick = {
                            onSaveUserName()
                            isEditingName = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                }
            }
        }
        StickerDecoration(
            drawableRes = R.drawable.cake02,
            size = 46.dp,
            rotation = -15f,
            alpha = 0.5f,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 16.dp, y = (-16).dp)
        )
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
        } ?: error("Write error")
    }
}

private suspend fun readTextFromUri(context: Context, uri: Uri): Result<String> =
    withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
                reader.readText()
            } ?: error("Read error")
        }
    }


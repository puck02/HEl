package com.heldairy.feature.medication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heldairy.R
import com.heldairy.feature.medication.AddEvent
import com.heldairy.feature.medication.AddMedicationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationRoute(
    viewModel: AddMedicationViewModel,
    paddingValues: PaddingValues = PaddingValues(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showConfirmDialog by remember { mutableStateOf<AddEvent.ShowConfirmDialog?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AddEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                AddEvent.NavigateBack -> onBack()
                is AddEvent.ShowConfirmDialog -> {
                    showConfirmDialog = event
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.medication_add)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // NLP输入卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "AI 智能识别",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedTextField(
                        value = uiState.naturalInput,
                        onValueChange = viewModel::onNaturalInput,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.medication_description_label)) },
                        placeholder = { Text(stringResource(R.string.medication_description_placeholder)) },
                        minLines = 3,
                        enabled = !uiState.isParsing
                    )

                    if (uiState.parseError != null) {
                        Text(
                            text = "⚠️ ${uiState.parseError}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (uiState.isParsing) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .height(24.dp)
                                    .width(24.dp)
                            )
                        } else {
                            OutlinedButton(
                                onClick = viewModel::parseNaturalInput,
                                enabled = uiState.naturalInput.isNotBlank()
                            ) {
                                Text(stringResource(R.string.medication_parse))
                            }
                        }
                    }
                }
            }

            Text(
                "或手动填写详细信息",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 手动输入表单
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.medication_name_required)) },
                singleLine = true
            )

            OutlinedTextField(
                value = uiState.aliases,
                onValueChange = viewModel::onAliasesChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.medication_aliases_label)) },
                placeholder = { Text(stringResource(R.string.medication_aliases_placeholder)) }
            )

            OutlinedTextField(
                value = uiState.frequency,
                onValueChange = viewModel::onFrequencyChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.medication_frequency_label)) },
                placeholder = { Text(stringResource(R.string.medication_frequency_placeholder)) }
            )

            OutlinedTextField(
                value = uiState.dose,
                onValueChange = viewModel::onDoseChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.medication_dose_label)) },
                placeholder = { Text(stringResource(R.string.medication_dose_placeholder)) }
            )

            OutlinedTextField(
                value = uiState.timeHints,
                onValueChange = viewModel::onTimeHintsChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.medication_time_hints_label)) },
                placeholder = { Text(stringResource(R.string.medication_time_hints_placeholder)) }
            )

            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.medication_note_label)) },
                minLines = 2
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = viewModel::saveMedication,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving && uiState.name.isNotBlank()
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp).width(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
    
    // 确认对话框
    showConfirmDialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = { showConfirmDialog = null },
            title = { Text(stringResource(R.string.medication_active_conflict_title)) },
            text = { 
                Text("「${dialog.existingCourseName}」当前正在进行疗程。是否结束当前疗程并开启新疗程？")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmEndCurrentAndStartNew(
                            existingMedId = dialog.existingMedId,
                            currentCourseId = dialog.currentCourseId
                        )
                        showConfirmDialog = null
                    }
                ) {
                    Text(stringResource(R.string.medication_end_and_start_new))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

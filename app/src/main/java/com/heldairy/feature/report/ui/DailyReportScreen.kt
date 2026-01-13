package com.heldairy.feature.report.ui

import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import com.heldairy.feature.report.DailyReportEvent
import com.heldairy.feature.report.DailyReportUiState
import com.heldairy.feature.report.DailyReportViewModel
import com.heldairy.feature.report.QuestionUiState
import com.heldairy.feature.report.StepProgress
import com.heldairy.feature.report.model.QuestionKind

@Composable
fun DailyReportRoute(
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: DailyReportViewModel = viewModel(factory = DailyReportViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is DailyReportEvent.Snackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        DailyReportScreen(
            state = state,
            onOptionSelected = viewModel::onOptionSelected,
            onSliderValueChange = viewModel::onSliderValueChanged,
            onSliderValueChangeFinished = viewModel::onSliderValueChangeFinished,
            onTextChanged = viewModel::onTextChanged,
            onSubmit = viewModel::submitDailyReport,
            modifier = Modifier.fillMaxSize()
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}

@Composable
fun DailyReportScreen(
    state: DailyReportUiState,
    onOptionSelected: (String, String) -> Unit,
    onSliderValueChange: (String, Float) -> Unit,
    onSliderValueChangeFinished: (String) -> Unit,
    onTextChanged: (String, String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StepProgressSection(state.stepProgress)
            }
            items(
                items = state.questions.filter { it.isVisible },
                key = { it.question.id }
            ) { questionState ->
                QuestionCard(
                    questionState = questionState,
                    onOptionSelected = onOptionSelected,
                    onSliderValueChange = onSliderValueChange,
                    onSliderValueFinished = onSliderValueChangeFinished,
                    onTextChanged = onTextChanged
                )
            }
            item {
                Text(
                    text = "完成后我会在下一步继续陪伴你。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
        SubmitBar(
            canSubmit = state.canSubmit,
            isSaving = state.isSaving,
            onSubmit = onSubmit,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepProgressSection(progress: List<StepProgress>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "今日流程",
            style = MaterialTheme.typography.titleMedium
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            progress.forEach { step ->
                AssistChip(
                    onClick = {},
                    enabled = false,
                    leadingIcon = {
                        if (step.isComplete) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    },
                    label = {
                        Column {
                            Text(step.step.title)
                            Text(
                                text = step.step.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = if (step.isComplete) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                )
            }
        }
    }
}

@Composable
private fun QuestionCard(
    questionState: QuestionUiState,
    onOptionSelected: (String, String) -> Unit,
    onSliderValueChange: (String, Float) -> Unit,
    onSliderValueFinished: (String) -> Unit,
    onTextChanged: (String, String) -> Unit
) {
    ElevatedCard {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = questionState.question.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = questionState.question.prompt,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            when (questionState) {
                is QuestionUiState.SingleChoice -> ChoiceSection(questionState, onOptionSelected)
                is QuestionUiState.MultipleChoice -> MultiChoiceSection(questionState, onOptionSelected)
                is QuestionUiState.Slider -> SliderSection(questionState, onSliderValueChange, onSliderValueFinished)
                is QuestionUiState.TextInput -> TextInputSection(questionState, onTextChanged)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceSection(
    state: QuestionUiState.SingleChoice,
    onOptionSelected: (String, String) -> Unit
) {
    val options = (state.question.kind as QuestionKind.SingleChoice).options
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option.id == state.selectedOptionId,
                onClick = { onOptionSelected(state.question.id, option.id) },
                label = { Text(option.label) },
                border = FilterChipDefaults.filterChipBorder(enabled = true, selected = option.id == state.selectedOptionId)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MultiChoiceSection(
    state: QuestionUiState.MultipleChoice,
    onOptionSelected: (String, String) -> Unit
) {
    val options = (state.question.kind as QuestionKind.MultipleChoice).options
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            options.forEach { option ->
                val selected = state.selectedOptionIds.contains(option.id)
                FilterChip(
                    selected = selected,
                    onClick = { onOptionSelected(state.question.id, option.id) },
                    label = { Text(option.label) },
                    border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "已选 ${state.selectedOptionIds.size}/${state.maxSelection}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            state.helperText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SliderSection(
    state: QuestionUiState.Slider,
    onSliderValueChange: (String, Float) -> Unit,
    onSliderValueFinished: (String) -> Unit
) {
    val sliderKind = state.question.kind as QuestionKind.Slider
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "当前记录：", style = MaterialTheme.typography.labelLarge)
            Text(
                text = "${state.currentValue}${sliderKind.valueSuffix.orEmpty()}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = state.currentValue.toFloat(),
            valueRange = sliderKind.valueRange.first.toFloat()..sliderKind.valueRange.last.toFloat(),
            onValueChange = { newValue -> onSliderValueChange(state.question.id, newValue) },
            onValueChangeFinished = { onSliderValueFinished(state.question.id) },
            steps = sliderKind.valueRange.last - sliderKind.valueRange.first - 1
        )
        sliderKind.supportingText?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TextInputSection(
    state: QuestionUiState.TextInput,
    onTextChanged: (String, String) -> Unit
) {
    val textKind = state.question.kind as QuestionKind.TextInput
    OutlinedTextField(
        value = state.text,
        onValueChange = { updated -> onTextChanged(state.question.id, updated) },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(textKind.hint) },
        supportingText = textKind.supportingText?.let { helper ->
            {
                Text(
                    text = helper,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        singleLine = false,
        maxLines = 4
    )
}

@Composable
private fun SubmitBar(
    canSubmit: Boolean,
    isSaving: Boolean,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = canSubmit && !isSaving
        ) {
            Crossfade(targetState = isSaving, label = "buttonState") { saving ->
                if (saving) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text("正在保存…")
                    }
                } else {
                    Text("完成今日基础问题")
                }
            }
        }
        Text(
            text = "需要调整？可重新填写，我会以最新记录为准。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

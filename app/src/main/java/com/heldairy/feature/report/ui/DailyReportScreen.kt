package com.heldairy.feature.report.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heldairy.core.data.AdvicePayload
import com.heldairy.core.database.entity.AdviceTrackingEntity
import com.heldairy.core.database.entity.UserFeedback
import com.heldairy.core.data.AdviceSource
import com.heldairy.feature.report.AdviceUiState
import com.heldairy.feature.report.DailyReportEvent
import com.heldairy.feature.report.DailyReportUiState
import com.heldairy.feature.report.DailyReportViewModel
import com.heldairy.feature.report.QuestionUiState
import com.heldairy.feature.report.model.QuestionKind
import com.heldairy.ui.theme.Spacing
import com.heldairy.ui.theme.CornerRadius
import com.heldairy.ui.theme.Elevation
import com.heldairy.ui.theme.KittyBackground
import com.heldairy.ui.theme.BackgroundTheme
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun DailyReportRoute(
    paddingValues: PaddingValues,
    carePrompt: String,
    modifier: Modifier = Modifier,
    viewModel: DailyReportViewModel = viewModel(factory = DailyReportViewModel.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val adviceState by viewModel.adviceState.collectAsStateWithLifecycle()
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
        KittyBackground(backgroundRes = BackgroundTheme.DAILY_REPORT) {
            DailyReportScreen(
                state = state,
                adviceState = adviceState,
                carePrompt = carePrompt,
                onOptionSelected = viewModel::onOptionSelected,
                onSliderValueChange = viewModel::onSliderValueChanged,
                onSliderValueChangeFinished = viewModel::onSliderValueChangeFinished,
                onTextChanged = viewModel::onTextChanged,
                onSubmit = viewModel::submitDailyReport,
                onAdviceRetry = viewModel::refreshAdvice,
                onToggleAdviceCollapse = viewModel::toggleAdviceCollapse,
                onMarkAdviceHelpful = viewModel::markCurrentAdviceHelpful,
                onMarkAdviceNotHelpful = viewModel::markCurrentAdviceNotHelpful,
                modifier = Modifier.fillMaxSize()
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Spacing.M)
        )
    }
}

@Composable
fun DailyReportScreen(
    state: DailyReportUiState,
    adviceState: AdviceUiState,
    carePrompt: String,
    onOptionSelected: (String, String) -> Unit,
    onSliderValueChange: (String, Float) -> Unit,
    onSliderValueChangeFinished: (String) -> Unit,
    onTextChanged: (String, String) -> Unit,
    onSubmit: () -> Unit,
    onAdviceRetry: () -> Unit,
    onToggleAdviceCollapse: () -> Unit,
    onMarkAdviceHelpful: () -> Unit,
    onMarkAdviceNotHelpful: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.M,
            end = Spacing.M,
            top = Spacing.L,
            bottom = Spacing.M
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.M)
    ) {
        item { DailyHeader(carePrompt = carePrompt) }
        
        // Time suggestion hint
        item {
            Text(
                text = "建议每晚 20:00 后填写日报记录",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        
        if (state.isTimeRestricted) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Nightlight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = state.restrictionMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "记录睡前状态，让我更了解你的健康规律",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        
        itemsIndexed(
            items = state.questions.filter { it.isVisible },
            key = { _, item -> item.question.id }
        ) { index, questionState ->
            QuestionCard(
                position = index + 1,
                questionState = questionState,
                onOptionSelected = onOptionSelected,
                onSliderValueChange = onSliderValueChange,
                onSliderValueFinished = onSliderValueChangeFinished,
                onTextChanged = onTextChanged,
                isEnabled = !state.isTimeRestricted
            )
        }
        item {
            SubmitBar(
                canSubmit = state.canSubmit && !state.isTimeRestricted,
                isSaving = state.isSaving,
                onSubmit = onSubmit,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            AdviceSection(
                adviceState = adviceState,
                onRetry = onAdviceRetry,
                onToggleCollapse = onToggleAdviceCollapse,
                onMarkHelpful = onMarkAdviceHelpful,
                onMarkNotHelpful = onMarkAdviceNotHelpful,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item { Spacer(modifier = Modifier.height(Spacing.M)) }
    }
}

@Composable
private fun DailyHeader(carePrompt: String) {
    val now = LocalDateTime.now()
    val dateText = now.format(DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE"))
    val greeting = when (now.hour) {
        in 5..11 -> "早安"
        in 12..17 -> "午安"
        else -> "晚安"
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(CornerRadius.Medium),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.Low),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.M),
            horizontalArrangement = Arrangement.spacedBy(Spacing.S),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Nightlight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.XXS)) {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$greeting，今天 Kitty 陪你记录状态～",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = carePrompt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuestionCard(
    position: Int,
    questionState: QuestionUiState,
    onOptionSelected: (String, String) -> Unit,
    onSliderValueChange: (String, Float) -> Unit,
    onSliderValueFinished: (String) -> Unit,
    onTextChanged: (String, String) -> Unit,
    isEnabled: Boolean = true
) {
    Card(
        shape = RoundedCornerShape(CornerRadius.Medium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.None),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                    )
                ),
                shape = RoundedCornerShape(CornerRadius.Medium)
            )
    ) {
        Column(
            modifier = Modifier.padding(Spacing.M),
            verticalArrangement = Arrangement.spacedBy(Spacing.M)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.S)
            ) {
                NumberBadge(position)
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.XXS)) {
                    Text(
                        text = questionState.question.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = questionState.question.prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            when (questionState) {
                is QuestionUiState.SingleChoice -> ChoiceSection(questionState, onOptionSelected, isEnabled)
                is QuestionUiState.MultipleChoice -> MultiChoiceSection(questionState, onOptionSelected, isEnabled)
                is QuestionUiState.Slider -> SliderSection(questionState, onSliderValueChange, onSliderValueFinished, isEnabled)
                is QuestionUiState.TextInput -> TextInputSection(questionState, onTextChanged, isEnabled)
            }
        }
    }
}

@Composable
private fun NumberBadge(index: Int) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = index.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceSection(
    state: QuestionUiState.SingleChoice,
    onOptionSelected: (String, String) -> Unit,
    isEnabled: Boolean = true
) {
    val options = (state.question.kind as QuestionKind.SingleChoice).options
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.S),
        verticalArrangement = Arrangement.spacedBy(Spacing.S)
    ) {
        options.forEach { option ->
            val isSelected = option.id == state.selectedOptionId
            FilterChip(
                selected = isSelected,
                enabled = isEnabled,
                onClick = { onOptionSelected(state.question.id, option.id) },
                label = {
                    Text(
                        option.label,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
                    )
                },
                shape = RoundedCornerShape(CornerRadius.XLarge),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                    borderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MultiChoiceSection(
    state: QuestionUiState.MultipleChoice,
    onOptionSelected: (String, String) -> Unit,
    isEnabled: Boolean = true
) {
    val options = (state.question.kind as QuestionKind.MultipleChoice).options
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.S)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.S),
            verticalArrangement = Arrangement.spacedBy(Spacing.S)
        ) {
            options.forEach { option ->
                val selected = state.selectedOptionIds.contains(option.id)
                FilterChip(
                    selected = selected,
                    onClick = { onOptionSelected(state.question.id, option.id) },
                    enabled = isEnabled,
                    label = {
                        Text(
                            option.label,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    shape = RoundedCornerShape(CornerRadius.XLarge),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        borderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.XS),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
    onSliderValueFinished: (String) -> Unit,
    isEnabled: Boolean = true
) {
    val sliderKind = state.question.kind as QuestionKind.Slider
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.S)) {
        Text(
            text = "${state.currentValue}${sliderKind.valueSuffix.orEmpty()}",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.XXS),
            textAlign = TextAlign.Center
        )
        Slider(
            value = state.currentValue.toFloat(),
            valueRange = sliderKind.valueRange.first.toFloat()..sliderKind.valueRange.last.toFloat(),
            onValueChange = { newValue -> onSliderValueChange(state.question.id, newValue) },
            onValueChangeFinished = { onSliderValueFinished(state.question.id) },
            enabled = isEnabled,
            steps = sliderKind.valueRange.last - sliderKind.valueRange.first - 1,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
        sliderKind.supportingText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TextInputSection(
    state: QuestionUiState.TextInput,
    onTextChanged: (String, String) -> Unit,
    isEnabled: Boolean = true
) {
    val textKind = state.question.kind as QuestionKind.TextInput
    OutlinedTextField(
        value = state.text,
        onValueChange = { updated -> onTextChanged(state.question.id, updated) },
        enabled = isEnabled,
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
        maxLines = 4,
        shape = RoundedCornerShape(CornerRadius.Medium)
    )
}

@Composable
private fun SubmitBar(
    canSubmit: Boolean,
    isSaving: Boolean,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(CornerRadius.Medium),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.Low),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(Spacing.M),
            verticalArrangement = Arrangement.spacedBy(Spacing.XS)
        ) {
            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
                enabled = canSubmit && !isSaving,
                shape = RoundedCornerShape(CornerRadius.XLarge)
            ) {
                Crossfade(targetState = isSaving, label = "buttonState") { saving ->
                    if (saving) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.S)
                        ) {
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
}

@Composable
private fun AdviceSection(
    adviceState: AdviceUiState,
    onRetry: () -> Unit,
    onToggleCollapse: () -> Unit,
    onMarkHelpful: () -> Unit,
    onMarkNotHelpful: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleCollapse),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hello Kitty 今日建议",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (adviceState.isCollapsed) Icons.Outlined.ExpandMore else Icons.Outlined.ExpandLess,
                contentDescription = if (adviceState.isCollapsed) "展开" else "收起"
            )
        }

        if (!adviceState.isCollapsed) {
            when {
                !adviceState.aiEnabled -> AdviceInfoCard("AI 功能已关闭，请在【设置】中开启，Kitty 就能陪你一起生活啦～")
                adviceState.apiKeyMissing -> AdviceInfoCard("请在【设置】中填写 DeepSeek API Key，Kitty 会马上为你生成建议哦～")
                adviceState.isGenerating -> AdviceLoadingCard()
                adviceState.advice != null -> AdviceResultCard(
                    payload = adviceState.advice,
                    generatedAt = adviceState.lastGeneratedAt,
                    isFallback = adviceState.isFallback,
                    isCollapsed = adviceState.isCollapsed,
                    trackingItems = adviceState.trackingItems,
                    onMarkHelpful = onMarkHelpful,
                    onMarkNotHelpful = onMarkNotHelpful
                )
                adviceState.errorMessage != null -> AdviceErrorCard(adviceState.errorMessage, onRetry)
                else -> AdviceInfoCard("完成基础问题后，Kitty 会马上生成个性化建议～")
            }
            if (adviceState.canRequestAdvice) {
                TextButton(onClick = onRetry, enabled = !adviceState.isGenerating) {
                    Text("重新生成 Kitty 建议")
                }
            }
        }
    }
}

@Composable
private fun AdviceInfoCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun AdviceLoadingCard() {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Text("Kitty 正在整理建议，请稍等一下下～")
        }
    }
}

@Composable
private fun AdviceErrorCard(message: String, onRetry: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Button(onClick = onRetry) {
                Text("重试生成")
            }
        }
    }
}

@Composable
private fun AdviceResultCard(
    payload: AdvicePayload,
    generatedAt: Long?,
    isFallback: Boolean,
    isCollapsed: Boolean,
    trackingItems: List<AdviceTrackingEntity>,
    onMarkHelpful: () -> Unit,
    onMarkNotHelpful: () -> Unit
) {
    Card {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .heightIn(max = 320.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            generatedAt?.let {
                Text(
                    text = "生成时间：${formatTimestamp(it)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isFallback || payload.source == AdviceSource.FALLBACK) {
                Text(
                    text = "AI 建议暂时不可用，以下是 Kitty 的基础建议～",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            if (!isCollapsed) {
                AdviceListBlock(title = "观察", items = payload.observations)
                AdviceListBlock(title = "建议", items = payload.actions)
                AdviceListBlock(title = "明日关注", items = payload.tomorrowFocus)
                if (payload.redFlags.isNotEmpty()) {
                    AdviceListBlock(title = "提醒", items = payload.redFlags)
                }
                
                // Phase 3: 反馈按钮
                val hasFeedback = trackingItems.any { 
                    it.userFeedback != null && it.userFeedback.isNotBlank()
                }
                if (!hasFeedback) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Kitty 的建议对你有帮助吗～",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = onMarkHelpful, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.ThumbUp,
                                contentDescription = "有帮助",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = onMarkNotHelpful, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.ThumbDown,
                                contentDescription = "无帮助",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    val feedback = trackingItems.firstOrNull()?.userFeedback
                    Text(
                        text = when(feedback) {
                            UserFeedback.HELPFUL -> "✓ 已标记为有帮助"
                            UserFeedback.NOT_HELPFUL -> "✓ 已标记为无帮助"
                            UserFeedback.EXECUTED -> "✓ 已执行"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@Composable
private fun AdviceListBlock(title: String, items: List<String>) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        items.forEach { item ->
            Text(
                text = "• $item",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatTimestamp(epochMillis: Long): String {
    val instant = java.time.Instant.ofEpochMilli(epochMillis)
    val zone = java.time.ZoneId.systemDefault()
    val local = java.time.LocalDateTime.ofInstant(instant, zone)
    return "${local.monthValue}月${local.dayOfMonth}日 ${local.hour}:${local.minute.toString().padStart(2, '0')}"
}



package com.heldairy.feature.report.ui

import androidx.compose.animation.Crossfade
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
import com.heldairy.core.data.AdviceSource
import com.heldairy.feature.report.AdviceUiState
import com.heldairy.feature.report.DailyReportEvent
import com.heldairy.feature.report.DailyReportUiState
import com.heldairy.feature.report.DailyReportViewModel
import com.heldairy.feature.report.QuestionUiState
import com.heldairy.feature.report.model.QuestionKind
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
    val palette = rememberReportPalette()

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
            .background(palette.background)
            .padding(paddingValues)
    ) {
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
    adviceState: AdviceUiState,
    carePrompt: String,
    onOptionSelected: (String, String) -> Unit,
    onSliderValueChange: (String, Float) -> Unit,
    onSliderValueChangeFinished: (String) -> Unit,
    onTextChanged: (String, String) -> Unit,
    onSubmit: () -> Unit,
    onAdviceRetry: () -> Unit,
    onToggleAdviceCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { DailyHeader(carePrompt = carePrompt) }
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
                onTextChanged = onTextChanged
            )
        }
        item {
            SubmitBar(
                canSubmit = state.canSubmit,
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
                modifier = Modifier.fillMaxWidth()
            )
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
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
    val palette = rememberReportPalette()

    GlowCard(
        glowBrush = Brush.radialGradient(
            colors = listOf(palette.accentPrimary.copy(alpha = 0.35f), Color.Transparent)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.card, shape = RoundedCornerShape(22.dp))
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(palette.badgeBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Outlined.Nightlight, contentDescription = null, tint = palette.accentPrimary)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = dateText, style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                Text(
                    text = "$greeting，今天我陪你记录状态。",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = palette.textPrimary
                )
                Text(text = carePrompt, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
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
    onTextChanged: (String, String) -> Unit
) {
    val palette = rememberReportPalette()
    GlowCard(
        glowBrush = Brush.radialGradient(
            colors = listOf(palette.accentPrimary.copy(alpha = 0.35f), palette.accentSecondary.copy(alpha = 0.1f), Color.Transparent)
        ),
        shape = RoundedCornerShape(26.dp)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = palette.cardElevated),
            border = BorderStroke(1.dp, palette.cardStroke)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberBadge(position)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = questionState.question.title, style = MaterialTheme.typography.titleMedium, color = palette.textPrimary)
                        Text(
                            text = questionState.question.prompt,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.textSecondary
                        )
                    }
                }
                when (questionState) {
                    is QuestionUiState.SingleChoice -> ChoiceSection(questionState, onOptionSelected)
                    is QuestionUiState.MultipleChoice -> MultiChoiceSection(questionState, onOptionSelected)
                    is QuestionUiState.Slider -> SliderSection(questionState, onSliderValueChange, onSliderValueFinished)
                    is QuestionUiState.TextInput -> TextInputSection(questionState, onTextChanged)
                }
            }
        }
    }
}

@Composable
private fun NumberBadge(index: Int) {
    val palette = rememberReportPalette()
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(palette.badgeBackground),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = index.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = palette.accentPrimary
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceSection(
    state: QuestionUiState.SingleChoice,
    onOptionSelected: (String, String) -> Unit
) {
    val palette = rememberReportPalette()
    val options = (state.question.kind as QuestionKind.SingleChoice).options
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option.id == state.selectedOptionId,
                onClick = { onOptionSelected(state.question.id, option.id) },
                    label = { Text(option.label, color = if (option.id == state.selectedOptionId) MaterialTheme.colorScheme.onPrimary else palette.textPrimary) },
                colors = FilterChipDefaults.filterChipColors(
                        containerColor = palette.card,
                        selectedContainerColor = palette.accentPrimary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = palette.card
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = option.id == state.selectedOptionId,
                        selectedBorderColor = palette.accentPrimary,
                        borderColor = palette.chipBorder
                )
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
    val palette = rememberReportPalette()
    val options = (state.question.kind as QuestionKind.MultipleChoice).options
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            options.forEach { option ->
                val selected = state.selectedOptionIds.contains(option.id)
                FilterChip(
                    selected = selected,
                    onClick = { onOptionSelected(state.question.id, option.id) },
                    label = { Text(option.label, color = if (selected) MaterialTheme.colorScheme.onPrimary else palette.textPrimary) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = palette.card,
                        selectedContainerColor = palette.accentPrimary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = palette.card
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        selectedBorderColor = palette.accentPrimary,
                        borderColor = palette.chipBorder
                    )
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "已选 ${state.selectedOptionIds.size}/${state.maxSelection}",
                style = MaterialTheme.typography.labelMedium,
                color = palette.accentPrimary
            )
            state.helperText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary
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
    val palette = rememberReportPalette()
    val sliderKind = state.question.kind as QuestionKind.Slider
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "${state.currentValue}${sliderKind.valueSuffix.orEmpty()}",
            style = MaterialTheme.typography.headlineMedium,
            color = palette.textPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            textAlign = TextAlign.Center
        )
        Slider(
            value = state.currentValue.toFloat(),
            valueRange = sliderKind.valueRange.first.toFloat()..sliderKind.valueRange.last.toFloat(),
            onValueChange = { newValue -> onSliderValueChange(state.question.id, newValue) },
            onValueChangeFinished = { onSliderValueFinished(state.question.id) },
            steps = sliderKind.valueRange.last - sliderKind.valueRange.first - 1,
            colors = SliderDefaults.colors(
                thumbColor = palette.accentPrimary,
                activeTrackColor = palette.accentPrimary,
                inactiveTrackColor = palette.sliderInactive
            )
        )
        sliderKind.supportingText?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
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
                    color = Color(0xFF8DA0C2)
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
    val palette = rememberReportPalette()
    GlowCard(
        glowBrush = Brush.radialGradient(
            colors = listOf(palette.accentPrimary.copy(alpha = 0.35f), Color.Transparent)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = palette.cardElevated),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
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
                    color = palette.textSecondary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AdviceSection(
    adviceState: AdviceUiState,
    onRetry: () -> Unit,
    onToggleCollapse: () -> Unit,
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
                text = "AI 今日建议",
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
                !adviceState.aiEnabled -> AdviceInfoCard("AI 功能已关闭，请在“设置”中开启以获取生活管家建议。")
                adviceState.apiKeyMissing -> AdviceInfoCard("请在“设置”中填写 DeepSeek API Key，我会立刻为你生成建议。")
                adviceState.isGenerating -> AdviceLoadingCard()
                adviceState.advice != null -> AdviceResultCard(
                    payload = adviceState.advice,
                    generatedAt = adviceState.lastGeneratedAt,
                    isFallback = adviceState.isFallback,
                    isCollapsed = adviceState.isCollapsed
                )
                adviceState.errorMessage != null -> AdviceErrorCard(adviceState.errorMessage, onRetry)
                else -> AdviceInfoCard("完成基础问题后，我会立刻生成个性化建议。")
            }
            if (adviceState.canRequestAdvice) {
                TextButton(onClick = onRetry, enabled = !adviceState.isGenerating) {
                    Text("重新生成 AI 建议")
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
            Text("AI 正在整理建议，请稍等…")
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
    isCollapsed: Boolean
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
                    text = "AI 建议暂时不可用，以下为基础建议。",
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
            }
        }
    }
}

@Composable
private fun AdviceListBlock(title: String, items: List<String>) {
    if (items.isEmpty()) return
    val palette = rememberReportPalette()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
        items.forEach { item ->
            Text(
                text = "• $item",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textPrimary
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

@Composable
private fun GlowCard(
    glowBrush: Brush,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(glowBrush, shape = shape)
            .padding(1.5.dp)
    ) {
        Box(modifier = Modifier.clip(shape)) {
            content()
        }
    }
}

@Composable
private fun rememberReportPalette(): ReportPalette {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.5f
    return if (isDark) {
        ReportPalette(
            background = Brush.verticalGradient(listOf(Color(0xFF0C101C), Color(0xFF0A0D18))),
            card = Color(0xFF141B2C),
            cardElevated = Color(0xFF11182A),
            accentPrimary = Color(0xFF4E7DFF),
            accentSecondary = Color(0xFF7C5CFF),
            textPrimary = Color(0xFFE7ECF7),
            textSecondary = Color(0xFF9FB2DA),
            badgeBackground = Color(0xFF4E7DFF).copy(alpha = 0.18f),
            chipBorder = Color(0xFF2C3650),
            sliderInactive = Color(0xFF1F2740),
            cardStroke = Color(0xFF2C3650)
        )
    } else {
        ReportPalette(
            background = Brush.verticalGradient(listOf(colorScheme.surfaceVariant, colorScheme.surface)),
            card = colorScheme.surfaceVariant,
            cardElevated = colorScheme.surfaceVariant.copy(alpha = 0.9f),
            accentPrimary = colorScheme.primary,
            accentSecondary = colorScheme.secondary,
            textPrimary = colorScheme.onSurface,
            textSecondary = colorScheme.onSurfaceVariant,
            badgeBackground = colorScheme.primary.copy(alpha = 0.12f),
            chipBorder = colorScheme.outlineVariant,
            sliderInactive = colorScheme.surfaceVariant,
            cardStroke = colorScheme.outlineVariant
        )
    }
}

private data class ReportPalette(
    val background: Brush,
    val card: Color,
    val cardElevated: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val badgeBackground: Color,
    val chipBorder: Color,
    val sliderInactive: Color,
    val cardStroke: Color
)

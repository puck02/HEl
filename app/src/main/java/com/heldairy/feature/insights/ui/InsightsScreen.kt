package com.heldairy.feature.insights.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heldairy.core.data.InsightSymptomMetric
import com.heldairy.core.data.InsightWindow
import com.heldairy.core.data.TrendFlag
import com.heldairy.core.data.WeeklyInsightStatus
import com.heldairy.feature.insights.InsightWindowType
import com.heldairy.feature.insights.InsightsUiState
import com.heldairy.feature.insights.InsightsViewModel
import com.heldairy.feature.insights.WeeklyInsightUi
import kotlin.math.roundToInt

private data class InsightPalette(
	val backgroundStart: Color,
	val backgroundEnd: Color,
	val panel: Color,
	val border: Color,
	val track: Color,
	val selectorContainerSelected: Color,
	val selectorContentSelected: Color,
	val selectorContent: Color
)

@Composable
private fun rememberInsightPalette(): InsightPalette {
	val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
	return if (dark) {
		InsightPalette(
			backgroundStart = Color(0xFF0F1118),
			backgroundEnd = Color(0xFF0B0D12),
			panel = Color(0xFF141821),
			border = Color(0xFF1E2331),
			track = Color(0xFF23293B),
			selectorContainerSelected = Color(0xFF4B4E59),
			selectorContentSelected = Color.White,
			selectorContent = MaterialTheme.colorScheme.onSurfaceVariant
		)
	} else {
		InsightPalette(
			backgroundStart = Color(0xFFF7F9FD),
			backgroundEnd = Color(0xFFF1F4FA),
			panel = Color.White,
			border = Color(0xFFE4E7F0),
			track = Color(0xFFE8ECF5),
			selectorContainerSelected = Color(0xFFE3E7F2),
			selectorContentSelected = Color(0xFF1F2533),
			selectorContent = Color(0xFF5E6470)
		)
	}
}

@Composable
private fun WeeklyInsightExpandedOverlay(weekly: WeeklyInsightUi, onClose: () -> Unit) {
	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(Color.Black.copy(alpha = 0.55f))
	) {
		Card(
			shape = RoundedCornerShape(24.dp),
			modifier = Modifier
				.align(Alignment.Center)
				.fillMaxWidth()
				.padding(horizontal = 20.dp)
				.heightIn(min = 320.dp, max = 560.dp),
			colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
		) {
			Column(modifier = Modifier.padding(20.dp)) {
				Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
					Text(text = "AI 洞察建议", style = MaterialTheme.typography.titleMedium)
					IconButton(onClick = onClose) {
						Icon(imageVector = Icons.Default.Close, contentDescription = "关闭")
					}
				}
				Spacer(modifier = Modifier.height(12.dp))
				val payload = weekly.result?.payload
				if (payload != null) {
					Column(
						modifier = Modifier.verticalScroll(rememberScrollState()),
						verticalArrangement = Arrangement.spacedBy(12.dp)
					) {
						Text(text = payload.summary, style = MaterialTheme.typography.bodyMedium)
						if (payload.highlights.isNotEmpty()) {
							Text(text = "重点", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
							payload.highlights.forEach { highlight ->
								Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
									Icon(imageVector = Icons.Outlined.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
									Text(text = highlight, style = MaterialTheme.typography.bodyMedium)
								}
							}
						}
					}
				} else {
					Text(text = "暂时没有可展示的 AI 建议。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
				}
			}
		}
	}
}


@Composable
fun InsightsRoute(
	paddingValues: PaddingValues,
	modifier: Modifier = Modifier,
	viewModel: InsightsViewModel = viewModel(factory = InsightsViewModel.Factory)
) {
	val state by viewModel.uiState.collectAsStateWithLifecycle()

	InsightsScreen(
		state = state,
		onSelectWindow = viewModel::selectWindow,
		onRetryWeekly = { viewModel.refreshWeekly(force = true) },
		modifier = modifier.padding(paddingValues)
	)
}

@Composable
fun InsightsScreen(
	state: InsightsUiState,
	onSelectWindow: (InsightWindowType) -> Unit,
	onRetryWeekly: () -> Unit,
	modifier: Modifier = Modifier
) {
	val palette = rememberInsightPalette()
	var showWeeklyExpanded by rememberSaveable { mutableStateOf(false) }
	val selectedWindow = when (state.selectedWindow) {
		InsightWindowType.Seven -> state.summary?.window7
		InsightWindowType.Thirty -> state.summary?.window30
	}

	Box(
		modifier = modifier
			.fillMaxSize()
			.background(
				Brush.verticalGradient(
					colors = listOf(
						palette.backgroundStart,
						palette.backgroundEnd
					)
				)
			)
	) {
		LazyColumn(
			modifier = Modifier.blur(if (showWeeklyExpanded) 16.dp else 0.dp),
			contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
			verticalArrangement = Arrangement.spacedBy(16.dp)
		) {
			item {
				WeeklyInsightCard(
					weekly = state.weeklyInsight,
					onRetry = onRetryWeekly,
					onOpen = { showWeeklyExpanded = true }
				)
			}
			item { WindowSelector(selected = state.selectedWindow, onSelectWindow = onSelectWindow, palette = palette) }

			if (state.isLoading) {
				item { LoadingCard() }
			}

			state.error?.let { error ->
				item { ErrorCard(message = error) }
			}

			selectedWindow?.let { window ->
				item { CompletionCard(window, palette) }
				item { InsightGrid(window, palette) }
				if (window.symptomMetrics.isNotEmpty()) {
					item { SymptomCard(metrics = window.symptomMetrics, palette = palette) }
				}
			} ?: run {
				if (!state.isLoading && state.error == null) {
					item { EmptyState() }
				}
			}
		}

		if (showWeeklyExpanded) {
			WeeklyInsightExpandedOverlay(
				weekly = state.weeklyInsight,
				onClose = { showWeeklyExpanded = false }
			)
		}
	}
}

@Composable
private fun WindowSelector(selected: InsightWindowType, onSelectWindow: (InsightWindowType) -> Unit, palette: InsightPalette) {
	Row(
		horizontalArrangement = Arrangement.spacedBy(8.dp),
		modifier = Modifier
			.fillMaxWidth()
			.background(
				palette.panel,
				shape = RoundedCornerShape(14.dp)
			)
			.padding(4.dp)
	) {
		SegmentButton(
			label = "近7天",
			selected = selected == InsightWindowType.Seven,
			onClick = { onSelectWindow(InsightWindowType.Seven) },
			modifier = Modifier.weight(1f),
			palette = palette
		)
		SegmentButton(
			label = "近30天",
			selected = selected == InsightWindowType.Thirty,
			onClick = { onSelectWindow(InsightWindowType.Thirty) },
			modifier = Modifier.weight(1f),
			palette = palette
		)
	}
}

@Composable
private fun SegmentButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, palette: InsightPalette) {
	val container = if (selected) palette.selectorContainerSelected else Color.Transparent
	val content = if (selected) palette.selectorContentSelected else palette.selectorContent
	FilterChip(
		selected = selected,
		onClick = onClick,
		label = { Text(label) },
		modifier = modifier,
		shape = RoundedCornerShape(12.dp),
		colors = FilterChipDefaults.filterChipColors(
			selectedContainerColor = container,
			selectedLabelColor = content,
			labelColor = content,
			containerColor = Color.Transparent
		)
	)
}

@Composable
private fun LoadingCard() {
	Card(modifier = Modifier.fillMaxWidth()) {
		Row(
			modifier = Modifier
				.padding(20.dp)
				.fillMaxWidth(),
			horizontalArrangement = Arrangement.spacedBy(12.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			CircularProgressIndicator(modifier = Modifier.height(24.dp))
			Text("正在加载洞察…", style = MaterialTheme.typography.bodyMedium)
		}
	}
}

@Composable
private fun ErrorCard(message: String) {
	Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
		Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
			Text(text = "加载失败", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
			Text(text = message, color = MaterialTheme.colorScheme.onErrorContainer)
		}
	}
}

@Composable
private fun EmptyState() {
	Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
		Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
			Text(text = "暂无数据", style = MaterialTheme.typography.titleMedium)
			Text(text = "完成每日填报后，这里会出现你的习惯分布和趋势。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
		}
	}
}

@Composable
private fun CompletionCard(window: InsightWindow, palette: InsightPalette) {
	Card(
		colors = CardDefaults.cardColors(
			containerColor = palette.panel
		),
		modifier = Modifier.fillMaxWidth()
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
				Text(text = "今日进度", style = MaterialTheme.typography.titleMedium)
				Text(text = "${window.entryCount}/${window.days} 天已完成", color = MaterialTheme.colorScheme.onSurfaceVariant)
				LinearProgressIndicator(
					progress = { (window.entryCount.toFloat() / window.days.toFloat()).coerceIn(0f, 1f) },
					modifier = Modifier.fillMaxWidth()
				)
			}
			Icon(imageVector = Icons.Outlined.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
		}
	}
}

@Composable
private fun InsightGrid(window: InsightWindow, palette: InsightPalette) {
	Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
		SleepQualityCard(window, palette)
		MoodDistributionCard(window, palette)
		SymptomGrid(metrics = window.symptomMetrics, palette = palette)
	}
}

@Composable
private fun SleepQualityCard(window: InsightWindow, palette: InsightPalette) {
	val buckets = listOf(
		SleepSegment(label = "≥8h", key = ">8", midpoint = 8.5f, color = Color(0xFF51D1BD)),
		SleepSegment(label = "7-8h", key = "7-8", midpoint = 7.5f, color = Color(0xFF5D5FEF)),
		SleepSegment(label = "6-7h", key = "6-7", midpoint = 6.5f, color = Color(0xFF9D79FF)),
		SleepSegment(label = "<6h", key = "<6", midpoint = 5f, color = Color(0xFF8C52FF))
	).map { segment -> segment.copy(value = window.sleepDistribution[segment.key] ?: 0) }

	val total = buckets.sumOf { it.value }.coerceAtLeast(1)
	val averageHours = buckets.sumOf { it.midpoint.toDouble() * it.value } / total
	val sleepScore = buckets.sumOf { bucketWeight(it) * it.value }.toDouble() / total

	Card(
		colors = CardDefaults.cardColors(containerColor = palette.panel),
		modifier = Modifier.fillMaxWidth()
	) {
		Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
			Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
				Text(text = "睡眠质量", style = MaterialTheme.typography.titleMedium)
				Text(text = "平均 ${formatHours(averageHours)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
			}
			Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
				RingChart(segments = buckets, palette = palette, centerLabel = sleepScore.roundToInt().coerceIn(0, 100).toString(), centerCaption = "分数", modifier = Modifier.size(160.dp))
				Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
					buckets.forEach { bucket ->
						val percent = (bucket.value * 100 / total).coerceAtMost(100)
						LegendRowCompact(color = bucket.color, label = bucket.label, percent = percent)
					}
				}
			}
		}
	}
}

@Composable
private fun MoodDistributionCard(window: InsightWindow, palette: InsightPalette) {
	val irritability = window.symptomMetrics.firstOrNull { it.questionId == "mood_irritability" }?.average ?: 5.0
	val happy = (100 - irritability * 8).toInt().coerceIn(20, 90)
	val calm = (70 - (irritability - 5) * 6).toInt().coerceIn(10, 80)
	val tired = (100 - happy - calm).coerceIn(5, 60)
	val rows = listOf(
		MoodRow("愉快 (Happy)", happy, Color(0xFFF8B84E)),
		MoodRow("平静 (Calm)", calm, Color(0xFF6FA4FF)),
		MoodRow("疲劳 (Tired)", tired, Color(0xFF9EA3B5))
	)

	Card(colors = CardDefaults.cardColors(containerColor = palette.panel)) {
		Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
			Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
				Text(text = "情绪分布", style = MaterialTheme.typography.titleMedium)
				Text(text = "主要情绪: 愉快", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
			}
			rows.forEach { row ->
				MoodBar(row, palette)
			}
		}
	}
}

@Composable
private fun SymptomGrid(metrics: List<InsightSymptomMetric>, palette: InsightPalette) {
	if (metrics.isEmpty()) return
	val topMetrics = metrics.take(4)
	OutlinedCard(
		colors = CardDefaults.outlinedCardColors(containerColor = palette.panel),
		border = BorderStroke(1.dp, palette.border)
	) {
		Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
			Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
				Text(text = "身体症状监测", style = MaterialTheme.typography.titleMedium)
				TextButton(onClick = { /* reserved for navigation */ }) { Text("查看详情") }
			}
			Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
				topMetrics.forEach { metric ->
					SymptomPill(metric = metric, palette = palette, modifier = Modifier.weight(1f))
				}
			}
		}
	}
}

private data class SleepSegment(val label: String, val key: String, val midpoint: Float, val color: Color, val value: Int = 0)
private data class MoodRow(val label: String, val percent: Int, val color: Color)

private fun bucketWeight(segment: SleepSegment): Int = when (segment.key) {
	">8" -> 95
	"7-8" -> 90
	"6-7" -> 75
	else -> 55
}

private fun formatHours(hours: Double): String {
	val h = hours.toInt()
	val m = ((hours - h) * 60).roundToInt()
	return "${h}h${m}m"
}

@Composable
private fun RingChart(segments: List<SleepSegment>, palette: InsightPalette, centerLabel: String, centerCaption: String, modifier: Modifier = Modifier) {
	Box(contentAlignment = Alignment.Center, modifier = modifier) {
		Canvas(modifier = Modifier.fillMaxSize()) {
			val safeSegments = if (segments.all { it.value == 0 }) segments.map { it.copy(value = 1) } else segments
			val total = safeSegments.sumOf { it.value }.coerceAtLeast(1)
			val strokeWidth = 18.dp.toPx()
			drawArc(
				color = palette.track,
				startAngle = 0f,
				sweepAngle = 360f,
				useCenter = false,
				style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
			)
			var startAngle = -90f
			safeSegments.forEach { segment ->
				val sweep = 360f * (segment.value / total.toFloat())
				drawArc(
					color = segment.color,
					startAngle = startAngle,
					sweepAngle = sweep,
					useCenter = false,
					style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
				)
				startAngle += sweep
			}
		}
		Column(horizontalAlignment = Alignment.CenterHorizontally) {
			Text(text = centerLabel, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
			Text(text = centerCaption, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
		}
	}
}

@Composable
private fun LegendRowCompact(color: Color, label: String, percent: Int) {
	Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
		Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
			Box(modifier = Modifier.size(10.dp).background(color, shape = RoundedCornerShape(50)))
			Text(text = label, style = MaterialTheme.typography.bodyMedium)
		}
		Text(text = "$percent%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
	}
}

@Composable
private fun MoodBar(row: MoodRow, palette: InsightPalette) {
	Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
		Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
			Text(text = row.label, style = MaterialTheme.typography.bodyMedium)
			Text(text = "${row.percent}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
		}
		LinearProgressIndicator(
			progress = { row.percent / 100f },
			modifier = Modifier.fillMaxWidth(),
			color = row.color,
			trackColor = palette.track
		)
	}
}

@Composable
private fun SymptomPill(metric: InsightSymptomMetric, palette: InsightPalette, modifier: Modifier = Modifier) {
	val riskColor = when {
		metric.average >= 7 -> Color(0xFFE96A76)
		metric.average >= 4 -> Color(0xFFF1B563)
		else -> Color(0xFF51D1BD)
	}
	val status = when {
		metric.average >= 7 -> "高风险"
		metric.average >= 4 -> "需注意"
		else -> "低风险"
	}
	Card(colors = CardDefaults.cardColors(containerColor = palette.border), modifier = modifier) {
		Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
			Text(text = symptomLabel(metric.questionId), style = MaterialTheme.typography.bodyMedium)
			Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
				Text(text = status, color = riskColor, style = MaterialTheme.typography.labelMedium)
				TrendIcon(metric.trend)
			}
		}
	}
}

@Composable
private fun SymptomCard(metrics: List<InsightSymptomMetric>, palette: InsightPalette) {
	Card(colors = CardDefaults.cardColors(containerColor = palette.panel)) {
		Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
			Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
				Icon(imageVector = Icons.Outlined.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
				Text(text = "症状趋势", style = MaterialTheme.typography.titleMedium)
			}
			metrics.forEach { metric ->
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceBetween,
					verticalAlignment = Alignment.CenterVertically
				) {
					Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
						Text(text = symptomLabel(metric.questionId), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
						Text(text = "均值 ${metric.average} · 最新 ${metric.latestValue ?: "-"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
					}
					TrendIcon(metric.trend)
				}
			}
		}
	}
}

@Composable
private fun TrendIcon(trend: TrendFlag) {
	val icon = when (trend) {
		TrendFlag.rising -> Icons.AutoMirrored.Outlined.TrendingUp
		TrendFlag.falling -> Icons.AutoMirrored.Outlined.TrendingDown
		TrendFlag.stable -> Icons.AutoMirrored.Outlined.TrendingFlat
	}
	Icon(imageVector = icon, contentDescription = trend.name, tint = MaterialTheme.colorScheme.primary)
}

private fun symptomLabel(questionId: String): String = when (questionId) {
	"headache_intensity" -> "头痛"
	"neck_back_intensity" -> "颈肩腰"
	"stomach_intensity" -> "胃部"
	"nasal_intensity" -> "鼻咽"
	"knee_intensity" -> "膝盖"
	"mood_irritability" -> "情绪烦躁"
	else -> questionId
}

@Composable
private fun WeeklyInsightCard(weekly: WeeklyInsightUi, onRetry: () -> Unit, onOpen: () -> Unit) {
	val gradient = Brush.linearGradient(
		colors = listOf(
			MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
			MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
		)
	)
	val hasPayload = weekly.result?.payload != null

	val clickableModifier = Modifier.clickable { onOpen() }
	Card(
		shape = RoundedCornerShape(20.dp),
		modifier = Modifier
			.fillMaxWidth()
			.then(clickableModifier),
		colors = CardDefaults.cardColors(containerColor = Color.Transparent)
	) {
		Box(
			modifier = Modifier
				.background(gradient)
				.padding(18.dp)
		) {
			Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
				Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
					Icon(imageVector = Icons.Outlined.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
					Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
						Text(text = "AI 洞察建议", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
						Text(text = "每周会生成 AI 洞察建议，点击查看", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = if (hasPayload) 1f else 0.6f))
					}
					if (weekly.status == WeeklyInsightStatus.Error) {
						Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimary, contentColor = MaterialTheme.colorScheme.primary)) {
							Text("重试")
						}
					}
				}
				weekly.result?.payload?.let { payload ->
					Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
						Text(text = payload.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary)
						Text(text = "点击查看完整建议 ›", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f))
					}
				}
			}
		}
	}
}


package com.heldairy.feature.insights.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.draw.clip
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
import com.heldairy.feature.insights.preview.PdfPreviewScreen
import com.heldairy.ui.theme.Spacing
import com.heldairy.ui.theme.CornerRadius
import com.heldairy.ui.theme.Elevation
import com.heldairy.ui.theme.success
import com.heldairy.ui.theme.warning
import com.heldairy.ui.theme.semanticError
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.PictureAsPdf
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.heldairy.feature.insights.WeeklyInsightUi
import kotlin.math.roundToInt

@Composable
private fun WeeklyInsightExpandedOverlay(weekly: WeeklyInsightUi, onClose: () -> Unit) {
	Box(
		modifier = Modifier
			.fillMaxSize()
			.background(Color.Black.copy(alpha = 0.55f))
	) {
		Card(
			shape = RoundedCornerShape(CornerRadius.Medium),
			modifier = Modifier
				.align(Alignment.Center)
				.fillMaxWidth()
				.padding(horizontal = Spacing.M)
				.heightIn(min = 320.dp, max = 560.dp),
			colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
			elevation = CardDefaults.cardElevation(defaultElevation = Elevation.High)
		) {
			Column(modifier = Modifier.padding(Spacing.M)) {
				Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
					Text(text = "AI 洞察建议", style = MaterialTheme.typography.titleMedium)
					IconButton(onClick = onClose) {
						Icon(imageVector = Icons.Default.Close, contentDescription = "关闭")
					}
				}
				Spacer(modifier = Modifier.height(Spacing.S))
				val payload = weekly.result?.payload
				if (payload != null) {
					Column(
						modifier = Modifier.verticalScroll(rememberScrollState()),
						verticalArrangement = Arrangement.spacedBy(Spacing.S)
					) {
						Text(text = payload.summary, style = MaterialTheme.typography.bodyMedium)
						if (payload.highlights.isNotEmpty()) {
							Text(text = "重点", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
							payload.highlights.forEach { highlight ->
									Row(horizontalArrangement = Arrangement.spacedBy(Spacing.XS), verticalAlignment = Alignment.CenterVertically) {
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
	val context = LocalContext.current
	
	// 保存PDF的Launcher
	val savePdfLauncher = rememberLauncherForActivityResult(
		contract = ActivityResultContracts.CreateDocument("application/pdf")
	) { uri ->
		uri?.let { viewModel.completeSave(it) }
	}

	// 显示PDF预览
	if (state.previewPdfFile != null) {
		PdfPreviewScreen(
			pdfFile = state.previewPdfFile!!,
			onSave = {
				val intent = viewModel.createSavePdfIntent(state.previewPdfFile!!)
				intent.getStringExtra(Intent.EXTRA_TITLE)?.let { fileName ->
					savePdfLauncher.launch(fileName)
				}
			},
			onShare = {
				viewModel.createShareIntent(state.previewPdfFile!!)?.let { shareIntent ->
					context.startActivity(shareIntent)
				}
			},
			onRegenerate = { viewModel.generatePreview() },
			onDismiss = { viewModel.closePreview() }
		)
	} else {
		InsightsScreen(
			state = state,
			onSelectWindow = viewModel::selectWindow,
			onRetryWeekly = { viewModel.refreshWeekly(force = true) },
			onGeneratePreview = { viewModel.generatePreview() },
			onClearReportStatus = viewModel::clearReportStatus,
			onSetReportDateRange = viewModel::setReportDateRange,
			modifier = modifier.padding(paddingValues)
		)
	}
}

@Composable
fun InsightsScreen(
	state: InsightsUiState,
	onSelectWindow: (InsightWindowType) -> Unit,
	onRetryWeekly: () -> Unit,
	onGeneratePreview: () -> Unit,
	onClearReportStatus: () -> Unit,
	onSetReportDateRange: (LocalDate?, LocalDate?) -> Unit,
	modifier: Modifier = Modifier
) {
	var showWeeklyExpanded by rememberSaveable { mutableStateOf(false) }
	val selectedWindow = when (state.selectedWindow) {
		InsightWindowType.Seven -> state.summary?.window7
		InsightWindowType.Thirty -> state.summary?.window30
	}

	Box(
		modifier = modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background)
	) {
		LazyColumn(
			modifier = Modifier.blur(if (showWeeklyExpanded) 16.dp else 0.dp),
			contentPadding = PaddingValues(
				start = Spacing.M,
				end = Spacing.M,
				top = Spacing.L,
				bottom = Spacing.M
			),
			verticalArrangement = Arrangement.spacedBy(Spacing.M)
		) {
			item {
				WeeklyInsightCard(
					weekly = state.weeklyInsight,
					onRetry = onRetryWeekly,
					onOpen = { showWeeklyExpanded = true }
				)
			}
			item { WindowSelector(selected = state.selectedWindow, onSelectWindow = onSelectWindow) }
			
			// 医生报表生成卡片
			item { 
				DoctorReportCard(
					isGenerating = state.isGeneratingPreview,
					errorMessage = state.previewError,
					startDate = state.reportStartDate,
					endDate = state.reportEndDate,
					onGenerate = onGeneratePreview,
					onDismissStatus = onClearReportStatus,
					onDateRangeChange = onSetReportDateRange
				) 
			}

			if (state.isLoading) {
				item { LoadingCard() }
			}

			state.error?.let { error ->
				item { ErrorCard(message = error) }
			}

			selectedWindow?.let { window ->
				item { CompletionCard(window) }
				item { InsightGrid(window) }
				if (window.symptomMetrics.isNotEmpty()) {
					item { SymptomCard(metrics = window.symptomMetrics) }
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
private fun WindowSelector(selected: InsightWindowType, onSelectWindow: (InsightWindowType) -> Unit) {
	Row(
		horizontalArrangement = Arrangement.spacedBy(Spacing.XS),
		modifier = Modifier
			.fillMaxWidth()
			.background(
				MaterialTheme.colorScheme.surfaceVariant,
				shape = RoundedCornerShape(CornerRadius.Small)
			)
			.padding(Spacing.XXS)
	) {
		SegmentButton(
			label = "近7天",
			selected = selected == InsightWindowType.Seven,
			onClick = { onSelectWindow(InsightWindowType.Seven) },
			modifier = Modifier.weight(1f)
		)
		SegmentButton(
			label = "近30天",
			selected = selected == InsightWindowType.Thirty,
			onClick = { onSelectWindow(InsightWindowType.Thirty) },
			modifier = Modifier.weight(1f)
		)
	}
}

@Composable
private fun SegmentButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
	Box(
		modifier = modifier
			.clip(RoundedCornerShape(CornerRadius.Small))
			.background(
				if (selected) Brush.horizontalGradient(
					colors = listOf(
						MaterialTheme.colorScheme.primary,
						MaterialTheme.colorScheme.tertiary
					)
				) else Brush.horizontalGradient(
					colors = listOf(
						Color.Transparent,
						Color.Transparent
					)
				)
			)
			.clickable(onClick = onClick)
			.padding(vertical = Spacing.S, horizontal = Spacing.M),
		contentAlignment = Alignment.Center
	) {
		Text(
			text = label,
			style = MaterialTheme.typography.labelLarge,
			fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
			color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
		)
	}
}

@Composable
private fun LoadingCard() {
	Card(
		modifier = Modifier.fillMaxWidth(),
		elevation = CardDefaults.cardElevation(defaultElevation = Elevation.None)
	) {
		Row(
			modifier = Modifier
				.padding(Spacing.M)
				.fillMaxWidth(),
			horizontalArrangement = Arrangement.spacedBy(Spacing.S),
			verticalAlignment = Alignment.CenterVertically
		) {
			CircularProgressIndicator(modifier = Modifier.height(24.dp))
			Text("正在加载洞察…", style = MaterialTheme.typography.bodyMedium)
		}
	}
}

@Composable
private fun ErrorCard(message: String) {
	Card(
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
		elevation = CardDefaults.cardElevation(defaultElevation = Elevation.None)
	) {
		Column(modifier = Modifier.padding(Spacing.M), verticalArrangement = Arrangement.spacedBy(Spacing.XS)) {
			Text(text = "加载失败", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
			Text(text = message, color = MaterialTheme.colorScheme.onErrorContainer)
		}
	}
}

@Composable
private fun EmptyState() {
	Card(
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
		),
		elevation = CardDefaults.cardElevation(defaultElevation = Elevation.None)
	) {
		Column(modifier = Modifier.padding(Spacing.M), verticalArrangement = Arrangement.spacedBy(Spacing.XS)) {
			Text(
				text = "暂无数据",
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.SemiBold
			)
			Text(
				text = "完成每日填报后，这里会出现你的习惯分布和趋势。",
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
	}
}

@Composable
private fun CompletionCard(window: InsightWindow) {
	Card(
		colors = CardDefaults.cardColors(
			containerColor = MaterialTheme.colorScheme.surface
		),
		elevation = CardDefaults.cardElevation(defaultElevation = Elevation.None),
		modifier = Modifier.fillMaxWidth()
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(Spacing.M),
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			Column(verticalArrangement = Arrangement.spacedBy(Spacing.XS), modifier = Modifier.weight(1f)) {
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
private fun InsightGrid(window: InsightWindow) {
	Column(verticalArrangement = Arrangement.spacedBy(Spacing.S)) {
		SleepQualityCard(window)
		MoodDistributionCard(window)
		SymptomGrid(metrics = window.symptomMetrics)
	}
}

@Composable
private fun SleepQualityCard(window: InsightWindow) {
	val buckets = listOf(
		SleepSegment(label = "≥8h", key = ">8", midpoint = 8.5f, color = MaterialTheme.colorScheme.success),
		SleepSegment(label = "7-8h", key = "7-8", midpoint = 7.5f, color = MaterialTheme.colorScheme.primary),
		SleepSegment(label = "6-7h", key = "6-7", midpoint = 6.5f, color = MaterialTheme.colorScheme.warning),
		SleepSegment(label = "<6h", key = "<6", midpoint = 5f, color = MaterialTheme.colorScheme.semanticError)
	).map { segment -> segment.copy(value = window.sleepDistribution[segment.key] ?: 0) }

	val total = buckets.sumOf { it.value }.coerceAtLeast(1)
	val averageHours = buckets.sumOf { it.midpoint.toDouble() * it.value } / total
	val sleepScore = buckets.sumOf { bucketWeight(it) * it.value }.toDouble() / total

	Card(
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
		elevation = CardDefaults.cardElevation(defaultElevation = Elevation.None),
		modifier = Modifier.fillMaxWidth()
	) {
		Column(modifier = Modifier.padding(Spacing.M), verticalArrangement = Arrangement.spacedBy(Spacing.S)) {
			Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
				Text(text = "睡眠质量", style = MaterialTheme.typography.titleMedium)
				Text(text = "平均 ${formatHours(averageHours)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
			}
			Row(horizontalArrangement = Arrangement.spacedBy(Spacing.M), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
				RingChart(segments = buckets, centerLabel = sleepScore.roundToInt().coerceIn(0, 100).toString(), centerCaption = "分数", modifier = Modifier.size(160.dp))
				Column(verticalArrangement = Arrangement.spacedBy(Spacing.XS), modifier = Modifier.weight(1f)) {
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
private fun MoodDistributionCard(window: InsightWindow) {
	val irritability = window.symptomMetrics.firstOrNull { it.questionId == "mood_irritability" }?.average ?: 5.0
	val happy = (100 - irritability * 8).toInt().coerceIn(20, 90)
	val calm = (70 - (irritability - 5) * 6).toInt().coerceIn(10, 80)
	val tired = (100 - happy - calm).coerceIn(5, 60)
	val rows = listOf(
		MoodRow("愉快 (Happy)", happy, MaterialTheme.colorScheme.warning),
		MoodRow("平静 (Calm)", calm, MaterialTheme.colorScheme.primary),
		MoodRow("疲劳 (Tired)", tired, MaterialTheme.colorScheme.onSurfaceVariant)
	)

	Card(
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
		elevation = CardDefaults.cardElevation(defaultElevation = Elevation.None)
	) {
		Column(modifier = Modifier.padding(Spacing.M), verticalArrangement = Arrangement.spacedBy(Spacing.S)) {
			Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
				Text(text = "情绪分布", style = MaterialTheme.typography.titleMedium)
				Text(text = "主要情绪: 愉快", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
			}
			rows.forEach { row ->
				MoodBar(row)
			}
		}
	}
}

@Composable
private fun SymptomGrid(metrics: List<InsightSymptomMetric>) {
	if (metrics.isEmpty()) return
	val topMetrics = metrics.take(4)
	OutlinedCard(
		colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
		border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
	) {
		Column(modifier = Modifier.padding(Spacing.M), verticalArrangement = Arrangement.spacedBy(Spacing.S)) {
			Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
				Text(text = "身体症状监测", style = MaterialTheme.typography.titleMedium)
				TextButton(onClick = { /* reserved for navigation */ }) { Text("查看详情") }
			}
			Row(horizontalArrangement = Arrangement.spacedBy(Spacing.S), modifier = Modifier.fillMaxWidth()) {
				topMetrics.forEach { metric ->
					SymptomPill(metric = metric, modifier = Modifier.weight(1f))
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
private fun RingChart(segments: List<SleepSegment>, centerLabel: String, centerCaption: String, modifier: Modifier = Modifier) {
	val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
	Box(contentAlignment = Alignment.Center, modifier = modifier) {
		Canvas(modifier = Modifier.fillMaxSize()) {
			val safeSegments = if (segments.all { it.value == 0 }) segments.map { it.copy(value = 1) } else segments
			val total = safeSegments.sumOf { it.value }.coerceAtLeast(1)
			val strokeWidth = 18.dp.toPx()
			drawArc(
				color = surfaceVariant.copy(alpha = 0.6f),
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
		Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.XS)) {
			Box(modifier = Modifier.size(10.dp).background(color, shape = RoundedCornerShape(50)))
			Text(text = label, style = MaterialTheme.typography.bodyMedium)
		}
		Text(text = "$percent%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
	}
}

@Composable
private fun MoodBar(row: MoodRow) {
	Column(verticalArrangement = Arrangement.spacedBy(Spacing.XS)) {
		Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
			Text(text = row.label, style = MaterialTheme.typography.bodyMedium)
			Text(text = "${row.percent}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
		}
		LinearProgressIndicator(
			progress = { row.percent / 100f },
			modifier = Modifier.fillMaxWidth(),
			color = row.color,
			trackColor = MaterialTheme.colorScheme.surfaceVariant
		)
	}
}

@Composable
private fun SymptomPill(metric: InsightSymptomMetric, modifier: Modifier = Modifier) {
	val riskColor = when {
		metric.average >= 7 -> MaterialTheme.colorScheme.semanticError
		metric.average >= 4 -> MaterialTheme.colorScheme.warning
		else -> MaterialTheme.colorScheme.success
	}
	val status = when {
		metric.average >= 7 -> "高风险"
		metric.average >= 4 -> "需注意"
		else -> "低风险"
	}
	Card(
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
		modifier = modifier
	) {
		Column(modifier = Modifier.padding(Spacing.XS), verticalArrangement = Arrangement.spacedBy(Spacing.XS)) {
			Text(text = symptomLabel(metric.questionId), style = MaterialTheme.typography.bodyMedium)
			Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
				Text(text = status, color = riskColor, style = MaterialTheme.typography.labelMedium)
				TrendIcon(metric.trend)
			}
		}
	}
}

@Composable
private fun SymptomCard(metrics: List<InsightSymptomMetric>) {
	Card(
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
		elevation = CardDefaults.cardElevation(defaultElevation = Elevation.None)
	) {
		Column(modifier = Modifier.padding(Spacing.M), verticalArrangement = Arrangement.spacedBy(Spacing.XS)) {
			Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.XS)) {
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
	val hasPayload = weekly.result?.payload != null

	val clickableModifier = Modifier.clickable { onOpen() }
	Card(
		shape = RoundedCornerShape(CornerRadius.Medium),
		modifier = Modifier
			.fillMaxWidth()
			.then(clickableModifier),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
		elevation = CardDefaults.cardElevation(defaultElevation = Elevation.None)
	) {
		Box(
			modifier = Modifier
				.padding(Spacing.M)
		) {
			Column(verticalArrangement = Arrangement.spacedBy(Spacing.S), modifier = Modifier.fillMaxWidth()) {
				Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.S)) {
					Icon(imageVector = Icons.Outlined.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp))
					Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.XXS)) {
						Text(text = "AI 洞察建议", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
						Text(text = "每周会生成 AI 洞察建议，点击查看", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = if (hasPayload) 1f else 0.7f))
					}
					if (weekly.status == WeeklyInsightStatus.Error) {
						Button(
							onClick = onRetry,
							colors = ButtonDefaults.buttonColors(
								containerColor = MaterialTheme.colorScheme.primary,
								contentColor = MaterialTheme.colorScheme.onPrimary
							)
						) {
							Text("重试")
						}
					}
				}
				weekly.result?.payload?.let { payload ->
					Column(verticalArrangement = Arrangement.spacedBy(Spacing.XS)) {
						Text(text = payload.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
						Text(text = "点击查看完整建议 ›", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f))
					}
				}
			}
		}
	}
}

@Composable
private fun DoctorReportCard(
	isGenerating: Boolean,
	errorMessage: String?,
	startDate: LocalDate?,
	endDate: LocalDate?,
	onGenerate: () -> Unit,
	onDismissStatus: () -> Unit,
	onDateRangeChange: (LocalDate?, LocalDate?) -> Unit
) {
	var showDatePicker by remember { mutableStateOf(false) }
	val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
	
	Card(
		shape = RoundedCornerShape(CornerRadius.Medium),
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
		elevation = CardDefaults.cardElevation(defaultElevation = Elevation.Low)
	) {
		Column(
			modifier = Modifier.padding(Spacing.M),
			verticalArrangement = Arrangement.spacedBy(Spacing.S)
		) {
			Row(
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.spacedBy(Spacing.S)
			) {
				Icon(
					imageVector = Icons.Outlined.PictureAsPdf,
					contentDescription = null,
					tint = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.size(28.dp)
				)
				Column(modifier = Modifier.weight(1f)) {
					Text(
						text = "医生报表",
						style = MaterialTheme.typography.titleMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
					Text(
						text = "生成可供医生参考的PDF健康报表",
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
					)
				}
			}

			// 日期范围选择器
			OutlinedCard(
				modifier = Modifier.fillMaxWidth(),
				onClick = { showDatePicker = !showDatePicker },
				colors = CardDefaults.outlinedCardColors(
					containerColor = MaterialTheme.colorScheme.surface
				)
			) {
				Row(
					modifier = Modifier.padding(Spacing.S),
					verticalAlignment = Alignment.CenterVertically,
					horizontalArrangement = Arrangement.spacedBy(Spacing.XS)
				) {
					Icon(
						imageVector = Icons.Outlined.DateRange,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.primary,
						modifier = Modifier.size(20.dp)
					)
					Text(
						text = if (startDate != null && endDate != null) {
							"${startDate.format(dateFormatter)} 至 ${endDate.format(dateFormatter)}"
						} else {
							"点击选择日期范围"
						},
						style = MaterialTheme.typography.bodyMedium,
						color = MaterialTheme.colorScheme.onSurface
					)
				}
			}

			// 简易日期选择器（展开式）
			if (showDatePicker) {
				val today = LocalDate.now()
				Column(
					modifier = Modifier
						.fillMaxWidth()
						.background(
							MaterialTheme.colorScheme.surface,
							RoundedCornerShape(CornerRadius.Small)
						)
						.padding(Spacing.S),
					verticalArrangement = Arrangement.spacedBy(Spacing.XS)
				) {
					Text(
						text = "快速选择",
						style = MaterialTheme.typography.labelMedium,
						color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
					)
					Row(
						horizontalArrangement = Arrangement.spacedBy(Spacing.XS),
						modifier = Modifier.fillMaxWidth()
					) {
						FilterChip(
							selected = false,
							onClick = {
								onDateRangeChange(today.minusDays(6), today)
								showDatePicker = false
							},
							label = { Text("最近7天") }
						)
						FilterChip(
							selected = false,
							onClick = {
								onDateRangeChange(today.minusDays(29), today)
								showDatePicker = false
							},
							label = { Text("最近30天") }
						)
						FilterChip(
							selected = false,
							onClick = {
								onDateRangeChange(today.minusDays(89), today)
								showDatePicker = false
							},
							label = { Text("最近3个月") }
						)
					}
					Row(
						horizontalArrangement = Arrangement.spacedBy(Spacing.XS),
						modifier = Modifier.fillMaxWidth()
					) {
						FilterChip(
							selected = false,
							onClick = {
								onDateRangeChange(today.minusYears(1), today)
								showDatePicker = false
							},
							label = { Text("最近1年") }
						)
						if (startDate != null || endDate != null) {
							FilterChip(
								selected = false,
								onClick = {
									onDateRangeChange(null, null)
									showDatePicker = false
								},
								label = { Text("清除") },
								colors = FilterChipDefaults.filterChipColors(
									selectedContainerColor = MaterialTheme.colorScheme.errorContainer
								)
							)
						}
					}
				}
			}

			// 状态提示
			when {
				isGenerating -> {
					Row(
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.spacedBy(Spacing.S),
						modifier = Modifier
							.fillMaxWidth()
							.background(
								MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
								RoundedCornerShape(CornerRadius.Small)
							)
							.padding(Spacing.S)
					) {
						CircularProgressIndicator(
							modifier = Modifier.size(16.dp),
							strokeWidth = 2.dp
						)
						Text(
							text = "正在生成预览...",
							style = MaterialTheme.typography.bodySmall,
							color = MaterialTheme.colorScheme.onSurfaceVariant
						)
					}
				}
				errorMessage != null -> {
					Row(
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.SpaceBetween,
						modifier = Modifier
							.fillMaxWidth()
							.background(
							MaterialTheme.colorScheme.semanticError.copy(alpha = 0.15f),
								RoundedCornerShape(CornerRadius.Small)
							)
							.padding(Spacing.S)
					) {
						Text(
							text = "生成失败：$errorMessage",
							style = MaterialTheme.typography.bodySmall,
							color = MaterialTheme.colorScheme.semanticError,
							modifier = Modifier.weight(1f)
						)
						IconButton(onClick = onDismissStatus, modifier = Modifier.size(20.dp)) {
							Icon(
								imageVector = Icons.Filled.Close,
								contentDescription = "关闭",
								tint = MaterialTheme.colorScheme.semanticError,
								modifier = Modifier.size(16.dp)
							)
						}
					}
				}
			}

			// 生成按钮
			Button(
				onClick = onGenerate,
				enabled = !isGenerating,
				modifier = Modifier.fillMaxWidth(),
				colors = ButtonDefaults.buttonColors(
					containerColor = MaterialTheme.colorScheme.primary,
					contentColor = MaterialTheme.colorScheme.onPrimary
				)
			) {
				Icon(
					imageVector = Icons.Outlined.PictureAsPdf,
					contentDescription = null,
					modifier = Modifier.size(18.dp)
				)
				Spacer(modifier = Modifier.size(Spacing.XS))
				Text(
					text = if (isGenerating) "生成中..." else "生成报表预览",
					style = MaterialTheme.typography.labelLarge
				)
			}

			// 免责说明
			Text(
				text = "本报告仅供医生参考，不构成医疗诊断依据",
				style = MaterialTheme.typography.labelSmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
				modifier = Modifier.padding(top = Spacing.XXS)
			)
		}
	}
}


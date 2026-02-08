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
import androidx.compose.foundation.layout.width
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
import com.heldairy.ui.theme.KittyBackground
import com.heldairy.ui.theme.BackgroundTheme
import com.heldairy.ui.theme.StickerDecoration
import com.heldairy.R
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
					Text(text = stringResource(R.string.insights_ai_advice), style = MaterialTheme.typography.titleMedium)
					IconButton(onClick = onClose) {
						Icon(imageVector = Icons.Default.Close, contentDescription = "关闭")
					}
				}
				Spacer(modifier = Modifier.height(Spacing.S))
				val payload = weekly.result?.payload
				when {
					payload != null -> {
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
							if (payload.suggestions.isNotEmpty()) {
								Text(text = "建议", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
								payload.suggestions.forEach { suggestion ->
									Row(horizontalArrangement = Arrangement.spacedBy(Spacing.XS), verticalAlignment = Alignment.CenterVertically) {
										Icon(imageVector = Icons.Outlined.TipsAndUpdates, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
										Text(text = suggestion, style = MaterialTheme.typography.bodyMedium)
									}
								}
							}
							if (payload.cautions.isNotEmpty()) {
								Text(text = "注意事项", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
								payload.cautions.forEach { caution ->
									Row(horizontalArrangement = Arrangement.spacedBy(Spacing.XS), verticalAlignment = Alignment.CenterVertically) {
										Icon(imageVector = Icons.Outlined.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
										Text(text = caution, style = MaterialTheme.typography.bodyMedium)
									}
								}
							}
						}
					}
					weekly.status == WeeklyInsightStatus.Pending -> {
						Box(modifier = Modifier.fillMaxWidth().padding(Spacing.L), contentAlignment = Alignment.Center) {
							Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.M)) {
								CircularProgressIndicator()
								Text("正在生成 AI 洞察...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
							}
						}
					}
					weekly.status == WeeklyInsightStatus.Disabled -> {
						Text(text = "AI 功能已禁用，请在设置中开启", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
					}
					weekly.status == WeeklyInsightStatus.Error -> {
						Column(verticalArrangement = Arrangement.spacedBy(Spacing.S)) {
							Text(text = weekly.result?.message ?: "生成失败", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
							Text(text = "请检查网络连接和 API Key 设置", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
						}
					}
					weekly.status == WeeklyInsightStatus.NoData -> {
						Column(
							modifier = Modifier.fillMaxWidth().padding(Spacing.L),
							horizontalAlignment = Alignment.CenterHorizontally,
							verticalArrangement = Arrangement.spacedBy(Spacing.M)
						) {
							Text(
								text = "🌱",
								style = MaterialTheme.typography.displayMedium
							)
							Text(
								text = weekly.result?.message ?: "等待你填写更多日报，就会生成 AI 洞察哦！",
								style = MaterialTheme.typography.bodyMedium,
								color = MaterialTheme.colorScheme.onSurfaceVariant,
								textAlign = TextAlign.Center
							)
						}
					}
					else -> {
						Text(text = "暂时没有可展示的 AI 建议。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
					}
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
		KittyBackground(backgroundRes = BackgroundTheme.INSIGHTS) {
			InsightsScreen(
				state = state,
				onSelectWindow = viewModel::selectWindow,
				onGeneratePreview = { viewModel.generatePreview() },
				onClearReportStatus = viewModel::clearReportStatus,
				onSetReportDateRange = viewModel::setReportDateRange,
				modifier = modifier.padding(paddingValues)
			)
		}
	}
}

@Composable
fun InsightsScreen(
	state: InsightsUiState,
	onSelectWindow: (InsightWindowType) -> Unit,
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
			Text(text = stringResource(R.string.insights_error_loading), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
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
		Column(
			modifier = Modifier.padding(Spacing.M).fillMaxWidth(),
			verticalArrangement = Arrangement.spacedBy(Spacing.S),
			horizontalAlignment = Alignment.CenterHorizontally
		) {
			StickerDecoration(
				drawableRes = R.drawable.kitty01,
				size = 72.dp,
				rotation = 0f,
				alpha = 0.6f
			)
			Text(
				text = stringResource(R.string.insights_empty_title),
				style = MaterialTheme.typography.titleMedium,
				fontWeight = FontWeight.SemiBold
			)
			Text(
				text = stringResource(R.string.insights_empty_message),
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
	}
}

@Composable
private fun CompletionCard(window: InsightWindow) {
	Box(modifier = Modifier.fillMaxWidth()) {
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
					Text(text = stringResource(R.string.insights_progress_title), style = MaterialTheme.typography.titleMedium)
					Text(text = stringResource(R.string.insights_progress_days, window.entryCount, window.days), color = MaterialTheme.colorScheme.onSurfaceVariant)
					LinearProgressIndicator(
						progress = { (window.entryCount.toFloat() / window.days.toFloat()).coerceIn(0f, 1f) },
						modifier = Modifier.fillMaxWidth()
					)
				}
				Icon(imageVector = Icons.Outlined.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
			}
	}
	StickerDecoration(
		drawableRes = R.drawable.bow,
		size = 44.dp,
		rotation = 20f,
		alpha = 0.5f,
		modifier = Modifier
			.align(Alignment.TopEnd)
			.offset(x = 16.dp, y = (-16).dp)
	)
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
	// Keys must match InsightCalculator: "lt6", "6_7", "7_8", "gt8"
	val buckets = listOf(
		SleepSegment(label = "≥8h", key = "gt8", midpoint = 8.5f, color = MaterialTheme.colorScheme.primary),
		SleepSegment(label = "7-8h", key = "7_8", midpoint = 7.5f, color = MaterialTheme.colorScheme.secondary),
		SleepSegment(label = "6-7h", key = "6_7", midpoint = 6.5f, color = MaterialTheme.colorScheme.tertiary),
		SleepSegment(label = "<6h", key = "lt6", midpoint = 5f, color = MaterialTheme.colorScheme.semanticError)
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

// Keys must match: "gt8", "7_8", "6_7", "lt6"
private fun bucketWeight(segment: SleepSegment): Int = when (segment.key) {
	"gt8" -> 95
	"7_8" -> 90
	"6_7" -> 75
	"lt6" -> 55
	else -> 50
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
private fun WeeklyInsightCard(
	weekly: WeeklyInsightUi, 
	onOpen: () -> Unit
) {
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
					StickerDecoration(
						drawableRes = R.drawable.kitty02,
						size = 52.dp,
						rotation = 0f,
						alpha = 1f
					)
					Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.XXS)) {
						Text(text = stringResource(R.string.insights_ai_advice), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
						Text(text = "每周会生成 AI 洞察建议，点击查看", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = if (hasPayload) 1f else 0.7f))
					}
				}
				weekly.result?.payload?.let { payload ->
					Column(verticalArrangement = Arrangement.spacedBy(Spacing.XS)) {
						Text(text = payload.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
						Text(text = "点击查看完整建议 ›", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f))
					}
				}
				
				// 自动生成逻辑：
				// - 周日首次打开 → 自动调用LLM生成本周洞察
				// - 非周日 → 显示上周的数据
				// - 数据为空/失败 → 自动重新生成
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
	
	Box(modifier = Modifier.fillMaxWidth()) {
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
	StickerDecoration(
		drawableRes = R.drawable.strawberry,
		size = 48.dp,
		rotation = -15f,
		alpha = 0.5f,
		modifier = Modifier
			.align(Alignment.TopEnd)
			.offset(x = 18.dp, y = (-18).dp)
	)
	}
}


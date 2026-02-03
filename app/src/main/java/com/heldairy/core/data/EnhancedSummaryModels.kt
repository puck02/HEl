package com.heldairy.core.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 增强型周总结 - 包含趋势分析、异常检测、改善信号
 * 阶段1优化：为AI提供更深入的上下文
 */
@Serializable
data class EnhancedWeeklySummary(
    @SerialName("basic_metrics") val basicMetrics: SummaryWindow,
    @SerialName("trend_analysis") val trendAnalysis: Map<String, DetailedTrend>,
    @SerialName("anomalies") val anomalies: List<AnomalyEvent>,
    @SerialName("improvements") val improvements: List<String>,
    @SerialName("concern_patterns") val concernPatterns: List<String>,
    @SerialName("week_over_week") val weekOverWeekChange: Map<String, Float>?
)

/**
 * 详细趋势 - 不仅是方向，还包括变化幅度和置信度
 */
@Serializable
data class DetailedTrend(
    val direction: TrendDirection,
    @SerialName("magnitude") val magnitude: Float,  // 变化幅度百分比
    @SerialName("confidence") val confidence: TrendConfidence,
    @SerialName("description") val description: String  // 人类可读描述
)

@Serializable
enum class TrendDirection {
    @SerialName("improving") IMPROVING,
    @SerialName("stable") STABLE,
    @SerialName("declining") DECLINING
}

@Serializable
enum class TrendConfidence {
    @SerialName("high") HIGH,      // 连续3天以上同方向
    @SerialName("medium") MEDIUM,  // 2-3天
    @SerialName("low") LOW         // 不足2天
}

/**
 * 异常事件 - 标记需要关注的异常数据点
 */
@Serializable
data class AnomalyEvent(
    @SerialName("date") val date: String,
    @SerialName("metric") val metric: String,
    @SerialName("value") val value: Double,
    @SerialName("expected_range") val expectedRange: String,  // 如 "6.5-8.5h"
    @SerialName("severity") val severity: AnomalySeverity,
    @SerialName("description") val description: String
)

@Serializable
enum class AnomalySeverity {
    @SerialName("mild") MILD,        // 1-2个标准差
    @SerialName("moderate") MODERATE, // 2-3个标准差
    @SerialName("severe") SEVERE      // 3+个标准差
}

/**
 * 扩展现有DailySummaryPayload支持增强摘要
 */
@Serializable
data class EnhancedDailySummaryPayload(
    @SerialName("window_7") val window7: SummaryWindow?,
    @SerialName("window_30") val window30: SummaryWindow?,
    @SerialName("enhanced_7") val enhanced7: EnhancedWeeklySummary?
)

package com.heldairy.core.data

import com.heldairy.core.database.entity.DailyEntryWithResponses
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 增强分析器 - 阶段1：计算趋势、异常、改善信号
 */
object EnhancedAnalyzer {
    
    /**
     * 构建增强型7天摘要
     */
    fun buildEnhancedWeeklySummary(
        entries: List<DailyEntryWithResponses>,
        endDate: LocalDate
    ): EnhancedWeeklySummary? {
        val startDate = endDate.minusDays(6)
        val indexed = entries.mapNotNull { snapshot ->
            runCatching { LocalDate.parse(snapshot.entry.entryDate) to snapshot }.getOrNull()
        }.filter { (date, _) -> date in startDate..endDate }
            .sortedBy { it.first }  // 按时间正序
        
        if (indexed.size < 3) return null  // 至少需要3天数据才能分析趋势
        
        val basicMetrics = buildBasicMetrics(indexed)
        val trendAnalysis = analyzeTrends(indexed)
        val anomalies = detectAnomalies(indexed)
        val improvements = identifyImprovements(trendAnalysis)
        val concernPatterns = identifyConcerns(trendAnalysis, anomalies)
        val weekOverWeek = calculateWeekOverWeek(entries, endDate)
        
        return EnhancedWeeklySummary(
            basicMetrics = basicMetrics,
            trendAnalysis = trendAnalysis,
            anomalies = anomalies,
            improvements = improvements,
            concernPatterns = concernPatterns,
            weekOverWeekChange = weekOverWeek
        )
    }
    
    private fun buildBasicMetrics(indexed: List<Pair<LocalDate, DailyEntryWithResponses>>): SummaryWindow {
        val metrics = TRACKED_METRICS.mapNotNull { questionId ->
            val values = indexed.mapNotNull { (_, entry) ->
                entry.responses.firstOrNull { it.questionId == questionId }
                    ?.answerValue
                    ?.toDoubleOrNull()
            }
            if (values.isEmpty()) return@mapNotNull null
            
            val average = values.average()
            val latest = values.lastOrNull()
            val highCount = values.count { it >= HIGH_THRESHOLD }
            val trend = computeSimpleTrend(values)
            
            SummaryMetric(
                questionId = questionId,
                average = average,
                latestValue = latest,
                highCount = highCount,
                trend = trend
            )
        }
        
        return SummaryWindow(
            days = 7,
            entryCount = indexed.size,
            metrics = metrics
        )
    }
    
    private fun analyzeTrends(indexed: List<Pair<LocalDate, DailyEntryWithResponses>>): Map<String, DetailedTrend> {
        return TRACKED_METRICS.mapNotNull { questionId ->
            val values = indexed.mapNotNull { (_, entry) ->
                entry.responses.firstOrNull { it.questionId == questionId }
                    ?.answerValue
                    ?.toDoubleOrNull()
            }
            if (values.size < 2) return@mapNotNull null
            
            // 比较前3天和后3天
            val first3 = values.take(minOf(3, values.size / 2))
            val last3 = values.takeLast(minOf(3, values.size / 2))
            
            if (first3.isEmpty() || last3.isEmpty()) return@mapNotNull null
            
            val avgFirst = first3.average()
            val avgLast = last3.average()
            val change = avgLast - avgFirst
            val magnitude = if (avgFirst != 0.0) (change / avgFirst * 100).toFloat() else 0f
            
            val direction = when {
                change > 1.0 -> if (questionId in INVERSE_METRICS) TrendDirection.DECLINING else TrendDirection.IMPROVING
                change < -1.0 -> if (questionId in INVERSE_METRICS) TrendDirection.IMPROVING else TrendDirection.DECLINING
                else -> TrendDirection.STABLE
            }
            
            val confidence = when {
                values.size >= 5 && isConsistentTrend(values) -> TrendConfidence.HIGH
                values.size >= 3 -> TrendConfidence.MEDIUM
                else -> TrendConfidence.LOW
            }
            
            val description = generateTrendDescription(questionId, direction, magnitude)
            
            questionId to DetailedTrend(
                direction = direction,
                magnitude = abs(magnitude),
                confidence = confidence,
                description = description
            )
        }.toMap()
    }
    
    private fun detectAnomalies(indexed: List<Pair<LocalDate, DailyEntryWithResponses>>): List<AnomalyEvent> {
        val anomalies = mutableListOf<AnomalyEvent>()
        
        TRACKED_METRICS.forEach { questionId ->
            val dataPoints = indexed.mapNotNull { (date, entry) ->
                val value = entry.responses.firstOrNull { it.questionId == questionId }
                    ?.answerValue
                    ?.toDoubleOrNull()
                value?.let { date to it }
            }
            
            if (dataPoints.size < 3) return@forEach
            
            val values = dataPoints.map { it.second }
            val mean = values.average()
            val stdDev = sqrt(values.map { (it - mean).pow(2) }.average())
            
            if (stdDev < 0.1) return@forEach  // 变化太小，不检测异常
            
            dataPoints.forEach { (date, value) ->
                val zScore = abs((value - mean) / stdDev)
                val severity = when {
                    zScore >= 3.0 -> AnomalySeverity.SEVERE
                    zScore >= 2.0 -> AnomalySeverity.MODERATE
                    zScore >= 1.5 -> AnomalySeverity.MILD
                    else -> null
                }
                
                if (severity != null) {
                    val expectedMin = (mean - stdDev).coerceAtLeast(0.0)
                    val expectedMax = mean + stdDev
                    anomalies.add(
                        AnomalyEvent(
                            date = date.toString(),
                            metric = questionId,
                            value = value,
                            expectedRange = "${expectedMin.format(1)}-${expectedMax.format(1)}",
                            severity = severity,
                            description = generateAnomalyDescription(questionId, value, mean, severity)
                        )
                    )
                }
            }
        }
        
        return anomalies.sortedByDescending { it.severity.ordinal }
    }
    
    private fun identifyImprovements(trends: Map<String, DetailedTrend>): List<String> {
        return trends.filter { (_, trend) ->
            trend.direction == TrendDirection.IMPROVING && 
            trend.confidence in listOf(TrendConfidence.HIGH, TrendConfidence.MEDIUM)
        }.map { (metric, trend) ->
            "${METRIC_NAMES[metric] ?: metric}${trend.description}"
        }
    }
    
    private fun identifyConcerns(
        trends: Map<String, DetailedTrend>,
        anomalies: List<AnomalyEvent>
    ): List<String> {
        val concerns = mutableListOf<String>()
        
        // 持续恶化趋势
        trends.filter { (_, trend) ->
            trend.direction == TrendDirection.DECLINING &&
            trend.confidence == TrendConfidence.HIGH &&
            trend.magnitude > 15f
        }.forEach { (metric, trend) ->
            concerns.add("${METRIC_NAMES[metric] ?: metric}持续恶化（${trend.magnitude.toInt()}%）")
        }
        
        // 严重异常
        anomalies.filter { it.severity == AnomalySeverity.SEVERE }
            .take(2)
            .forEach { anomaly ->
                concerns.add("${anomaly.date} ${METRIC_NAMES[anomaly.metric]}异常（${anomaly.value}）")
            }
        
        return concerns
    }
    
    private fun calculateWeekOverWeek(
        entries: List<DailyEntryWithResponses>,
        endDate: LocalDate
    ): Map<String, Float>? {
        val thisWeekStart = endDate.minusDays(6)
        val lastWeekStart = endDate.minusDays(13)
        val lastWeekEnd = endDate.minusDays(7)
        
        val indexed = entries.mapNotNull { snapshot ->
            runCatching { LocalDate.parse(snapshot.entry.entryDate) to snapshot }.getOrNull()
        }
        
        val thisWeek = indexed.filter { (date, _) -> date in thisWeekStart..endDate }
        val lastWeek = indexed.filter { (date, _) -> date in lastWeekStart..lastWeekEnd }
        
        if (thisWeek.size < 3 || lastWeek.size < 3) return null
        
        return TRACKED_METRICS.mapNotNull { questionId ->
            val thisValues = thisWeek.mapNotNull { (_, entry) ->
                entry.responses.firstOrNull { it.questionId == questionId }?.answerValue?.toDoubleOrNull()
            }
            val lastValues = lastWeek.mapNotNull { (_, entry) ->
                entry.responses.firstOrNull { it.questionId == questionId }?.answerValue?.toDoubleOrNull()
            }
            
            if (thisValues.isEmpty() || lastValues.isEmpty()) return@mapNotNull null
            
            val thisAvg = thisValues.average()
            val lastAvg = lastValues.average()
            val change = if (lastAvg != 0.0) ((thisAvg - lastAvg) / lastAvg * 100).toFloat() else 0f
            
            questionId to change
        }.toMap()
    }
    
    // ========== 辅助函数 ==========
    
    private fun isConsistentTrend(values: List<Double>): Boolean {
        if (values.size < 3) return false
        val diffs = values.zipWithNext { a, b -> b - a }
        val positiveCount = diffs.count { it > 0 }
        val negativeCount = diffs.count { it < 0 }
        // 至少70%同方向才算consistent
        return positiveCount.toDouble() / diffs.size >= 0.7 || 
               negativeCount.toDouble() / diffs.size >= 0.7
    }
    
    private fun computeSimpleTrend(values: List<Double>): TrendFlag {
        if (values.size < 2) return TrendFlag.stable
        val recent = values.last()
        val comparison = values.dropLast(1).average()
        val delta = recent - comparison
        return when {
            delta >= 1.0 -> TrendFlag.rising
            delta <= -1.0 -> TrendFlag.falling
            else -> TrendFlag.stable
        }
    }
    
    private fun generateTrendDescription(metric: String, direction: TrendDirection, magnitude: Float): String {
        val change = when {
            magnitude > 30 -> "显著"
            magnitude > 15 -> "明显"
            else -> "略有"
        }
        val verb = when (direction) {
            TrendDirection.IMPROVING -> "改善"
            TrendDirection.DECLINING -> "恶化"
            TrendDirection.STABLE -> "稳定"
        }
        return "$change$verb"
    }
    
    private fun generateAnomalyDescription(
        metric: String,
        value: Double,
        mean: Double,
        severity: AnomalySeverity
    ): String {
        val metricName = METRIC_NAMES[metric] ?: metric
        val diff = abs(value - mean)
        val direction = if (value > mean) "偏高" else "偏低"
        return "$metricName${direction}（差异${diff.format(1)}）"
    }
    
    private fun Double.format(decimals: Int): String = "%.${decimals}f".format(this)
    
    // ========== 常量配置 ==========
    
    private const val HIGH_THRESHOLD = 7.0  // 症状高值阈值
    
    private val TRACKED_METRICS = listOf(
        "headache_intensity",
        "neck_back_intensity",
        "stomach_intensity",
        "nasal_intensity",
        "knee_intensity",
        "mood_irritability",
        "mood_scale",
        "energy_level"
    )
    
    // 数值越高越不好的指标（如疼痛、烦躁）
    private val INVERSE_METRICS = setOf(
        "headache_intensity",
        "neck_back_intensity",
        "stomach_intensity",
        "nasal_intensity",
        "knee_intensity",
        "mood_irritability"
    )
    
    private val METRIC_NAMES = mapOf(
        "headache_intensity" to "头痛",
        "neck_back_intensity" to "颈背痛",
        "stomach_intensity" to "胃部不适",
        "nasal_intensity" to "鼻塞",
        "knee_intensity" to "膝盖痛",
        "mood_irritability" to "烦躁感",
        "mood_scale" to "心情",
        "energy_level" to "能量"
    )
}

package com.heldairy.core.data

import com.heldairy.core.database.entity.DailyEntryWithResponses

object DailySummaryCalculator {
    private val trackedMetrics = listOf(
        TrackedMetric("headache_intensity", highThreshold = 6),
        TrackedMetric("neck_back_intensity", highThreshold = 6),
        TrackedMetric("stomach_intensity", highThreshold = 6),
        TrackedMetric("nasal_intensity", highThreshold = 6),
        TrackedMetric("knee_intensity", highThreshold = 6),
        TrackedMetric("mood_irritability", highThreshold = 6)
    )

    fun buildPayload(entries: List<DailyEntryWithResponses>): DailySummaryPayload {
        val sorted = entries.sortedByDescending { it.entry.entryDate }
        return DailySummaryPayload(
            window7 = buildWindow(sorted.take(7), 7),
            window30 = buildWindow(sorted.take(30), 30)
        )
    }

    private fun buildWindow(entries: List<DailyEntryWithResponses>, days: Int): SummaryWindow? {
        if (entries.isEmpty()) return null
        val metrics = trackedMetrics.mapNotNull { metric ->
            val values = entries.mapNotNull { entry ->
                entry.responses.firstOrNull { it.questionId == metric.questionId }
                    ?.answerValue
                    ?.toDoubleOrNull()
            }
            if (values.isEmpty()) return@mapNotNull null
            val average = values.average().roundToOneDecimal()
            val latest = values.firstOrNull()?.roundToOneDecimal()
            val highCount = values.count { it >= metric.highThreshold }
            val trend = computeTrend(values)
            SummaryMetric(
                questionId = metric.questionId,
                average = average,
                latestValue = latest,
                highCount = highCount,
                trend = trend
            )
        }
        if (metrics.isEmpty()) return null
        return SummaryWindow(
            days = days,
            entryCount = entries.size,
            metrics = metrics
        )
    }

    private fun computeTrend(values: List<Double>): TrendFlag {
        if (values.size < 2) return TrendFlag.stable
        val recent = values.first()
        val comparison = values.drop(1).averageOrNull() ?: return TrendFlag.stable
        val delta = recent - comparison
        return when {
            delta >= 1.0 -> TrendFlag.rising
            delta <= -1.0 -> TrendFlag.falling
            else -> TrendFlag.stable
        }
    }

    private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()

    private fun Double.roundToOneDecimal(): Double = kotlin.math.round(this * 10.0) / 10.0

    private data class TrackedMetric(
        val questionId: String,
        val highThreshold: Int
    )
}

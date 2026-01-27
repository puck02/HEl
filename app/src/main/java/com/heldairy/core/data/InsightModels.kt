package com.heldairy.core.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InsightWindow(
    val days: Int,
    @SerialName("entries") val entryCount: Int,
    @SerialName("completion_rate") val completionRate: Double,
    @SerialName("sleep_distribution") val sleepDistribution: Map<String, Int>,
    @SerialName("nap_distribution") val napDistribution: Map<String, Int>,
    @SerialName("steps_distribution") val stepsDistribution: Map<String, Int>,
    @SerialName("symptom_metrics") val symptomMetrics: List<InsightSymptomMetric>,
    @SerialName("chill_days") val chillDays: Int,
    @SerialName("medication") val medication: MedicationAdherenceSummary,
    @SerialName("menstrual_counts") val menstrualCounts: Map<String, Int>
)

@Serializable
data class InsightSymptomMetric(
    @SerialName("question_id") val questionId: String,
    val average: Double,
    @SerialName("latest") val latestValue: Double?,
    val trend: TrendFlag
)

@Serializable
data class MedicationAdherenceSummary(
    @SerialName("on_time") val onTime: Int,
    val missed: Int,
    val na: Int
)

@Serializable
data class InsightLocalSummary(
    @SerialName("window_7") val window7: InsightWindow?,
    @SerialName("window_30") val window30: InsightWindow?
)

@Serializable
data class WeeklyInsightPayload(
    @SerialName("schema_version") val schemaVersion: Int = 1,
    @SerialName("week_start_date") val weekStartDate: String = "",
    @SerialName("week_end_date") val weekEndDate: String = "",
    val summary: String = "",
    val highlights: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val cautions: List<String> = emptyList(),
    val confidence: String = "medium"
) {
    fun normalized(): WeeklyInsightPayload {
        return copy(
            summary = summary.trim(),
            highlights = highlights.map { it.trim() }.filter { it.isNotEmpty() },
            suggestions = suggestions.map { it.trim() }.filter { it.isNotEmpty() },
            cautions = cautions.map { it.trim() }.filter { it.isNotEmpty() },
            confidence = confidence.trim().ifEmpty { "medium" },
            weekStartDate = weekStartDate.trim(),
            weekEndDate = weekEndDate.trim()
        )
    }

    fun validationErrors(): List<String> {
        val errors = mutableListOf<String>()
        if (summary.isBlank()) errors += "summary_empty"
        if (highlights.isEmpty()) errors += "highlights_empty"
        if (suggestions.isEmpty()) errors += "suggestions_empty"
        if (weekStartDate.isBlank() || weekEndDate.isBlank()) errors += "week_range_missing"
        if (confidence !in setOf("low", "medium", "high")) errors += "confidence_invalid"
        return errors
    }
}

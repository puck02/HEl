package com.heldairy.core.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SummaryMetric(
    @SerialName("question_id") val questionId: String,
    val average: Double,
    @SerialName("latest") val latestValue: Double?,
    @SerialName("high_count") val highCount: Int,
    val trend: TrendFlag
)

@Serializable
enum class TrendFlag { rising, falling, stable }

@Serializable
data class SummaryWindow(
    @SerialName("days") val days: Int,
    @SerialName("entries") val entryCount: Int,
    val metrics: List<SummaryMetric>
)

@Serializable
data class DailySummaryPayload(
    @SerialName("window_7") val window7: SummaryWindow?,
    @SerialName("window_30") val window30: SummaryWindow?
)

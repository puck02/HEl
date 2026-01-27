package com.heldairy.core.data

import com.heldairy.core.database.entity.DailyEntryWithResponses
import java.time.LocalDate
import kotlin.math.round

object InsightCalculator {
    private val symptomQuestionIds = listOf(
        "headache_intensity",
        "neck_back_intensity",
        "stomach_intensity",
        "nasal_intensity",
        "knee_intensity",
        "mood_irritability"
    )

    fun buildSummary(
        entries: List<DailyEntryWithResponses>,
        endDate: LocalDate = LocalDate.now()
    ): InsightLocalSummary {
        val indexed = entries.mapNotNull { snapshot ->
            runCatching { LocalDate.parse(snapshot.entry.entryDate) to snapshot }.getOrNull()
        }.sortedByDescending { it.first }

        return InsightLocalSummary(
            window7 = buildWindow(indexed, endDate, 7),
            window30 = buildWindow(indexed, endDate, 30)
        )
    }

    private fun buildWindow(
        entries: List<Pair<LocalDate, DailyEntryWithResponses>>,
        endDate: LocalDate,
        days: Int
    ): InsightWindow? {
        val startDate = endDate.minusDays((days - 1).toLong())
        val windowEntries = entries.filter { (date, _) -> date in startDate..endDate }
        if (windowEntries.isEmpty()) return null

        val entryCount = windowEntries.size
        val completionRate = (entryCount.toDouble() / days).roundToTwoDecimals()
        val sleep = countOptions(windowEntries, "sleep_duration", listOf("lt6", "6_7", "7_8", "gt8"))
        val nap = countOptions(windowEntries, "nap_duration", listOf("none", "lt30", "30_60", "gt60"))
        val steps = countOptions(windowEntries, "daily_steps", listOf("lt3k", "3_6k", "6_10k", "gt10k"))
        val symptomMetrics = buildSymptomMetrics(windowEntries)
        val chillDays = countOptionHits(windowEntries, "chill_exposure", "yes")
        val medication = buildMedication(windowEntries)
        val menstrual = countOptions(windowEntries, "menstrual_status", listOf("period", "non_period", "irregular"))

        return InsightWindow(
            days = days,
            entryCount = entryCount,
            completionRate = completionRate,
            sleepDistribution = sleep,
            napDistribution = nap,
            stepsDistribution = steps,
            symptomMetrics = symptomMetrics,
            chillDays = chillDays,
            medication = medication,
            menstrualCounts = menstrual
        )
    }

    private fun buildSymptomMetrics(
        entries: List<Pair<LocalDate, DailyEntryWithResponses>>
    ): List<InsightSymptomMetric> {
        return symptomQuestionIds.mapNotNull { questionId ->
            val values = entries.mapNotNull { (_, entry) ->
                entry.responses.firstOrNull { it.questionId == questionId }
                    ?.answerValue
                    ?.toDoubleOrNull()
            }
            if (values.isEmpty()) return@mapNotNull null
            val average = values.average().roundToOneDecimal()
            val latest = values.firstOrNull()?.roundToOneDecimal()
            val trend = computeTrend(values)
            InsightSymptomMetric(
                questionId = questionId,
                average = average,
                latestValue = latest,
                trend = trend
            )
        }
    }

    private fun buildMedication(
        entries: List<Pair<LocalDate, DailyEntryWithResponses>>
    ): MedicationAdherenceSummary {
        var onTime = 0
        var missed = 0
        var na = 0
        entries.forEach { (_, entry) ->
            val value = entry.responses.firstOrNull { it.questionId == "medication_adherence" }?.answerValue
            when (value) {
                "on_time" -> onTime++
                "missed" -> missed++
                "na" -> na++
            }
        }
        return MedicationAdherenceSummary(onTime = onTime, missed = missed, na = na)
    }

    private fun countOptions(
        entries: List<Pair<LocalDate, DailyEntryWithResponses>>,
        questionId: String,
        buckets: List<String>
    ): Map<String, Int> {
        val counts = buckets.associateWith { 0 }.toMutableMap()
        entries.forEach { (_, entry) ->
            val value = entry.responses.firstOrNull { it.questionId == questionId }?.answerValue
            if (value != null && value in buckets) {
                counts[value] = counts.getValue(value) + 1
            }
        }
        return counts
    }

    private fun countOptionHits(
        entries: List<Pair<LocalDate, DailyEntryWithResponses>>,
        questionId: String,
        targetValue: String
    ): Int {
        return entries.count { (_, entry) ->
            entry.responses.firstOrNull { it.questionId == questionId }?.answerValue == targetValue
        }
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

    private fun Double.roundToOneDecimal(): Double = round(this * 10.0) / 10.0

    private fun Double.roundToTwoDecimals(): Double = round(this * 100.0) / 100.0
}

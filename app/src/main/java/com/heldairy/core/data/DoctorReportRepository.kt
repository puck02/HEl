package com.heldairy.core.data

import com.heldairy.feature.medication.Med
import com.heldairy.feature.medication.MedicationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 医生报表数据汇总Repository
 * 负责收集和组织生成PDF报表所需的所有数据
 */
class DoctorReportRepository(
    private val insightRepository: InsightRepository,
    private val medicationRepository: MedicationRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * 生成医生报表数据
     * @param timeWindow 时间窗口（7或30天）
     * @param endDate 结束日期（默认今天）
     */
    suspend fun generateReportData(
        timeWindow: Int = 30,
        endDate: LocalDate = LocalDate.now(clock)
    ): DoctorReportData = withContext(ioDispatcher) {
        val startDate = endDate.minusDays((timeWindow - 1).toLong())
        generateReportDataWithDateRange(startDate, endDate)
    }

    /**
     * 生成医生报表数据（自定义日期范围）
     * @param startDate 开始日期
     * @param endDate 结束日期
     */
    suspend fun generateReportDataWithDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): DoctorReportData = withContext(ioDispatcher) {
        val timeWindow = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        
        val summary = insightRepository.buildLocalSummary(endDate)
        val window = if (timeWindow == 7) summary.window7 else summary.window30
        
        // 获取活跃用药列表
        val activeMeds = medicationRepository.getActiveMeds().first()
        
        val reportData = DoctorReportData(
            reportDate = endDate,
            timeWindow = timeWindow,
            patientInfo = buildPatientInfo(startDate, endDate),
            dataCompleteness = buildDataCompleteness(window),
            medicationSummary = buildMedicationSummary(activeMeds, window),
            symptomSummary = buildSymptomSummary(window),
            lifestyleSummary = buildLifestyleSummary(window),
            aiInsightsSummary = buildAiInsightsSummary()
        )
        
        reportData
    }

    private fun buildPatientInfo(startDate: LocalDate, endDate: LocalDate): PatientInfo {
        val now = LocalDateTime.now(clock)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        
        return PatientInfo(
            reportGeneratedAt = now.format(formatter),
            dataRangeStart = startDate.toString(),
            dataRangeEnd = endDate.toString()
        )
    }

    private fun buildDataCompleteness(window: InsightWindow?): DataCompleteness {
        return if (window != null) {
            DataCompleteness(
                filledDays = window.entryCount,
                totalDays = window.days,
                completionRate = window.completionRate
            )
        } else {
            DataCompleteness(
                filledDays = 0,
                totalDays = 7,
                completionRate = 0.0
            )
        }
    }

    private fun buildMedicationSummary(
        activeMeds: List<Med>,
        window: InsightWindow?
    ): MedicationSummaryForReport {
        val activeMedications = activeMeds.mapNotNull { med ->
            val activeCourse = med.currentCourse
            if (activeCourse != null && med.hasActiveCourse) {
                ActiveMedication(
                    name = med.name,
                    dosage = activeCourse.doseText,
                    frequency = activeCourse.frequencyText,
                    startDate = activeCourse.startDate.toString(),
                    timeHints = activeCourse.timeHints
                )
            } else null
        }
        
        val adherence = window?.medication ?: MedicationAdherenceSummary(0, 0, 0)
        
        return MedicationSummaryForReport(
            activeMedications = activeMedications,
            adherence = adherence
        )
    }

    private fun buildSymptomSummary(window: InsightWindow?): SymptomSummaryForReport {
        val metrics = window?.symptomMetrics?.map { metric ->
            SymptomMetricForReport(
                symptomName = getSymptomDisplayName(metric.questionId),
                questionId = metric.questionId,
                average = metric.average,
                latestValue = metric.latestValue,
                trend = metric.trend,
                trendDescription = getTrendDescription(metric.trend)
            )
        } ?: emptyList()
        
        return SymptomSummaryForReport(metrics = metrics)
    }

    private fun buildLifestyleSummary(window: InsightWindow?): LifestyleSummaryForReport {
        if (window == null) {
            return LifestyleSummaryForReport(
                sleepSummary = "无数据",
                napSummary = "无数据",
                stepsSummary = "无数据",
                chillExposureDays = 0,
                menstrualSummary = null
            )
        }
        
        return LifestyleSummaryForReport(
            sleepSummary = formatSleepDistribution(window.sleepDistribution, window.entryCount),
            napSummary = formatNapDistribution(window.napDistribution, window.entryCount),
            stepsSummary = formatStepsDistribution(window.stepsDistribution, window.entryCount),
            chillExposureDays = window.chillDays,
            menstrualSummary = formatMenstrualSummary(window.menstrualCounts)
        )
    }

    private suspend fun buildAiInsightsSummary(): AiInsightsSummaryForReport? {
        // 获取最近的周度洞察
        val latestInsight = insightRepository.latestInsight()
        if (latestInsight == null || latestInsight.status != "success") {
            return null
        }
        
        // 解析AI结果JSON
        val aiResult = try {
            kotlinx.serialization.json.Json.decodeFromString<WeeklyInsightPayload>(
                latestInsight.aiResultJson ?: return null
            )
        } catch (e: Exception) {
            return null
        }
        
        return AiInsightsSummaryForReport(
            weeklyInsights = aiResult.highlights.take(3),
            topSuggestions = aiResult.suggestions.take(3)
        )
    }

    // Helper functions
    private fun getSymptomDisplayName(questionId: String): String {
        return when (questionId) {
            "headache_intensity" -> "头痛"
            "neck_back_intensity" -> "颈肩腰"
            "stomach_intensity" -> "胃部不适"
            "nasal_intensity" -> "鼻咽不适"
            "knee_intensity" -> "膝盖不适"
            "mood_irritability" -> "情绪烦躁"
            else -> questionId
        }
    }

    private fun getTrendDescription(trend: TrendFlag): String {
        return when (trend) {
            TrendFlag.rising -> "↑ 上升"
            TrendFlag.falling -> "↓ 下降"
            TrendFlag.stable -> "→ 稳定"
        }
    }

    private fun formatSleepDistribution(distribution: Map<String, Int>, total: Int): String {
        val dominant = distribution.maxByOrNull { it.value } ?: return "无数据"
        val percentage = if (total > 0) (dominant.value * 100 / total) else 0
        val label = when (dominant.key) {
            "lt6" -> "少于6小时"
            "6_7" -> "6-7小时"
            "7_8" -> "7-8小时"
            "gt8" -> "多于8小时"
            else -> dominant.key
        }
        return "$label ($percentage%)"
    }

    private fun formatNapDistribution(distribution: Map<String, Int>, total: Int): String {
        val dominant = distribution.maxByOrNull { it.value } ?: return "无数据"
        val percentage = if (total > 0) (dominant.value * 100 / total) else 0
        val label = when (dominant.key) {
            "none" -> "无午睡"
            "lt30" -> "少于30分钟"
            "30_60" -> "30-60分钟"
            "gt60" -> "多于60分钟"
            else -> dominant.key
        }
        return "$label ($percentage%)"
    }

    private fun formatStepsDistribution(distribution: Map<String, Int>, total: Int): String {
        val dominant = distribution.maxByOrNull { it.value } ?: return "无数据"
        val percentage = if (total > 0) (dominant.value * 100 / total) else 0
        val label = when (dominant.key) {
            "lt3k" -> "少于3000步"
            "3_6k" -> "3000-6000步"
            "6_10k" -> "6000-10000步"
            "gt10k" -> "多于10000步"
            else -> dominant.key
        }
        return "$label ($percentage%)"
    }

    private fun formatMenstrualSummary(counts: Map<String, Int>): String? {
        val period = counts["period"] ?: 0
        val nonPeriod = counts["non_period"] ?: 0
        val irregular = counts["irregular"] ?: 0
        
        if (period + nonPeriod + irregular == 0) return null
        
        val parts = mutableListOf<String>()
        if (period > 0) parts.add("经期${period}天")
        if (nonPeriod > 0) parts.add("非经期${nonPeriod}天")
        if (irregular > 0) parts.add("异常${irregular}天")
        
        return parts.joinToString("，")
    }
}

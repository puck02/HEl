package com.heldairy.core.data

import java.time.LocalDate

/**
 * 医生报表数据模型 v4.0
 * 用于生成专业PDF健康洞察报告的结构化数据
 * 包含雷达图、饼图、色条等可视化所需的全部数据
 */
data class DoctorReportData(
    val reportDate: LocalDate,
    val timeWindow: Int, // 7 or 30 days
    val patientInfo: PatientInfo,
    val dataCompleteness: DataCompleteness,
    val medicationSummary: MedicationSummaryForReport,
    val symptomSummary: SymptomSummaryForReport,
    val lifestyleSummary: LifestyleSummaryForReport,
    val aiInsightsSummary: AiInsightsSummaryForReport?,
    // --- v4.0 新增字段 ---
    val overallFeelingDistribution: Map<String, Int> = emptyMap(),       // great/ok/unwell/awful → count
    val followUpSummary: Map<String, List<String>> = emptyMap(),         // 症状名 → ["钝痛压迫感×3", ...]
    val anomalies: List<AnomalyEvent> = emptyList(),
    val detailedTrends: Map<String, DetailedTrend> = emptyMap(),
    val weekOverWeekChange: Map<String, Float>? = null,                  // 仅7天窗口有效
    val improvements: List<String> = emptyList(),
    val concernPatterns: List<String> = emptyList(),
    val moodScaleAverage: Double? = null,
    val energyLevelAverage: Double? = null
)

data class PatientInfo(
    val reportGeneratedAt: String,
    val dataRangeStart: String,
    val dataRangeEnd: String,
    val disclaimer: String = "本报告由健康日记APP生成，仅供医生参考，不构成医疗诊断依据。"
)

data class DataCompleteness(
    val filledDays: Int,
    val totalDays: Int,
    val completionRate: Double
)

data class MedicationEventForReport(
    val date: String,
    val time: String,
    val description: String
)

data class MedicationSummaryForReport(
    val activeMedications: List<ActiveMedication>,
    val adherence: MedicationAdherenceSummary,
    val events: List<MedicationEventForReport> = emptyList()
)

data class ActiveMedication(
    val name: String,
    val dosage: String?,
    val frequency: String,
    val startDate: String,
    val timeHints: String?
)

data class SymptomSummaryForReport(
    val metrics: List<SymptomMetricForReport>
)

data class SymptomMetricForReport(
    val symptomName: String,
    val questionId: String,
    val average: Double,
    val latestValue: Double?,
    val trend: TrendFlag,
    val trendDescription: String,
    val highCount: Int = 0           // 高值(≥7)天数
)

data class LifestyleSummaryForReport(
    val sleepSummary: String,
    val napSummary: String,
    val stepsSummary: String,
    val chillExposureDays: Int,
    val menstrualSummary: String?,
    // 原始分布数据，用于绘制堆叠色条
    val sleepDistribution: Map<String, Int> = emptyMap(),
    val napDistribution: Map<String, Int> = emptyMap(),
    val stepsDistribution: Map<String, Int> = emptyMap()
)

data class AiInsightsSummaryForReport(
    val weeklyInsights: List<String>,
    val topSuggestions: List<String>,
    val summary: String = "",              // AI总评
    val cautions: List<String> = emptyList(), // 健康警示
    val confidence: String = "medium"      // HIGH / MEDIUM / LOW
)

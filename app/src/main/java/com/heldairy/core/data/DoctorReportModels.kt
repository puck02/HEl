package com.heldairy.core.data

import java.time.LocalDate

/**
 * 医生报表数据模型
 * 用于生成PDF医生报表的结构化数据
 */
data class DoctorReportData(
    val reportDate: LocalDate,
    val timeWindow: Int, // 7 or 30 days
    val patientInfo: PatientInfo,
    val dataCompleteness: DataCompleteness,
    val medicationSummary: MedicationSummaryForReport,
    val symptomSummary: SymptomSummaryForReport,
    val lifestyleSummary: LifestyleSummaryForReport,
    val aiInsightsSummary: AiInsightsSummaryForReport?
)

data class PatientInfo(
    val reportGeneratedAt: String, // e.g., "2026-02-03 14:30"
    val dataRangeStart: String, // e.g., "2026-01-27"
    val dataRangeEnd: String, // e.g., "2026-02-03"
    val disclaimer: String = "本报告由健康日记APP生成，仅供医生参考，不构成医疗诊断依据。"
)

data class DataCompleteness(
    val filledDays: Int,
    val totalDays: Int,
    val completionRate: Double
)

data class MedicationSummaryForReport(
    val activeMedications: List<ActiveMedication>,
    val adherence: MedicationAdherenceSummary
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
    val symptomName: String, // 中文名称，如 "头痛"
    val questionId: String,
    val average: Double,
    val latestValue: Double?,
    val trend: TrendFlag,
    val trendDescription: String // "上升" / "下降" / "稳定"
)

data class LifestyleSummaryForReport(
    val sleepSummary: String, // e.g., "平均每晚7-8小时 (70%)"
    val napSummary: String, // e.g., "无午睡 (80%)"
    val stepsSummary: String, // e.g., "日均6-10k步 (60%)"
    val chillExposureDays: Int,
    val menstrualSummary: String? // e.g., "经期3天，非经期4天"
)

data class AiInsightsSummaryForReport(
    val weeklyInsights: List<String>, // 周度洞察的关键点摘要
    val topSuggestions: List<String> // 最重要的AI建议（精选2-3条）
)

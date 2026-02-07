package com.heldairy.core.data

import com.heldairy.core.network.AdvicePayloadFormatException
import com.heldairy.core.network.DeepSeekClient
import com.heldairy.core.preferences.AiPreferencesStore
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class WeeklyInsightCoordinator(
    private val insightRepository: InsightRepository,
    private val preferencesStore: AiPreferencesStore,
    private val deepSeekClient: DeepSeekClient,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {

    suspend fun getWeeklyInsight(force: Boolean = false): WeeklyInsightResult {
        val today = LocalDate.now(clock)
        // 计算最近完成的周（上周一到上周日）
        val weekRange = InsightWeekRange.forLastCompletedWeek(today)
        
        // 先快速查找该周是否已有缓存
        val cached = insightRepository.findInsightForWeek(weekRange.start)

        // 如果该周已有成功的洞察，且不强制刷新，直接返回（快速路径）
        cached?.let { entity ->
            if (entity.status == STATUS_SUCCESS && !force) {
                val payload = entity.aiResultJson?.let { runCatching { json.decodeFromString<WeeklyInsightPayload>(it) }.getOrNull() }
                return WeeklyInsightResult(
                    status = WeeklyInsightStatus.Success,
                    payload = payload,
                    generatedAt = entity.generatedAt,
                    weekRange = InsightWeekRange(LocalDate.parse(entity.weekStartDate), LocalDate.parse(entity.weekEndDate)),
                    message = null
                )
            }
        }

        // 如果该周还未生成过（或失败过），则生成新的洞察
        // 不再限制只能在周日生成

        val settings = preferencesStore.currentSettings()
        if (!settings.aiEnabled) {
            return WeeklyInsightResult(
                status = WeeklyInsightStatus.Disabled,
                payload = null,
                generatedAt = null,
                weekRange = weekRange,
                message = "AI 建议暂时不可用"
            )
        }
        if (settings.apiKey.isBlank()) {
            return WeeklyInsightResult(
                status = WeeklyInsightStatus.Error,
                payload = null,
                generatedAt = null,
                weekRange = weekRange,
                message = "请先在设置里填写 DeepSeek API Key"
            )
        }

        val localSummary = insightRepository.buildLocalSummary(endDate = weekRange.end)
        val userPrompt = WeeklyInsightPromptBuilder.buildUserPrompt(weekRange, localSummary)
        val payload = try {
            deepSeekClient.fetchWeeklyInsight(
                apiKey = settings.apiKey,
                model = DEFAULT_MODEL,
                systemPrompt = SYSTEM_PROMPT,
                userPrompt = userPrompt
            )
        } catch (e: Exception) {
            insightRepository.saveInsightReport(
                weekStart = weekRange.start,
                weekEnd = weekRange.end,
                summary = localSummary,
                aiPayload = null,
                status = STATUS_FAILED,
                errorMessage = e.message
            )
            val message = if (e is AdvicePayloadFormatException) "AI 返回格式不正确" else "生成周报失败，请稍后重试"
            return WeeklyInsightResult(
                status = WeeklyInsightStatus.Error,
                payload = null,
                generatedAt = null,
                weekRange = weekRange,
                message = message
            )
        }

        val normalized = payload.normalized()
        val issues = normalized.validationErrors()
        if (issues.isNotEmpty()) {
            insightRepository.saveInsightReport(
                weekStart = weekRange.start,
                weekEnd = weekRange.end,
                summary = localSummary,
                aiPayload = null,
                status = STATUS_FAILED,
                errorMessage = issues.joinToString()
            )
            return WeeklyInsightResult(
                status = WeeklyInsightStatus.Error,
                payload = null,
                generatedAt = null,
                weekRange = weekRange,
                message = "AI 返回格式不正确"
            )
        }

        insightRepository.saveInsightReport(
            weekStart = weekRange.start,
            weekEnd = weekRange.end,
            summary = localSummary,
            aiPayload = normalized,
            status = STATUS_SUCCESS,
            errorMessage = null
        )
        return WeeklyInsightResult(
            status = WeeklyInsightStatus.Success,
            payload = normalized,
            generatedAt = clock.instant().toEpochMilli(),
            weekRange = weekRange,
            message = null
        )
    }

    companion object {
        private const val DEFAULT_MODEL = "deepseek-chat"
        private const val STATUS_SUCCESS = "success"
        private const val STATUS_FAILED = "failed"
        private val SYSTEM_PROMPT = buildString {
            appendLine("你是生活管家，帮用户写一段简短的周度洞察，不做医疗诊断，不谈药物调整。")
            appendLine("参考输入的 7 天/30 天摘要，给出 2-4 句 summary，1-3 条 highlights，1-3 条 suggestions，0-2 条 cautions。")
            appendLine("输出纯 JSON，不要 Markdown 或额外文字。")
        }
    }
}

object WeeklyInsightPromptBuilder {
    fun buildUserPrompt(range: InsightWeekRange, summary: InsightLocalSummary?): String {
        val builder = StringBuilder()
        builder.appendLine("周范围: ${range.start} 至 ${range.end}")
        summary?.window7?.let { window ->
            builder.appendLine("近7天填写: ${window.entryCount}/${window.days} 天，完成率 ${window.completionRate}")
            builder.appendLine("睡眠分布: ${window.sleepDistribution}")
            builder.appendLine("午休分布: ${window.napDistribution}")
            builder.appendLine("步数分布: ${window.stepsDistribution}")
            builder.appendLine("受凉天数: ${window.chillDays}")
            builder.appendLine("用药按时: ${window.medication.onTime} 天，遗漏 ${window.medication.missed} 天，未用 ${window.medication.na} 天")
            builder.appendLine("经期记录: ${window.menstrualCounts}")
            if (window.symptomMetrics.isNotEmpty()) {
                builder.appendLine("症状均值: ${window.symptomMetrics.joinToString { metric -> "${metric.questionId}=${metric.average} (${metric.trend})" }}")
            }
        }
        summary?.window30?.let { window ->
            builder.appendLine("近30天填写: ${window.entryCount}/${window.days} 天，完成率 ${window.completionRate}")
            if (window.symptomMetrics.isNotEmpty()) {
                builder.appendLine("30天症状均值: ${window.symptomMetrics.joinToString { metric -> "${metric.questionId}=${metric.average} (${metric.trend})" }}")
            }
        }
        builder.appendLine("请用温柔、理性的语气输出 JSON，避免诊断或药物建议。").appendLine("只返回 JSON。")
        return builder.toString()
    }
}

data class InsightWeekRange(val start: LocalDate, val end: LocalDate) {
    companion object {
        /**
         * 计算最近完成的周（周一到周日）
         * - 如果今天是周一到周日，返回上一个完整周
         * - 例如：今天是2026-02-09(周一)，返回2026-01-27(周一)到2026-02-02(周日)
         */
        fun forLastCompletedWeek(date: LocalDate): InsightWeekRange {
            // 找到上一个周日（不包括今天）
            val lastSunday = if (date.dayOfWeek == DayOfWeek.SUNDAY) {
                date.minusDays(7)
            } else {
                date.with(TemporalAdjusters.previous(DayOfWeek.SUNDAY))
            }
            // 周一是7天前
            val weekStart = lastSunday.minusDays(6)
            return InsightWeekRange(start = weekStart, end = lastSunday)
        }
        
        @Deprecated("Use forLastCompletedWeek instead", ReplaceWith("forLastCompletedWeek(date)"))
        fun forDate(date: LocalDate): InsightWeekRange = forLastCompletedWeek(date)
    }
}

data class WeeklyInsightResult(
    val status: WeeklyInsightStatus,
    val payload: WeeklyInsightPayload?,
    val generatedAt: Long?,
    val weekRange: InsightWeekRange,
    val message: String?
)

enum class WeeklyInsightStatus { Success, Pending, Disabled, Error }

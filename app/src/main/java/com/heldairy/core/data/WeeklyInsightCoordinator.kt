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
        val isSunday = today.dayOfWeek == DayOfWeek.SUNDAY
        
        // 周日：查看本周（周一到周日）的洞察
        // 非周日：查看上周（上周一到上周日）的洞察
        val weekRange = if (isSunday) {
            // 今天周日，计算本周一到今天
            val thisMonday = today.minusDays(6)
            InsightWeekRange(thisMonday, today)
        } else {
            // 非周日，显示上周的完整周
            InsightWeekRange.forLastCompletedWeek(today)
        }
        
        // 先快速查找该周是否已有缓存
        val cached = insightRepository.findInsightForWeek(weekRange.start)

        // 判断是否是第一周（无任何历史数据）
        val localSummary = insightRepository.buildLocalSummary(endDate = weekRange.end)
        val isFirstWeek = localSummary?.window7?.entryCount == 0 && cached == null
        
        if (isFirstWeek && !force) {
            // 第一周，还没有数据，不尝试生成
            return WeeklyInsightResult(
                status = WeeklyInsightStatus.NoData,
                payload = null,
                generatedAt = null,
                weekRange = weekRange,
                message = "等待你填写更多日报，就会生成 AI 洞察哦！"
            )
        }

        // 判断是否需要自动生成：
        // 1. 周日且本周还没有成功的洞察 → 自动生成
        // 2. 任何时候数据为空或失败 → 自动重新生成
        // 3. 已有成功数据且不force → 直接返回
        val shouldAutoGenerate = (isSunday && cached?.status != STATUS_SUCCESS) || 
                                 (cached == null) ||
                                 (cached.status == STATUS_FAILED)
        
        cached?.let { entity ->
            if (entity.status == STATUS_SUCCESS && !force && !shouldAutoGenerate) {
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

        // 需要生成新的洞察（周日首次打开/数据为空/失败/force）

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
            android.util.Log.e("WeeklyInsightCoordinator", "Validation failed: $issues")
            android.util.Log.e("WeeklyInsightCoordinator", "Payload: summary='${normalized.summary}', highlights=${normalized.highlights.size}, suggestions=${normalized.suggestions.size}")
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
                message = "AI 返回格式不正确: ${issues.joinToString()}"
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
            appendLine("你是生活管家，帮用户写一段简短的周度健康洞察，不做医疗诊断，不谈药物调整。")
            appendLine("要求：")
            appendLine("1. 结合长期记忆（7天/30天数据），针对这一周的情况进行分析")
            appendLine("2. summary: 2-3句话总结本周整体情况，40-60字")
            appendLine("3. highlights: 1-2条本周的亮点/进步，每条15-25字")
            appendLine("4. suggestions: 1-2条具体改善建议，每条20-30字")
            appendLine("5. cautions: 0-1条需要注意的事项，每条15-25字")
            appendLine("6. 总字数控制在20字以内")
            appendLine("7. 语气温柔、理性，像朋友聊天")
            appendLine("")
            appendLine("输出JSON格式：")
            appendLine("""")
            appendLine("{")
            appendLine("  \"schema_version\": 1,")
            appendLine("  \"week_start_date\": \"周一日期\",")
            appendLine("  \"week_end_date\": \"周日日期\",")
            appendLine("  \"summary\": \"总结文本\",")
            appendLine("  \"highlights\": [\"亮点1\", \"亮点2\"],")
            appendLine("  \"suggestions\": [\"建议1\", \"建议2\"],")
            appendLine("  \"cautions\": [\"注意事项\"],")
            appendLine("  \"confidence\": \"medium\"")
            appendLine("}")
            appendLine("""")
            appendLine("不要Markdown或额外文字，只返回JSON。week_start_date和week_end_date必须填写！")
        }
    }
}

object WeeklyInsightPromptBuilder {
    fun buildUserPrompt(range: InsightWeekRange, summary: InsightLocalSummary?): String {
        val builder = StringBuilder()
        builder.appendLine("周范围: ${range.start} 至 ${range.end}")
        builder.appendLine("请在JSON中使用: \"week_start_date\": \"${range.start}\", \"week_end_date\": \"${range.end}\"")
        builder.appendLine("")
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

enum class WeeklyInsightStatus { Success, Pending, Disabled, Error, NoData }

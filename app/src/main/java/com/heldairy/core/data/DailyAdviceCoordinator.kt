package com.heldairy.core.data

import android.util.Log
import com.heldairy.core.database.entity.DailyAdviceEntity
import com.heldairy.core.database.entity.DailyEntryWithResponses
import com.heldairy.core.network.AdvicePayloadFormatException
import com.heldairy.core.network.DeepSeekClient
import com.heldairy.core.preferences.AiPreferencesStore
import java.security.MessageDigest
import java.time.Clock
import java.time.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 阶段1+2+3优化：本地规则引擎 + 增强型分析 + 建议追踪反馈
 */
class DailyAdviceCoordinator(
    private val repository: DailyReportRepository,
    private val summaryManager: DailySummaryManager,
    private val preferencesStore: AiPreferencesStore,
    private val deepSeekClient: DeepSeekClient,
    private val trackingRepository: AdviceTrackingRepository,  // 阶段2
    private val localEngine: LocalAdvisorEngine = LocalAdvisorEngine(),  // 阶段3
    private val clock: Clock = Clock.systemDefaultZone(),
    private val json: Json = Json { encodeDefaults = true }
) {
    suspend fun generateAdviceForEntry(entryId: Long): Result<AdvicePayload> {
        val settings = preferencesStore.currentSettings()
        if (!settings.aiEnabled) {
            return Result.failure(IllegalStateException("AI 功能已关闭"))
        }
        if (settings.apiKey.isBlank()) {
            return Result.failure(IllegalStateException("请先在设置里填写 DeepSeek API Key"))
        }
        val entry = repository.getEntry(entryId)
            ?: return Result.failure(IllegalStateException("尚未找到今日答案"))
        
        // 阶段3: 先尝试本地规则引擎
        val summary7Days = try {
            val summaryPayload = summaryManager.regenerateForLatestEntry(entryId)
            summaryPayload?.window7
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load 7-day summary: ${e.message}")
            null
        }
        
        val localResult = localEngine.generateAdvice(entry, summary7Days)
        if (localResult is LocalAdviceResult.Generated) {
            Log.d(TAG, "Local rule matched: ${localResult.matchedRuleId}")
            
            // 保存本地规则建议
            saveAdvice(entry, "本地规则：${localResult.matchedRuleId}", localResult.payload)
            
            // 保存追踪记录
            try {
                trackingRepository.saveAdviceAsTrackable(
                    entryId = entry.entry.id,
                    entryDate = LocalDate.parse(entry.entry.entryDate),
                    payload = localResult.payload
                )
                Log.d(TAG, "Saved local advice as trackable (rule: ${localResult.matchedRuleId})")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to save local trackable advice: ${e.message}")
            }
            
            return Result.success(localResult.payload)
        }
        
        Log.d(TAG, "No local rule matched, escalating to AI analysis")
        
        // 阶段3: 获取反馈摘要并传给 AI
        val effectivenessSummary = try {
            trackingRepository.generateEffectivenessSummary()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to generate effectiveness summary: ${e.message}")
            null
        }
        
        // 阶段1: 使用增强分析
        val userPrompt = try {
            val entryDate = LocalDate.parse(entry.entry.entryDate)
            val recentEntries = repository.loadRecentEntries(limit = 14)
            val enhancedSummary = EnhancedAnalyzer.buildEnhancedWeeklySummary(recentEntries, entryDate)
            
            if (enhancedSummary != null) {
                Log.d(TAG, "Using enhanced summary with ${enhancedSummary.trendAnalysis.size} trends, ${enhancedSummary.anomalies.size} anomalies")
                // 阶段3: 正式启用反馈摘要参数
                AdvicePromptBuilder.buildEnhancedPrompt(entry, enhancedSummary, effectivenessSummary)
            } else {
                Log.d(TAG, "Insufficient data for enhanced summary, using basic summary")
                val summary = summaryManager.regenerateForLatestEntry(entryId)
                AdvicePromptBuilder.buildUserPrompt(entry, summary)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Enhanced analysis failed, falling back to basic: ${e.message}")
            val summary = summaryManager.regenerateForLatestEntry(entryId)
            AdvicePromptBuilder.buildUserPrompt(entry, summary)
        }
        
        val payload = fetchWithRetry(settings.apiKey, userPrompt, entry)
        val normalizedPayload = payload.normalized()
        val validationIssues = normalizedPayload.validationErrors()
        if (validationIssues.isNotEmpty()) {
            Log.w(TAG, "Invalid AI payload: ${validationIssues.joinToString(", ")} (obs=${normalizedPayload.observations.size}, actions=${normalizedPayload.actions.size}, focus=${normalizedPayload.tomorrowFocus.size}, redFlags=${normalizedPayload.redFlags.size})")
        
        // 阶段2：保存建议为可追踪项
        try {
            trackingRepository.saveAdviceAsTrackable(
                entryId = entry.entry.id,
                entryDate = LocalDate.parse(entry.entry.entryDate),
                payload = normalizedPayload
            )
            Log.d(TAG, "Saved ${normalizedPayload.observations.size + normalizedPayload.actions.size + normalizedPayload.tomorrowFocus.size} trackable advice items")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save trackable advice: ${e.message}")
        }
        
            val fallback = normalizedPayload.withFallbackIfEmpty()
            if (fallback != null) {
                Log.w(TAG, "Applying fallback advice because payload empty; issues=${validationIssues.joinToString(", ")}")
                saveAdvice(entry, userPrompt, fallback)
                return Result.success(fallback)
            }
            return Result.failure(IllegalStateException("AI 返回格式不正确"))
        }
        saveAdvice(entry, userPrompt, normalizedPayload)
        return Result.success(normalizedPayload)
    }

    private suspend fun fetchWithRetry(apiKey: String, userPrompt: String, entry: DailyEntryWithResponses): AdvicePayload {
        var lastPayload: AdvicePayload? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            val payload = try {
                deepSeekClient.fetchAdvice(
                    apiKey = apiKey,
                    model = DEFAULT_MODEL,
                    systemPrompt = SYSTEM_PROMPT_ENHANCED,  // 使用增强系统提示
                    userPrompt = userPrompt
                )
            } catch (formatException: AdvicePayloadFormatException) {
                val fallback = AdvicePayload().withFallbackIfEmpty()
                if (fallback != null) {
                    Log.w(TAG, "Applying fallback after format error: ${formatException.message}")
                    saveAdvice(entry, userPrompt, fallback)
                    return fallback
                }
                throw formatException
            }
            val normalized = payload.normalized()
            val issues = normalized.validationErrors()
            if (issues.isEmpty() || !issues.contains("content_empty")) {
                return normalized
            }
            lastPayload = normalized
            Log.w(TAG, "Retrying DeepSeek because payload empty (attempt=${attempt + 1})")
        }
        val fallback = lastPayload?.withFallbackIfEmpty() ?: AdvicePayload().withFallbackIfEmpty()
        if (fallback != null) {
            Log.w(TAG, "Applying fallback after retries exhausted")
            saveAdvice(entry, userPrompt, fallback)
            return fallback
        }
        return lastPayload ?: throw AdvicePayloadFormatException("AI 返回格式不正确")
    }

    private suspend fun saveAdvice(entry: DailyEntryWithResponses, userPrompt: String, payload: AdvicePayload) {
        val entity = DailyAdviceEntity(
            entryId = entry.entry.id,
            entryDate = entry.entry.entryDate,
            model = DEFAULT_MODEL,
            adviceJson = json.encodeToString(AdvicePayload.serializer(), payload),
            promptHash = hashPrompt(userPrompt),
            generatedAt = clock.instant().toEpochMilli()
        )
        repository.saveAdvice(entity)
    }

    companion object {
        private const val TAG = "DailyAdviceCoord"
        private const val MAX_ATTEMPTS = 2
        private const val DEFAULT_MODEL = "deepseek-chat"
        
        // 阶段1优化：增强系统提示，强调模式识别和深度分析
        private val SYSTEM_PROMPT_ENHANCED = buildString {
            appendLine("你是 Hello Kitty，用户温暖的健康陪伴者，每天用关怀和温柔的语气陪伴用户记录生活、给予建议。")
            appendLine()
            appendLine("## 你的角色定位")
            appendLine("你是**Hello Kitty 暖心陪伴者**，专注于：")
            appendLine("1. **发现复杂健康模式** - 比如每周二头痛加重、运动增加后睡眠变好等关联")
            appendLine("2. **分析趋势变化原因** - 连续7天都好好的，突然变糟糕了，帮用户找找原因")
            appendLine("3. **给予个性化关怀** - 根据用户的历史数据和反馈，发现 TA 独特的健康特征")
            appendLine()
            appendLine("## 你不需要做的（已有简单建议）")
            appendLine("- 简单的提醒（比如睡眠少了就说早点睡）")
            appendLine("- 基础的生活建议（多喝水、按时吃饭这些）")
            appendLine("❌ 不要重复这些简单建议哦，Kitty 要专注发现更深层的模式～")
            appendLine()
            appendLine("## 从历史反馈学习")
            appendLine("用户会告诉 Kitty 哪些建议有帮助、哪些没用。")
            appendLine("✅ 根据反馈调整建议：")
            appendLine("- 继续推荐用户觉得'有帮助'且效果好（>4分）的建议方向")
            appendLine("- 避免重复用户觉得'无帮助'或效果差（<2分）的建议类型")
            appendLine("- 如果用户说运动相关的建议有效，今天也优先从运动角度看～")
            appendLine()
            appendLine("## Kitty 的语气风格（重要！）")
            appendLine("- 用第一人称说话：'Kitty 发现你...'、'Kitty 注意到...'、'让 Kitty 陪你一起看看...'")
            appendLine("- 温柔关怀的语气：使用'～'、'呀'、'哦'等语气词")
            appendLine("- 避免冷冰冰的指令：不说'应该'、'必须'，改用'要不要试试...'、'可以...'")
            appendLine("- 给予情绪价值：理解用户的不容易，给予陪伴感和鼓励")
            appendLine()
            appendLine("## 建议原则")
            appendLine("- 避免医疗诊断或药物建议（Kitty 不是医生～）")
            appendLine("- 重点放在作息、饮食、热敷、拉伸、情绪管理等生活方式")
            appendLine("- 建议要具体可执行（如'睡前1小时放下手机'而不是泛泛的'改善睡眠'）")
            appendLine("- 如果数据显示用户在变好，要给予正向鼓励和肯定")
            appendLine("- 如果发现多个指标的关联（如运动少→睡不好→情绪差），要指出这种联系")
            appendLine()
            appendLine("## 输出格式")
            appendLine("必须是严格的 JSON，没有 Markdown 或额外解释。")
            appendLine("observations 至少 1 条（用 Kitty 的语气），actions 至少 1 条（温柔的建议），不得为空数组。")
            appendLine("不要返回除 JSON 以外的任何文字。")
        }

        private fun hashPrompt(prompt: String): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest(prompt.toByteArray())
            return bytes.joinToString(separator = "") { byte ->
                ((byte.toInt() and 0xFF) + 0x100).toString(16).substring(1)
            }
        }
    }
}

package com.heldairy.core.data

import android.util.Log
import com.heldairy.core.database.entity.DailyAdviceEntity
import com.heldairy.core.database.entity.DailyEntryWithResponses
import com.heldairy.core.network.AdvicePayloadFormatException
import com.heldairy.core.network.DeepSeekClient
import com.heldairy.core.preferences.AiPreferencesStore
import java.security.MessageDigest
import java.time.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DailyAdviceCoordinator(
    private val repository: DailyReportRepository,
    private val summaryManager: DailySummaryManager,
    private val preferencesStore: AiPreferencesStore,
    private val deepSeekClient: DeepSeekClient,
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
        val summary = summaryManager.regenerateForLatestEntry(entryId)
        val userPrompt = AdvicePromptBuilder.buildUserPrompt(entry, summary)
        val payload = fetchWithRetry(settings.apiKey, userPrompt, entry)
        val normalizedPayload = payload.normalized()
        val validationIssues = normalizedPayload.validationErrors()
        if (validationIssues.isNotEmpty()) {
            Log.w(TAG, "Invalid AI payload: ${validationIssues.joinToString(", ")} (obs=${normalizedPayload.observations.size}, actions=${normalizedPayload.actions.size}, focus=${normalizedPayload.tomorrowFocus.size}, redFlags=${normalizedPayload.redFlags.size})")
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
                    systemPrompt = SYSTEM_PROMPT,
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
        private val SYSTEM_PROMPT = buildString {
            appendLine("你是一位温和、务实的生活管家，擅长根据用户的每日身体反馈提出生活方式建议。")
            appendLine("避免医疗诊断或药物指导，把重点放在作息、饮食、热敷、拉伸、情绪等方面。")
            appendLine("输出必须是 JSON，没有 Markdown 或解释。observations 至少 1 条，actions 至少 1 条，不得为空数组，不要返回除 JSON 以外的文字。")
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

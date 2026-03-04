package com.heldairy.core.data

import android.util.Log
import com.heldairy.core.network.DeepSeekClient
import com.heldairy.core.network.agent.AgentClient
import com.heldairy.core.network.agent.AgentNotReadyException
import com.heldairy.core.preferences.AiPreferencesStore
import kotlinx.serialization.Serializable

@Serializable
data class AiFollowUpQuestionDto(
    val id: String? = null,
    val text: String,
    val type: String,
    val options: List<String>? = null
)

class AiFollowUpCoordinator(
    private val preferencesStore: AiPreferencesStore,
    private val deepSeekClient: DeepSeekClient,
    private val agentClient: AgentClient? = null  // Agent 优先路径
) {
    suspend fun fetchFollowUps(prompt: String): Result<List<AiFollowUpQuestionDto>> {
        val settings = preferencesStore.currentSettings()
        if (!settings.aiEnabled) return Result.success(emptyList())

        // ── Agent 优先 ──
        if (agentClient != null) {
            try {
                val questions = agentClient.fetchFollowUpQuestions(
                    todayAnswers = mapOf("raw_prompt" to prompt)
                )
                if (questions.isNotEmpty()) {
                    Log.d(TAG, "Agent follow-up: ${questions.size} questions")
                    return Result.success(questions.take(MAX_QUESTIONS))
                }
            } catch (e: AgentNotReadyException) {
                Log.d(TAG, "Agent not ready: ${e.message}")
            } catch (e: Exception) {
                Log.w(TAG, "Agent follow-up failed: ${e.message}, fallback to DeepSeek")
            }
        }

        // ── DeepSeek 回退 ──
        if (settings.apiKey.isBlank()) return Result.failure(IllegalStateException("请先在设置里填写 DeepSeek API Key"))
        return runCatching {
            deepSeekClient.fetchFollowUpQuestions(
                apiKey = settings.apiKey,
                model = DEFAULT_MODEL,
                systemPrompt = SYSTEM_PROMPT,
                userPrompt = prompt
            ).take(MAX_QUESTIONS)
        }
    }

    companion object {
        private const val TAG = "FollowUpCoord"
        private const val DEFAULT_MODEL = "deepseek-chat"
        private const val MAX_QUESTIONS = 2
        private val SYSTEM_PROMPT = buildString {
            appendLine("你是生活方式陪伴助手，帮助用户补充 1-2 个简短的封闭式追问。")
            appendLine("返回 JSON 数组，每个元素包含 text、type、options。只允许 single_choice 类型。问题要简短，避免医疗诊断。")
            appendLine("不要返回解释或 Markdown，只返回 JSON。")
        }
    }
}

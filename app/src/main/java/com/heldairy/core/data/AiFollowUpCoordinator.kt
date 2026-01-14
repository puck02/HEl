package com.heldairy.core.data

import com.heldairy.core.network.DeepSeekClient
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
    private val deepSeekClient: DeepSeekClient
) {
    suspend fun fetchFollowUps(prompt: String): Result<List<AiFollowUpQuestionDto>> {
        val settings = preferencesStore.currentSettings()
        if (!settings.aiEnabled) return Result.success(emptyList())
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
        private const val DEFAULT_MODEL = "deepseek-chat"
        private const val MAX_QUESTIONS = 2
        private val SYSTEM_PROMPT = buildString {
            appendLine("你是生活方式陪伴助手，帮助用户补充 1-2 个简短的封闭式追问。")
            appendLine("返回 JSON 数组，每个元素包含 text、type、options。只允许 single_choice 类型。问题要简短，避免医疗诊断。")
            appendLine("不要返回解释或 Markdown，只返回 JSON。")
        }
    }
}

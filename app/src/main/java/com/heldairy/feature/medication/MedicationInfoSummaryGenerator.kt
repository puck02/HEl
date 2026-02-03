package com.heldairy.feature.medication

import android.util.Log
import com.heldairy.core.network.DeepSeekApi
import com.heldairy.core.network.DeepSeekMessage
import com.heldairy.core.network.DeepSeekRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class MedicationInfoSummary(
    @SerialName("summary") val summary: String
)

class MedicationInfoSummaryGenerator(
    private val api: DeepSeekApi,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    suspend fun generateSummary(
        apiKey: String,
        medName: String,
        aliases: String? = null
    ): Result<String> {
        return try {
            val systemPrompt = """
你是一个药物信息助手，负责生成药物的简介说明。

**要求：**
1. 详细全面，字数控制在400字以内
2. 包含：主要用途、常见适应症、基本注意事项、可能的副作用
3. 语言通俗易懂，面向普通用户
4. 不要包含具体用法用量（用户已单独记录）
5. 强调：本信息仅供参考，具体请遵医嘱
6. 必须完整输出，不能使用省略号

**输出格式（严格JSON）：**
{
  "summary": "药物简介内容"
}
            """.trimIndent()

            val userPrompt = buildString {
                append("药物名称：$medName")
                if (!aliases.isNullOrBlank()) {
                    append("\n别名：$aliases")
                }
                append("\n\n请生成该药物的详细简介（400字以内，完整输出不使用省略号）。")
            }

            val request = DeepSeekRequest(
                model = "deepseek-chat",
                messages = listOf(
                    DeepSeekMessage(role = "system", content = systemPrompt),
                    DeepSeekMessage(role = "user", content = userPrompt)
                )
            )

            val response = api.createChatCompletion(
                authHeader = "Bearer $apiKey",
                request = request
            )

            val rawContent = response.choices.firstOrNull()?.message?.content
                ?: return Result.failure(Exception("AI 未返回内容"))

            Log.d("MedicationInfoSummaryGenerator", "Raw AI response: $rawContent")

            val cleanedContent = rawContent
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val result = json.decodeFromString<MedicationInfoSummary>(cleanedContent)

            if (result.summary.isBlank()) {
                return Result.failure(Exception("生成的简介为空"))
            }

            // AI已按要求控制在400字内，直接返回完整内容
            Result.success(result.summary)
        } catch (e: Exception) {
            Log.e("MedicationInfoSummaryGenerator", "Generate failed", e)
            Result.failure(Exception("生成失败：${e.message}"))
        }
    }
}

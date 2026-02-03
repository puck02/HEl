package com.heldairy.feature.medication

import android.util.Log
import com.heldairy.core.network.DeepSeekApi
import com.heldairy.core.network.DeepSeekMessage
import com.heldairy.core.network.DeepSeekRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class MedicationNlpResult(
    @SerialName("name") val name: String,
    @SerialName("aliases") val aliases: List<String> = emptyList(),
    @SerialName("frequency") val frequency: String? = null,
    @SerialName("dose") val dose: String? = null,
    @SerialName("time_hints") val timeHints: String? = null,
    @SerialName("note") val note: String? = null
)

class MedicationNlpParser(
    private val api: DeepSeekApi,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    suspend fun parseInput(apiKey: String, userInput: String): Result<MedicationNlpResult> {
        return try {
            val systemPrompt = """
你是一个中文药物信息提取助手。用户会输入关于药物的自然语言描述，请提取结构化信息。

**输出格式（严格JSON）：**
{
  "name": "药物标准名称（必填）",
  "aliases": ["别名1", "别名2"],
  "frequency": "服用频率（如：每日3次、每8小时一次）",
  "dose": "单次剂量（如：1片、500mg）",
  "time_hints": "服用时间提示（如：饭后、睡前）",
  "note": "额外备注"
}

**提取规则：**
1. name：必须提取，优先用通用名，其次商品名
2. aliases：提取输入中的其他叫法（商品名、俗称等）
3. frequency/dose/time_hints：尽量从输入中提取，无法确定则返回null
4. note：记录输入中其他有价值的信息（禁忌、副作用提示等）

**示例：**
输入："阿莫西林胶囊，每次2粒，一天3次，饭后吃"
输出：
{
  "name": "阿莫西林",
  "aliases": ["阿莫西林胶囊"],
  "frequency": "每日3次",
  "dose": "2粒",
  "time_hints": "饭后",
  "note": null
}
            """.trimIndent()

            val request = DeepSeekRequest(
                model = "deepseek-chat",
                messages = listOf(
                    DeepSeekMessage(role = "system", content = systemPrompt),
                    DeepSeekMessage(role = "user", content = userInput)
                )
            )

            val response = api.createChatCompletion(
                authHeader = "Bearer $apiKey",
                request = request
            )

            val rawContent = response.choices.firstOrNull()?.message?.content
                ?: return Result.failure(Exception("AI 未返回内容"))

            Log.d("MedicationNlpParser", "Raw AI response: $rawContent")

            // 清理 markdown 代码块标记
            val cleanedContent = rawContent
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val result = json.decodeFromString<MedicationNlpResult>(cleanedContent)

            if (result.name.isBlank()) {
                return Result.failure(Exception("无法识别药物名称，请补充更多信息"))
            }

            Result.success(result)
        } catch (e: Exception) {
            Log.e("MedicationNlpParser", "Parse failed", e)
            Result.failure(Exception("解析失败：${e.message}"))
        }
    }
}

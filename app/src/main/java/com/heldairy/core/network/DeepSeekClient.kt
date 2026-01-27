package com.heldairy.core.network

import android.util.Log
import com.heldairy.core.data.AdvicePayload
import com.heldairy.core.data.AiFollowUpQuestionDto
import com.heldairy.core.data.WeeklyInsightPayload
import java.security.MessageDigest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

class DeepSeekClient(
    private val api: DeepSeekApi,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    suspend fun fetchAdvice(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userPrompt: String
    ): AdvicePayload {
        val request = DeepSeekRequest(
            model = model,
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
            ?: throw AdvicePayloadFormatException("AI 没有返回任何内容")
        return parseAdvicePayload(rawContent)
    }

    suspend fun fetchFollowUpQuestions(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userPrompt: String
    ): List<AiFollowUpQuestionDto> {
        val request = DeepSeekRequest(
            model = model,
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
            ?: throw AdvicePayloadFormatException("AI 没有返回任何内容")
        return parseFollowUpPayload(rawContent)
    }

    suspend fun fetchWeeklyInsight(
        apiKey: String,
        model: String,
        systemPrompt: String,
        userPrompt: String
    ): WeeklyInsightPayload {
        val request = DeepSeekRequest(
            model = model,
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
            ?: throw AdvicePayloadFormatException("AI 没有返回任何内容")
        return parseWeeklyInsightPayload(rawContent)
    }

    private fun parseAdvicePayload(content: String): AdvicePayload {
        val sanitized = extractJsonBlock(content)
            ?: throw AdvicePayloadFormatException("AI 返回格式不正确，请重试。")
        logPayloadMeta(sanitized)
        val root = runCatching { json.parseToJsonElement(sanitized) }.getOrElse {
            throw AdvicePayloadFormatException("AI 返回格式不正确，请重试。")
        } as? JsonObject ?: throw AdvicePayloadFormatException("AI 返回格式不正确，请重试。")

        val observations = root.extractStringList("observations")
        val actions = root.extractStringList("actions")
        val focus = root.extractStringList("tomorrow_focus")
        val redFlags = root.extractStringList("red_flags")

        val payload = AdvicePayload(
            observations = observations,
            actions = actions,
            tomorrowFocus = focus,
            redFlags = redFlags
        )
        debugLogStructure(sanitized)
        return payload
    }

    private fun parseFollowUpPayload(content: String): List<AiFollowUpQuestionDto> {
        val sanitized = extractJsonBlock(content)
            ?: throw AdvicePayloadFormatException("AI 返回格式不正确，请重试。")
        logPayloadMeta(sanitized)
        val root = runCatching { json.parseToJsonElement(sanitized) }.getOrElse {
            throw AdvicePayloadFormatException("AI 返回格式不正确，请重试。")
        }
        val items = when (root) {
            is JsonArray -> root
            is JsonObject -> (root["questions"] as? JsonArray)
            else -> null
        } ?: throw AdvicePayloadFormatException("AI 返回格式不正确，请重试。")

        val parsed = items.mapNotNull { element ->
            (element as? JsonObject)?.let { obj ->
                val text = obj.extractString("text") ?: return@mapNotNull null
                val type = obj.extractString("type") ?: return@mapNotNull null
                val options = obj.extractStringList("options")
                AiFollowUpQuestionDto(
                    id = obj.extractString("id"),
                    text = text,
                    type = type,
                    options = options
                )
            }
        }
        if (parsed.isEmpty()) throw AdvicePayloadFormatException("AI 返回格式不正确，请重试。")
        return parsed
    }

    private fun parseWeeklyInsightPayload(content: String): WeeklyInsightPayload {
        val sanitized = extractJsonBlock(content)
            ?: throw AdvicePayloadFormatException("AI 返回格式不正确，请重试。")
        logPayloadMeta(sanitized)
        val root = runCatching { json.parseToJsonElement(sanitized) }.getOrElse {
            throw AdvicePayloadFormatException("AI 返回格式不正确，请重试。")
        } as? JsonObject ?: throw AdvicePayloadFormatException("AI 返回格式不正确，请重试。")

        val payload = WeeklyInsightPayload(
            schemaVersion = (root["schema_version"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 1,
            weekStartDate = root.extractString("week_start_date") ?: "",
            weekEndDate = root.extractString("week_end_date") ?: "",
            summary = root.extractString("summary") ?: "",
            highlights = root.extractStringList("highlights"),
            suggestions = root.extractStringList("suggestions"),
            cautions = root.extractStringList("cautions"),
            confidence = root.extractString("confidence") ?: ""
        )
        return payload
    }

    private fun extractJsonBlock(content: String): String? {
        val trimmed = content.trim()
        extractFromCodeFence(trimmed)?.let { return it }
        return extractBalancedJson(trimmed)
    }

    private fun extractFromCodeFence(text: String): String? {
        if (!text.startsWith("```")) return null
        val firstLineBreak = text.indexOf('\n')
        val bodyStart = if (firstLineBreak != -1) firstLineBreak + 1 else text.length
        val withoutPrefix = if (bodyStart <= text.length) text.substring(bodyStart) else ""
        val closingFenceIndex = withoutPrefix.lastIndexOf("```")
        if (closingFenceIndex == -1) return null
        return withoutPrefix.substring(0, closingFenceIndex).trim().takeIf { it.isNotEmpty() }
    }

    private fun extractBalancedJson(text: String): String? {
        return extractBalancedBlock(text, '{', '}')
            ?: extractBalancedBlock(text, '[', ']')
    }

    private fun logPayloadMeta(sanitizedJson: String) {
        val len = sanitizedJson.length
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(sanitizedJson.toByteArray())
            .joinToString(separator = "") { byte ->
                ((byte.toInt() and 0xFF) + 0x100).toString(16).substring(1)
            }
        Log.d(TAG, "DeepSeek advice sanitized len=$len sha256=$hash")
    }

    private fun debugLogStructure(sanitizedJson: String) {
        runCatching {
            val element = json.parseToJsonElement(sanitizedJson)
            val summary = when (element) {
                is JsonObject -> element.entries.joinToString(prefix = "{", postfix = "}") { (key, value) ->
                    "$key=${describeElement(value)}"
                }
                is JsonArray -> "array[size=${element.size}]"
                else -> element::class.simpleName ?: "unknown"
            }
            Log.d(TAG, "DeepSeek structure: $summary")
        }
    }

    private fun describeElement(element: JsonElement): String {
        return when (element) {
            is JsonArray -> "array.size=${element.size}"
            is JsonObject -> "object.keys=${element.keys.joinToString()}"
            else -> "primitive=${element.jsonPrimitive.content.take(8)}"
        }
    }

    private fun JsonObject.extractStringList(key: String): List<String> {
        val element = this[key] ?: return emptyList()
        return when (element) {
            is JsonArray -> element.mapNotNull { it.coerceToString() }.filter { it.isNotBlank() }
            is JsonPrimitive -> listOf(element.content).filter { it.isNotBlank() }
            is JsonObject -> listOfNotNull(element.coerceToString()).filter { it.isNotBlank() }
            else -> emptyList()
        }
    }

    private fun JsonElement.coerceToString(): String? {
        return when (this) {
            is JsonPrimitive -> this.content
            is JsonObject -> {
                val description = (this["description"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
                val category = (this["category"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
                if (description != null && category != null) {
                    "$category：$description"
                } else {
                    description ?: run {
                        val candidateKeys = listOf(
                            "suggestion",
                            "text",
                            "message",
                            "content",
                            "value",
                            "detail",
                            "title",
                            "label"
                        )
                        candidateKeys.firstNotNullOfOrNull { key ->
                            (this[key] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
                        }
                    } ?: this.toString()
                }
            }
            is JsonArray -> this.firstOrNull()?.coerceToString()
            else -> null
        }
    }

    private fun JsonObject.extractString(key: String): String? =
        (this[key] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }

    companion object {
        private const val TAG = "DeepSeekClient"
    }

    private fun extractBalancedBlock(text: String, openChar: Char, closeChar: Char): String? {
        var startIndex = -1
        var depth = 0
        var inString = false
        var escapeNext = false
        text.forEachIndexed { index, char ->
            if (escapeNext) {
                escapeNext = false
                return@forEachIndexed
            }
            if (char == '\\' && inString) {
                escapeNext = true
                return@forEachIndexed
            }
            if (char == '"') {
                inString = !inString
                return@forEachIndexed
            }
            if (inString) {
                return@forEachIndexed
            }
            when (char) {
                openChar -> {
                    if (depth == 0) {
                        startIndex = index
                    }
                    depth++
                }

                closeChar -> {
                    if (depth == 0) return@forEachIndexed
                    depth--
                    if (depth == 0 && startIndex != -1) {
                        return text.substring(startIndex, index + 1).trim()
                    }
                }
            }
        }
        return null
    }
}

class AdvicePayloadFormatException(message: String) : IllegalStateException(message)

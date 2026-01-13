package com.heldairy.core.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AdvicePayload(
    val observations: List<String> = emptyList(),
    val actions: List<String> = emptyList(),
    @SerialName("tomorrow_focus") val tomorrowFocus: List<String> = emptyList(),
    @SerialName("red_flags") val redFlags: List<String> = emptyList(),
    val source: AdviceSource = AdviceSource.AI
) {
    fun normalized(): AdvicePayload {
        fun List<String>.cleanup(limit: Int): List<String> = asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(limit)
            .toList()

        val normalizedRedFlags = redFlags.cleanup(limit = 3).map { raw ->
            val prefixed = when {
                raw.startsWith("如出现") || raw.startsWith("若出现") -> raw
                else -> "如出现${raw}"
            }
            if (prefixed.endsWith("请就医")) prefixed else "$prefixed，请就医"
        }

        return copy(
            observations = observations.cleanup(limit = 3),
            actions = actions.cleanup(limit = 3),
            tomorrowFocus = tomorrowFocus.cleanup(limit = 2),
            redFlags = normalizedRedFlags
        )
    }

    fun validationErrors(): List<String> {
        val issues = mutableListOf<String>()
        val observationCount = observations.size
        val actionCount = actions.size
        if (observationCount + actionCount == 0) {
            issues += "content_empty"
        }
        if (observationCount > 3) {
            issues += "observations_too_many=$observationCount"
        }
        if (actionCount > 3) {
            issues += "actions_too_many=$actionCount"
        }
        val focusCount = tomorrowFocus.size
        if (focusCount > 2) {
            issues += "focus_too_many=$focusCount"
        }
        return issues
    }

    fun isValid(): Boolean = validationErrors().isEmpty()

    fun withFallbackIfEmpty(): AdvicePayload? {
        if (observations.isNotEmpty() || actions.isNotEmpty()) return null
        val safeObservation = "今日记录较少，先关注基础作息与补水。"
        val safeAction = "保持规律三餐、30-60 分钟轻运动，若出现明显不适请就医。"
        return copy(
            observations = listOf(safeObservation),
            actions = listOf(safeAction),
            redFlags = listOf("如出现持续发热、胸痛、剧烈头痛，请就医"),
            source = AdviceSource.FALLBACK
        )
    }
}

@Serializable
enum class AdviceSource {
    @SerialName("ai") AI,
    @SerialName("fallback") FALLBACK
}

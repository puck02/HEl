package com.heldairy.feature.report.model

import com.heldairy.core.data.TrendFlag

object AiFollowUpPromptBuilder {
    fun buildPrompt(
        answers: Map<String, DailyAnswerPayload>,
        trends: Map<String, TrendFlag>
    ): String {
        val summaryLines = mutableListOf<String>()
        summaryLines += "# 今日回答"
        answers.forEach { (id, payload) ->
            val value = when (payload) {
                is DailyAnswerPayload.Choice -> payload.optionId
                is DailyAnswerPayload.MultiChoice -> payload.optionIds.joinToString(",")
                is DailyAnswerPayload.Slider -> payload.value.toString()
                is DailyAnswerPayload.Text -> payload.value.take(50)
            }
            summaryLines += "- $id=$value"
        }
        if (trends.isNotEmpty()) {
            summaryLines += "# 近期趋势"
            trends.forEach { (id, flag) ->
                summaryLines += "- $id=$flag"
            }
        }
        summaryLines += "# 需求"
        summaryLines += "生成最多 2 个追问，使用 single_choice，避免与上述问题重复，JSON 数组返回 questions。"
        return summaryLines.joinToString("\n")
    }
}

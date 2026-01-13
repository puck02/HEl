package com.heldairy.core.data

import com.heldairy.core.database.entity.DailyEntryWithResponses

object AdvicePromptBuilder {
    fun buildUserPrompt(
        entry: DailyEntryWithResponses,
        summary: DailySummaryPayload?
    ): String {
        val answersBlock = entry.responses
            .sortedBy { it.questionOrder }
            .joinToString(separator = "\n") { response ->
                "- ${response.questionId}: ${response.answerLabel}"
            }

        val summaryBlock = summary?.window7?.metrics?.joinToString(separator = "\n") { metric ->
            "- ${metric.questionId}: 平均 ${metric.average} / 最新 ${metric.latestValue ?: "-"}, 高值 ${metric.highCount} 天, 趋势 ${metric.trend}"
        } ?: "无足够历史样本"

        return buildString {
            appendLine("今日基础回答：")
            appendLine(answersBlock)
            appendLine()
            appendLine("最近 7 天概览：")
            appendLine(summaryBlock)
            appendLine()
            appendLine("根据上述信息，输出 JSON，不要添加额外文本。")
        }
    }
}

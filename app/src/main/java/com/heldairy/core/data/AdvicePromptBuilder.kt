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

        val window7Block = summary?.window7?.metrics?.joinToString(separator = "\n") { metric ->
            "- ${metric.questionId}: 平均 ${metric.average} / 最新 ${metric.latestValue ?: "-"}, 高值 ${metric.highCount} 天, 趋势 ${metric.trend}"
        } ?: "无足够历史样本"

        val window30Block = summary?.window30?.metrics?.joinToString(separator = "\n") { metric ->
            "- ${metric.questionId}: 30 天平均 ${metric.average} / 最新 ${metric.latestValue ?: "-"}, 高值 ${metric.highCount} 天, 趋势 ${metric.trend}"
        } ?: "无足够历史样本"

        return buildString {
            appendLine("今日基础回答：")
            appendLine(answersBlock)
            appendLine()
            appendLine("最近 7 天概览（重点参考）：")
            appendLine(window7Block)
            appendLine()
            appendLine("最近 30 天趋势（次要参考，用于长期背景）：")
            appendLine(window30Block)
            appendLine()
            appendLine("根据上述信息，优先基于今日与最近 7 天给出建议，30 天数据仅作辅助背景。输出 JSON，不要添加额外文本。")
        }
    }
}

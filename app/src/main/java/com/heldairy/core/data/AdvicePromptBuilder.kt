package com.heldairy.core.data

import com.heldairy.core.database.entity.DailyEntryWithResponses
import java.time.LocalDate

/**
 * 阶段1优化：增强Prompt结构，提供趋势+异常+改善信号
 */
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
    
    /**
     * 阶段1+2增强：使用增强摘要 + 历史建议反馈构建Prompt
     */
    fun buildEnhancedPrompt(
        entry: DailyEntryWithResponses,
        enhancedSummary: EnhancedWeeklySummary?,
        effectivenessSummary: String? = null
    ): String {
        val answersBlock = entry.responses
            .sortedBy { it.questionOrder }
            .joinToString(separator = "\n") { response ->
                "- ${response.questionId}: ${response.answerLabel}"
            }
        
        if (enhancedSummary == null) {
            // Fallback到简单版本
            return buildString {
                appendLine("## 今日健康数据")
                appendLine(answersBlock)
                appendLine()
                appendLine("历史数据不足，无法生成趋势分析。")
                appendLine()
                appendLine("请基于今日数据给出基础建议。输出 JSON，不要添加额外文本。")
            }
        }
        
        val trendBlock = if (enhancedSummary.trendAnalysis.isNotEmpty()) {
            enhancedSummary.trendAnalysis.entries.joinToString(separator = "\n") { (metric, trend) ->
                "- ${metric}: ${trend.description}（变化${trend.magnitude.toInt()}%，置信度${trend.confidence}）"
            }
        } else {
            "暂无明显趋势"
        }
        
        val anomalyBlock = if (enhancedSummary.anomalies.isNotEmpty()) {
            enhancedSummary.anomalies.take(3).joinToString(separator = "\n") { anomaly ->
                "- ${anomaly.date}: ${anomaly.description}（严重程度：${anomaly.severity}）"
            }
        } else {
            "未检测到异常"
        }
        
        val improvementBlock = if (enhancedSummary.improvements.isNotEmpty()) {
            enhancedSummary.improvements.joinToString(separator = "\n") { "- $it" }
        } else {
            "暂无"
        }
        
        val concernBlock = if (enhancedSummary.concernPatterns.isNotEmpty()) {
            enhancedSummary.concernPatterns.joinToString(separator = "\n") { "- $it" }
        } else {
            "暂无"
        }
        
        val weekOverWeekBlock = enhancedSummary.weekOverWeekChange?.let { changes ->
            changes.entries.joinToString(separator = "\n") { (metric, change) ->
                val direction = if (change > 0) "↑" else if (change < 0) "↓" else "→"
                "- ${metric}: $direction ${String.format("%.1f", abs(change))}%"
            }
        } ?: "无上周对比数据"
        
        return buildString {
            appendLine("## 用户健康档案")
            
            // 阶段2新增：历史建议反馈摘要
            if (effectivenessSummary != null) {
                appendLine(effectivenessSummary)
                appendLine()
            }
            
            appendLine()
            appendLine("### 今日详细数据")
            appendLine(answersBlock)
            appendLine()
            appendLine("### 7天趋势分析（重点关注）")
            appendLine(trendBlock)
            appendLine()
            appendLine("### ⚠️ 需要关注的模式")
            appendLine(concernBlock)
            appendLine()
            appendLine("### ✅ 改善信号")
            appendLine(improvementBlock)
            appendLine()
            appendLine("### 🔍 异常事件")
            appendLine(anomalyBlock)
            appendLine()
            appendLine("### 📊 周环比变化")
            appendLine(weekOverWeekBlock)
            appendLine()
            appendLine("## 生成建议要求")
            appendLine("1. **优先解决\"需要关注的模式\"** - 这些是持续恶化或严重异常的项目")
            appendLine("2. **认可并强化\"改善信号\"** - 给予正向反馈，鼓励用户保持良好习惯")
            appendLine("3. **建议需具体可执行** - 例如\"睡前1小时关闭屏幕\"而非\"改善睡眠\"")
            appendLine("4. **参考异常事件** - 如果某天数据异常，可询问当天发生了什么")
            appendLine("5. **结合周环比** - 如果某项指标本周比上周恶化>20%，需特别关注")
            appendLine()
            appendLine("输出严格 JSON 格式，不要添加 Markdown 或解释文字。observations 至少 1 条，actions 至少 1 条。")
        }
    }
    
    private fun abs(value: Float): Float = if (value < 0) -value else value
}


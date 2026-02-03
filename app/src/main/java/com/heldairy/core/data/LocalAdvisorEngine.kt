package com.heldairy.core.data

import com.heldairy.core.database.entity.DailyEntryWithResponses
import com.heldairy.core.database.entity.QuestionResponseEntity

/**
 * 本地规则引擎 - 阶段3核心组件
 * 
 * 职责：
 * 1. 基于确定性规则处理简单健康指标异常（睡眠不足、运动缺乏等）
 * 2. 快速响应，无网络延迟
 * 3. 降低 70% 的 AI API 调用量
 * 
 * 设计原则：
 * - 规则触发条件明确（如 sleep_hours < 6）
 * - 建议模板简洁实用
 * - 按优先级匹配，返回最高优先级规则的建议
 */
class LocalAdvisorEngine {

    /**
     * 尝试使用本地规则生成建议
     * 
     * @param entry 今日完整问答数据
     * @param weeklySummary 7天摘要（可选）
     * @return LocalAdviceResult.Generated 如果匹配到规则；LocalAdviceResult.NoMatch 需要 AI 介入
     */
    fun generateAdvice(
        entry: DailyEntryWithResponses,
        weeklySummary: SummaryWindow?
    ): LocalAdviceResult {
        val responses = entry.responses
        
        // 按优先级顺序检查规则
        val matchedRule = RULES
            .sortedByDescending { it.priority }
            .firstOrNull { rule -> rule.triggerCondition(responses, weeklySummary) }
        
        return if (matchedRule != null) {
            LocalAdviceResult.Generated(
                payload = matchedRule.adviceTemplate,
                matchedRuleId = matchedRule.id
            )
        } else {
            LocalAdviceResult.NoMatch
        }
    }

    companion object {
        /**
         * 本地规则库
         * 
         * 优先级定义：
         * - 10: 紧急健康风险（严重疼痛、睡眠极度不足）
         * - 5: 常见健康问题（睡眠不足、运动缺乏）
         * - 1: 基础建议（保持现状）
         */
        private val RULES = listOf(
            // 规则1: 睡眠严重不足（< 5小时）
            LocalAdviceRule(
                id = "sleep_critical_lt_5h",
                category = "sleep",
                priority = 10,
                triggerCondition = { responses: List<QuestionResponseEntity>, _: SummaryWindow? ->
                    val sleepHours = responses.findFloatAnswer("sleep_duration") ?: 7f
                    sleepHours < 5f
                },
                adviceTemplate = AdvicePayload(
                    observations = listOf("睡眠时长严重不足（<5小时），身体恢复受限"),
                    actions = listOf(
                        "今晚务必提前1小时上床，目标睡足7小时",
                        "睡前1小时避免屏幕光线，可尝试热水泡脚",
                        "若持续失眠超过3天，建议咨询医生"
                    ),
                    tomorrowFocus = listOf("记录实际入睡时间和起床感受"),
                    source = AdviceSource.LOCAL
                )
            ),
            
            // 规则2: 睡眠不足（5-6小时）
            LocalAdviceRule(
                id = "sleep_insufficient_5_6h",
                category = "sleep",
                priority = 5,
                triggerCondition = { responses: List<QuestionResponseEntity>, _: SummaryWindow? ->
                    val sleepHours = responses.findFloatAnswer("sleep_duration") ?: 7f
                    sleepHours in 5f..6f
                },
                adviceTemplate = AdvicePayload(
                    observations = listOf("睡眠时长略显不足（5-6小时）"),
                    actions = listOf(
                        "今晚尝试提前30分钟上床",
                        "午休控制在20-30分钟内，避免影响夜间睡眠"
                    ),
                    tomorrowFocus = listOf("观察睡眠时长变化"),
                    source = AdviceSource.LOCAL
                )
            ),
            
            // 规则3: 完全无运动
            LocalAdviceRule(
                id = "exercise_zero",
                category = "exercise",
                priority = 6,
                triggerCondition = { responses: List<QuestionResponseEntity>, summary: SummaryWindow? ->
                    val exerciseMinutes = responses.findFloatAnswer("exercise_duration") ?: 30f
                    val exerciseTimes = responses.findFloatAnswer("exercise_frequency") ?: 1f
                    
                    // 今日无运动且7天内平均<15分钟/天
                    val todayZero = exerciseMinutes == 0f || exerciseTimes == 0f
                    val weekAvgLow = summary?.metrics?.find { 
                        it.questionId == "exercise_duration" 
                    }?.average?.let { it < 15.0 } ?: false
                    
                    todayZero && weekAvgLow
                },
                adviceTemplate = AdvicePayload(
                    observations = listOf("近期运动量明显不足"),
                    actions = listOf(
                        "从轻度活动开始：饭后散步15-20分钟",
                        "选择喜欢的活动（游泳、骑行、瑜伽），降低心理负担",
                        "设定小目标：本周至少运动2次"
                    ),
                    tomorrowFocus = listOf("记录是否完成运动计划"),
                    source = AdviceSource.LOCAL
                )
            ),
            
            // 规则4: 疼痛高强度（>7分）
            LocalAdviceRule(
                id = "pain_severe_gt_7",
                category = "pain",
                priority = 10,
                triggerCondition = { responses: List<QuestionResponseEntity>, _: SummaryWindow? ->
                    val painLevel = responses.findFloatAnswer("pain_level") ?: 0f
                    painLevel > 7f
                },
                adviceTemplate = AdvicePayload(
                    observations = listOf("疼痛程度较高（>7分），需重点关注"),
                    actions = listOf(
                        "记录疼痛具体部位和持续时长",
                        "避免剧烈活动，适当休息",
                        "可尝试冷敷或热敷缓解（根据疼痛类型选择）"
                    ),
                    tomorrowFocus = listOf("观察疼痛是否缓解"),
                    redFlags = listOf("如疼痛持续加重、伴有发热或活动受限，请及时就医"),
                    source = AdviceSource.LOCAL
                )
            ),
            
            // 规则5: 情绪低落（<3分）
            LocalAdviceRule(
                id = "mood_low_lt_3",
                category = "mood",
                priority = 7,
                triggerCondition = { responses: List<QuestionResponseEntity>, _: SummaryWindow? ->
                    val moodScore = responses.findFloatAnswer("mood_score") ?: 5f
                    moodScore < 3f
                },
                adviceTemplate = AdvicePayload(
                    observations = listOf("情绪状态偏低（<3分）"),
                    actions = listOf(
                        "尝试进行喜欢的轻度活动（听音乐、看书、晒太阳）",
                        "与信任的朋友或家人简单聊聊",
                        "保持规律作息，避免独自长时间封闭"
                    ),
                    tomorrowFocus = listOf("记录情绪变化和影响因素"),
                    redFlags = listOf("如情绪持续低落超过2周、出现自伤念头，请寻求专业心理咨询"),
                    source = AdviceSource.LOCAL
                )
            ),
            
            // 规则6: 饮食不规律（少于2餐）
            LocalAdviceRule(
                id = "diet_irregular_lt_2_meals",
                category = "diet",
                priority = 4,
                triggerCondition = { responses: List<QuestionResponseEntity>, _: SummaryWindow? ->
                    val mealCount = responses.findFloatAnswer("meal_count") ?: 3f
                    mealCount < 2f
                },
                adviceTemplate = AdvicePayload(
                    observations = listOf("今日进食不足2餐，营养摄入可能不足"),
                    actions = listOf(
                        "明日尽量保证三餐规律，即使食欲不佳也建议少量进食",
                        "准备一些健康零食（坚果、水果）避免长时间空腹",
                        "保持充足饮水（1500-2000ml/天）"
                    ),
                    tomorrowFocus = listOf("记录三餐时间和食欲变化"),
                    source = AdviceSource.LOCAL
                )
            ),
            
            // 规则7: 水分摄入不足（<800ml）
            LocalAdviceRule(
                id = "water_insufficient_lt_800ml",
                category = "diet",
                priority = 3,
                triggerCondition = { responses: List<QuestionResponseEntity>, _: SummaryWindow? ->
                    val waterIntake = responses.findFloatAnswer("water_intake") ?: 1500f
                    waterIntake < 800f
                },
                adviceTemplate = AdvicePayload(
                    observations = listOf("今日饮水量明显不足（<800ml）"),
                    actions = listOf(
                        "设置饮水提醒：每2小时喝一杯水（约200ml）",
                        "随身携带水杯，增加饮水便利性",
                        "可适当饮用温开水、淡茶，避免过多含糖饮料"
                    ),
                    tomorrowFocus = listOf("记录饮水总量"),
                    source = AdviceSource.LOCAL
                )
            ),
            
            // 规则8: 睡眠质量差（<5分）
            LocalAdviceRule(
                id = "sleep_quality_poor_lt_5",
                category = "sleep",
                priority = 6,
                triggerCondition = { responses: List<QuestionResponseEntity>, _: SummaryWindow? ->
                    val sleepQuality = responses.findFloatAnswer("sleep_quality") ?: 7f
                    sleepQuality < 5f
                },
                adviceTemplate = AdvicePayload(
                    observations = listOf("睡眠质量评分较低（<5分）"),
                    actions = listOf(
                        "检查睡眠环境：保持安静、黑暗、温度适宜（18-22℃）",
                        "睡前避免咖啡因、酒精摄入",
                        "建立固定睡眠仪式（如冥想、轻音乐）"
                    ),
                    tomorrowFocus = listOf("观察睡眠质量改善情况"),
                    source = AdviceSource.LOCAL
                )
            )
        )
    }
}

/**
 * 本地建议规则定义
 */
data class LocalAdviceRule(
    val id: String,
    val category: String,
    val priority: Int,
    val triggerCondition: (List<QuestionResponseEntity>, SummaryWindow?) -> Boolean,
    val adviceTemplate: AdvicePayload
)

/**
 * 本地建议生成结果
 */
sealed interface LocalAdviceResult {
    /**
     * 成功匹配到本地规则
     */
    data class Generated(
        val payload: AdvicePayload,
        val matchedRuleId: String
    ) : LocalAdviceResult
    
    /**
     * 无规则匹配，需要 AI 介入分析
     */
    object NoMatch : LocalAdviceResult
}

/**
 * 辅助扩展函数：从响应列表中查找浮点数答案
 */
private fun List<QuestionResponseEntity>.findFloatAnswer(questionId: String): Float? {
    return this.find { it.questionId == questionId }
        ?.answerValue
        ?.toFloatOrNull()
}

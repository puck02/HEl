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
                    observations = listOf("Kitty 发现你睡眠时间太少啦（不到5小时），身体恢复不过来呢～"),
                    actions = listOf(
                        "今晚一定要提前1小时上床哦，咱们争取睡足7小时～",
                        "睡前1小时就别看手机了～要不要试试泡泡脚，会更舒服～",
                        "如果连续3天都睡不好，记得去看看医生呀～"
                    ),
                    tomorrowFocus = listOf("明天告诉 Kitty 你几点睡的、几点起的，感觉怎么样～"),
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
                    observations = listOf("Kitty 觉得你睡得有点少呢（5-6小时）～"),
                    actions = listOf(
                        "今晚可以试着提前30分钟上床哦～",
                        "如果午睡的话，控制在20-30分钟就好，不然晚上会睡不着的～"
                    ),
                    tomorrowFocus = listOf("明天看看睡眠时间有没有改善～"),
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
                    observations = listOf("Kitty 看到你最近运动好少呀～"),
                    actions = listOf(
                        "咱们从简单的开始～吃完饭散散步，走个15-20分钟就很棒～",
                        "选你喜欢的活动就好（游泳、骑车、瑜伽...），开心最重要～",
                        "给自己定个小目标：这周至少动一动2次～"
                    ),
                    tomorrowFocus = listOf("明天记得告诉 Kitty 有没有完成运动计划哦～"),
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
                    observations = listOf("Kitty 看到你的疼痛有点严重呢（超过7分了），要好好关注～"),
                    actions = listOf(
                        "记下来疼痛的具体位置和持续了多久～",
                        "今天先好好休息，别做太剧烈的活动啦～",
                        "试试冷敷或热敷（根据疼痛类型选），可能会舒服一点～"
                    ),
                    tomorrowFocus = listOf("明天告诉 Kitty 疼痛有没有缓解～"),
                    redFlags = listOf("如果疼痛一直在加重、还发烧、或者影响活动了，一定要去看医生呀～"),
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
                    observations = listOf("Kitty 感觉到你今天情绪不太好呢（不到3分）～"),
                    actions = listOf(
                        "做点你喜欢的事情吧～听听音乐、看看书、或者出去晒晒太阳～",
                        "找个信任的朋友或家人聊聊天，说说话会舒服一点～",
                        "记得按时睡觉起床，别一个人闷太久哦～"
                    ),
                    tomorrowFocus = listOf("明天告诉 Kitty 心情有没有好一点，是什么影响了情绪～"),
                    redFlags = listOf("如果心情低落持续超过2周、或者有自我伤害的念头，一定要找专业的心理咨询师帮忙呀～"),
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
                    observations = listOf("Kitty 发现你今天吃饭太少了（不到2顿），营养可能跟不上呀～"),
                    actions = listOf(
                        "明天尽量三餐按时吃哦～就算不太饿也吃一点点～",
                        "准备些健康小零食（坚果、水果）放在身边，别让自己太饿～",
                        "记得多喝水呀（一天1500-2000ml）～"
                    ),
                    tomorrowFocus = listOf("明天记录吃饭时间和食欲怎么样～"),
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
                    observations = listOf("Kitty 看到你今天喝水好少呀（不到800ml）～"),
                    actions = listOf(
                        "给自己设个喝水提醒吧～每2小时喝一杯（大概200ml）～",
                        "随身带个水杯，想喝就喝，会方便很多～",
                        "可以喝温开水或淡茶，尽量少喝含糖饮料哦～"
                    ),
                    tomorrowFocus = listOf("明天记录下总共喝了多少水～"),
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
                    observations = listOf("Kitty 注意到你睡眠质量不太好呢（不到5分）～"),
                    actions = listOf(
                        "检查一下睡觉的环境～尽量保持安静、黑暗，温度在18-22℃最舒服～",
                        "睡前别喝咖啡或酒精饮料哦～",
                        "试试建立睡前小仪式（比如冥想、听轻音乐）～"
                    ),
                    tomorrowFocus = listOf("明天看看睡眠质量有没有改善～"),
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

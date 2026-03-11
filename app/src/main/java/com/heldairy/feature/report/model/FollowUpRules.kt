package com.heldairy.feature.report.model

import com.heldairy.core.data.TrendFlag

private data class FollowUpRule(
    val symptomQuestionId: String,
    val focusOptionId: String? = null,
    val extraPredicate: ((Map<String, DailyAnswerPayload>) -> Boolean)? = null,
    val questions: List<DailyQuestion>
)

object FollowUpRuleEngine {
    private const val SEVERITY_THRESHOLD = 6
    private const val MAX_QUESTIONS = 6

    private val rules: List<FollowUpRule> = listOf(
        FollowUpRule(
            symptomQuestionId = "headache_intensity",
            focusOptionId = "head",
            questions = listOf(
                DailyQuestion(
                    id = "fu_headache_nature",
                    title = "来～告诉 Kitty 头痛是什么感觉～",
                    prompt = "我来帮你看看是紧张型还是偏头痛～",
                    step = DailyQuestionStep.FollowUp,
                    order = 100,
                    required = true,
                    kind = QuestionKind.SingleChoice(
                        options = listOf(
                            QuestionOption("dull", "钝痛/压迫感"),
                            QuestionOption("throbbing", "跳痛/搏动感"),
                            QuestionOption("sharp", "刺痛/闪电感"),
                            QuestionOption("unclear", "说不清/变化不定")
                        )
                    )
                ),
                DailyQuestion(
                    id = "fu_headache_pattern",
                    title = "这个头痛持续多久了呀～",
                    prompt = "Kitty 想知道持续时间，还有是不是久坐或看屏幕后加重～",
                    step = DailyQuestionStep.FollowUp,
                    order = 101,
                    required = true,
                    kind = QuestionKind.SingleChoice(
                        options = listOf(
                            QuestionOption("lt1h", "<1 小时、偶发"),
                            QuestionOption("1_3h", "1-3 小时"),
                            QuestionOption("gt3h", ">3 小时或反复"),
                            QuestionOption("after_screen", "久坐/屏幕后加重")
                        )
                    )
                )
            )
        ),
        FollowUpRule(
            symptomQuestionId = "neck_back_intensity",
            focusOptionId = "neck_back",
            questions = listOf(
                DailyQuestion(
                    id = "fu_neck_trigger",
                    title = "脖子肩膀是什么引起的呀～",
                    prompt = "来一起找找今天的主要原因～",
                    step = DailyQuestionStep.FollowUp,
                    order = 110,
                    required = true,
                    kind = QuestionKind.SingleChoice(
                        options = listOf(
                            QuestionOption("posture", "久坐/低头或伏案"),
                            QuestionOption("sleep", "睡姿不佳/枕头不合适"),
                            QuestionOption("load", "搬重物/锻炼后"),
                            QuestionOption("none", "不确定或无明显诱因")
                        )
                    )
                ),
                DailyQuestion(
                    id = "fu_neck_radiation",
                    title = "有扩散到肩膀或手臂吗～",
                    prompt = "告诉 Kitty，这样能帮你判断是肌肉还是神经问题～",
                    step = DailyQuestionStep.FollowUp,
                    order = 111,
                    required = true,
                    kind = QuestionKind.SingleChoice(
                        options = listOf(
                            QuestionOption("shoulder", "牵涉到肩/背部"),
                            QuestionOption("arm", "手臂酸麻/无力"),
                            QuestionOption("stiff", "只有紧绷/僵硬"),
                            QuestionOption("none", "没有牵涉")
                        )
                    )
                )
            )
        ),
        FollowUpRule(
            symptomQuestionId = "stomach_intensity",
            focusOptionId = "stomach",
            questions = listOf(
                DailyQuestion(
                    id = "fu_stomach_pattern",
                    title = "胃是哪种不舒服呀～",
                    prompt = "让 Kitty 看看是反酸、胀痛还是刺痛～",
                    step = DailyQuestionStep.FollowUp,
                    order = 120,
                    required = true,
                    kind = QuestionKind.SingleChoice(
                        options = listOf(
                            QuestionOption("acid", "反酸/烧心"),
                            QuestionOption("bloat", "胀痛/顶住感"),
                            QuestionOption("sharp", "刺痛/痉挛"),
                            QuestionOption("nausea", "伴恶心/想吐")
                        )
                    )
                ),
                DailyQuestion(
                    id = "fu_stomach_trigger",
                    title = "什么时候更容易不舒服呀～",
                    prompt = "选一个最接近的情况～",
                    step = DailyQuestionStep.FollowUp,
                    order = 121,
                    required = true,
                    kind = QuestionKind.SingleChoice(
                        options = listOf(
                            QuestionOption("late_meal", "晚餐过晚/过饱"),
                            QuestionOption("spicy", "辛辣/油炸/酒精后"),
                            QuestionOption("coffee", "咖啡/浓茶后"),
                            QuestionOption("empty", "空腹时明显"),
                            QuestionOption("none", "无明显规律")
                        )
                    )
                )
            )
        ),
        FollowUpRule(
            symptomQuestionId = "nasal_intensity",
            focusOptionId = "nasal",
            extraPredicate = { answers ->
                (answers["chill_exposure"] as? DailyAnswerPayload.Choice)?.optionId == "yes"
            },
            questions = listOf(
                DailyQuestion(
                    id = "fu_nasal_main",
                    title = "鼻子喉咙主要是什么感觉～",
                    prompt = "Kitty 帮你判断是过敏、受凉还是感染～",
                    step = DailyQuestionStep.FollowUp,
                    order = 130,
                    required = true,
                    kind = QuestionKind.SingleChoice(
                        options = listOf(
                            QuestionOption("congestion", "鼻塞为主/呼吸不畅"),
                            QuestionOption("runny", "流涕/打喷嚏"),
                            QuestionOption("throat", "咽痒/异物感"),
                            QuestionOption("colored", "分泌物颜色变深/黏稠")
                        )
                    )
                ),
                DailyQuestion(
                    id = "fu_nasal_trigger",
                    title = "可能是什么引起的呀～",
                    prompt = "Kitty 来帮你确定是不是受凉或过敏～",
                    step = DailyQuestionStep.FollowUp,
                    order = 131,
                    required = true,
                    kind = QuestionKind.SingleChoice(
                        options = listOf(
                            QuestionOption("cold", "受凉/空调直吹后"),
                            QuestionOption("allergy", "过敏季/粉尘/宠物"),
                            QuestionOption("infection", "伴低热/乏力，像感冒"),
                            QuestionOption("unknown", "不确定")
                        )
                    )
                )
            )
        ),
        FollowUpRule(
            symptomQuestionId = "knee_intensity",
            focusOptionId = "knee",
            questions = listOf(
                DailyQuestion(
                    id = "fu_knee_trigger",
                    title = "什么时候膝盖更不舒服呀～",
                    prompt = "Kitty 帮你找找膝盖加重的场景～",
                    step = DailyQuestionStep.FollowUp,
                    order = 140,
                    required = true,
                    kind = QuestionKind.SingleChoice(
                        options = listOf(
                            QuestionOption("stairs", "上下楼/蹲起后"),
                            QuestionOption("sport", "跑跳/训练后"),
                            QuestionOption("cold", "天气变冷/受凉后"),
                            QuestionOption("sit", "久坐后僵硬")
                        )
                    )
                ),
                DailyQuestion(
                    id = "fu_knee_status",
                    title = "膝盖有肿胀或弹响吗～",
                    prompt = "Kitty 想知道有没有积液或不稳定感～",
                    step = DailyQuestionStep.FollowUp,
                    order = 141,
                    required = true,
                    kind = QuestionKind.SingleChoice(
                        options = listOf(
                            QuestionOption("swelling", "有肿胀/积液感"),
                            QuestionOption("click", "有弹响/不稳/打软腿"),
                            QuestionOption("sore", "只有酸胀/无肿胀"),
                            QuestionOption("none", "没有上述情况")
                        )
                    )
                )
            )
        ),
        FollowUpRule(
            symptomQuestionId = "mood_irritability",
            focusOptionId = "emotion",
            questions = listOf(
                DailyQuestion(
                    id = "fu_emotion_trigger",
                    title = "今天情绪波动主要和什么有关？",
                    prompt = "选最贴近的一项，我会给你更针对的建议。",
                    step = DailyQuestionStep.FollowUp,
                    order = 150,
                    required = true,
                    kind = QuestionKind.SingleChoice(
                        options = listOf(
                            QuestionOption("work_study", "工作/学习压力"),
                            QuestionOption("sleep_related", "睡眠不足或作息紊乱"),
                            QuestionOption("body_discomfort", "身体不适带来的烦躁"),
                            QuestionOption("social", "人际/家庭因素"),
                            QuestionOption("unknown", "说不清，综合因素")
                        )
                    )
                ),
                DailyQuestion(
                    id = "fu_emotion_relief",
                    title = "今天有没有什么方式让你缓解一些？",
                    prompt = "我想知道哪些方法对你更有效。",
                    step = DailyQuestionStep.FollowUp,
                    order = 151,
                    required = true,
                    kind = QuestionKind.SingleChoice(
                        options = listOf(
                            QuestionOption("walk_breath", "散步/深呼吸后好些"),
                            QuestionOption("rest_music", "休息/听音乐后好些"),
                            QuestionOption("chat_support", "聊天/倾诉后好些"),
                            QuestionOption("still_bad", "效果不明显"),
                            QuestionOption("not_try", "还没尝试")
                        )
                    )
                )
            )
        ),
        FollowUpRule(
            symptomQuestionId = "sleep_duration",
            focusOptionId = "sleep",
            extraPredicate = { answers ->
                (answers["sleep_duration"] as? DailyAnswerPayload.Choice)?.optionId == "fragmented"
            },
            questions = listOf(
                DailyQuestion(
                    id = "fu_sleep_issue",
                    title = "睡眠最主要的问题是哪个？",
                    prompt = "帮助我判断是入睡、维持还是醒后疲惫。",
                    step = DailyQuestionStep.FollowUp,
                    order = 160,
                    required = true,
                    kind = QuestionKind.SingleChoice(
                        options = listOf(
                            QuestionOption("hard_to_sleep", "入睡困难"),
                            QuestionOption("wake_up", "夜间易醒/多梦"),
                            QuestionOption("early_wake", "醒得太早"),
                            QuestionOption("not_rested", "睡了但不解乏"),
                            QuestionOption("unclear", "不确定")
                        )
                    )
                ),
                DailyQuestion(
                    id = "fu_sleep_hygiene",
                    title = "昨晚哪些因素可能影响了睡眠？",
                    prompt = "选最相关的一项就可以。",
                    step = DailyQuestionStep.FollowUp,
                    order = 161,
                    required = true,
                    kind = QuestionKind.SingleChoice(
                        options = listOf(
                            QuestionOption("late_screen", "睡前看屏幕较久"),
                            QuestionOption("late_meal", "晚餐太晚或太饱"),
                            QuestionOption("caffeine", "咖啡/茶/功能饮料"),
                            QuestionOption("stress", "压力或思虑多"),
                            QuestionOption("none", "无明显影响因素")
                        )
                    )
                )
            )
        ),
        FollowUpRule(
            symptomQuestionId = "nasal_intensity",
            focusOptionId = "chest",
            questions = listOf(
                DailyQuestion(
                    id = "fu_chest_pattern",
                    title = "胸闷或呼吸不适主要在什么情况下出现？",
                    prompt = "用来区分活动相关还是静息相关不适。",
                    step = DailyQuestionStep.FollowUp,
                    order = 170,
                    required = true,
                    kind = QuestionKind.SingleChoice(
                        options = listOf(
                            QuestionOption("activity", "活动后更明显"),
                            QuestionOption("rest", "静息时也明显"),
                            QuestionOption("anxiety", "紧张焦虑时加重"),
                            QuestionOption("night", "夜间/清晨更明显"),
                            QuestionOption("unclear", "不固定")
                        )
                    )
                ),
                DailyQuestion(
                    id = "fu_chest_companion",
                    title = "是否伴随以下表现？",
                    prompt = "请选最接近你当下感受的一项。",
                    step = DailyQuestionStep.FollowUp,
                    order = 171,
                    required = true,
                    kind = QuestionKind.SingleChoice(
                        options = listOf(
                            QuestionOption("palpitation", "心悸/心跳快"),
                            QuestionOption("cough", "咳嗽或痰多"),
                            QuestionOption("pain", "胸前区疼痛"),
                            QuestionOption("dizzy", "头晕乏力"),
                            QuestionOption("none", "无明显伴随症状")
                        )
                    )
                )
            )
        ),
        FollowUpRule(
            symptomQuestionId = "nasal_intensity",
            focusOptionId = "skin",
            questions = listOf(
                DailyQuestion(
                    id = "fu_skin_pattern",
                    title = "皮肤不适更接近哪一种？",
                    prompt = "帮助我判断是否和过敏或刺激相关。",
                    step = DailyQuestionStep.FollowUp,
                    order = 180,
                    required = true,
                    kind = QuestionKind.SingleChoice(
                        options = listOf(
                            QuestionOption("itch", "瘙痒为主"),
                            QuestionOption("rash", "泛红/皮疹"),
                            QuestionOption("dry", "干燥脱屑"),
                            QuestionOption("sting", "刺痛/灼热"),
                            QuestionOption("mixed", "多种情况并存")
                        )
                    )
                ),
                DailyQuestion(
                    id = "fu_skin_trigger",
                    title = "今天是否接触了新的刺激因素？",
                    prompt = "如护肤品、清洁剂、花粉、粉尘等。",
                    step = DailyQuestionStep.FollowUp,
                    order = 181,
                    required = true,
                    kind = QuestionKind.SingleChoice(
                        options = listOf(
                            QuestionOption("new_product", "新产品/新材质"),
                            QuestionOption("environment", "环境刺激（花粉粉尘）"),
                            QuestionOption("food", "饮食后出现"),
                            QuestionOption("unknown", "不确定"),
                            QuestionOption("none", "无明显诱因")
                        )
                    )
                )
            )
        ),
        FollowUpRule(
            symptomQuestionId = "headache_intensity",
            focusOptionId = "eyes",
            questions = listOf(
                DailyQuestion(
                    id = "fu_eyes_pattern",
                    title = "眼部不适主要是哪种表现？",
                    prompt = "帮你区分视疲劳、干眼或刺激反应。",
                    step = DailyQuestionStep.FollowUp,
                    order = 190,
                    required = true,
                    kind = QuestionKind.SingleChoice(
                        options = listOf(
                            QuestionOption("dry", "干涩/异物感"),
                            QuestionOption("sore", "酸胀/疲劳"),
                            QuestionOption("blur", "视物模糊"),
                            QuestionOption("light", "畏光/流泪"),
                            QuestionOption("mixed", "多种表现")
                        )
                    )
                ),
                DailyQuestion(
                    id = "fu_eyes_trigger",
                    title = "是否与用眼时长相关？",
                    prompt = "比如长时间看屏幕、夜间用眼等。",
                    step = DailyQuestionStep.FollowUp,
                    order = 191,
                    required = true,
                    kind = QuestionKind.SingleChoice(
                        options = listOf(
                            QuestionOption("screen", "长时间看屏幕后加重"),
                            QuestionOption("night", "夜间用眼后明显"),
                            QuestionOption("wind", "风吹/空调后明显"),
                            QuestionOption("none", "相关性不明显"),
                            QuestionOption("unclear", "不确定")
                        )
                    )
                )
            )
        ),
        FollowUpRule(
            symptomQuestionId = "mood_irritability",
            focusOptionId = "fatigue",
            questions = listOf(
                DailyQuestion(
                    id = "fu_fatigue_timing",
                    title = "乏力在一天中哪个时段最明显？",
                    prompt = "有助于判断作息和节律相关性。",
                    step = DailyQuestionStep.FollowUp,
                    order = 200,
                    required = true,
                    kind = QuestionKind.SingleChoice(
                        options = listOf(
                            QuestionOption("morning", "起床后就明显"),
                            QuestionOption("noon", "中午到下午明显"),
                            QuestionOption("evening", "傍晚/晚上明显"),
                            QuestionOption("all_day", "全天都明显"),
                            QuestionOption("random", "不固定")
                        )
                    )
                ),
                DailyQuestion(
                    id = "fu_fatigue_companion",
                    title = "乏力通常伴随什么情况？",
                    prompt = "选最常见的伴随表现。",
                    step = DailyQuestionStep.FollowUp,
                    order = 201,
                    required = true,
                    kind = QuestionKind.SingleChoice(
                        options = listOf(
                            QuestionOption("sleepy", "嗜睡/注意力差"),
                            QuestionOption("dizzy", "头晕/站立无力"),
                            QuestionOption("appetite", "食欲下降"),
                            QuestionOption("stress", "压力大、恢复慢"),
                            QuestionOption("none", "无明显伴随")
                        )
                    )
                )
            )
        )
    )

    fun evaluate(
        answers: Map<String, DailyAnswerPayload>,
        trends: Map<String, TrendFlag>
    ): List<DailyQuestion> {
        val overallFeeling = (answers["overall_feeling"] as? DailyAnswerPayload.Choice)?.optionId
        val priorities = (answers["focus_priority"] as? DailyAnswerPayload.MultiChoice)?.optionIds ?: emptySet()
        val severityByQuestion = extractSliderValues(answers)
        val result = LinkedHashMap<String, DailyQuestion>()
        val overallSevere = overallFeeling == "awful"

        rules.forEach { rule ->
            val severityHit = (severityByQuestion[rule.symptomQuestionId] ?: 0) >= SEVERITY_THRESHOLD
            val trendHit = trends[rule.symptomQuestionId] == TrendFlag.rising
            val focusHit = rule.focusOptionId != null && priorities.contains(rule.focusOptionId)
            val extraHit = rule.extraPredicate?.invoke(answers) ?: false
            if (severityHit || trendHit || focusHit || overallSevere || extraHit) {
                rule.questions.forEach { q ->
                    if (result.size < MAX_QUESTIONS) {
                        result.putIfAbsent(q.id, q)
                    }
                }
            }
        }
        return result.values.toList()
    }

    private fun extractSliderValues(answers: Map<String, DailyAnswerPayload>): Map<String, Int> {
        val values = mutableMapOf<String, Int>()
        answers.forEach { (id, payload) ->
            val slider = (payload as? DailyAnswerPayload.Slider)?.value
            if (slider != null) {
                values[id] = slider
            }
        }
        return values
    }

    fun findQuestionById(id: String): DailyQuestion? =
        rules.asSequence().flatMap { it.questions.asSequence() }.firstOrNull { it.id == id }
}

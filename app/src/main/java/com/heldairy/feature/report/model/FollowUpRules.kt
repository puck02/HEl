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
                    title = "头痛特点",
                    prompt = "帮我判断是紧张型还是偏头痛。",
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
                    title = "持续与诱因",
                    prompt = "想了解它持续多久、是否久坐/屏幕后加重。",
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
                    title = "颈肩诱因",
                    prompt = "找找今天的主要诱因。",
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
                    title = "放射与麻木",
                    prompt = "有无牵涉到肩/手臂，方便判断肌肉还是神经。",
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
                    title = "胃部不适特点",
                    prompt = "看看是反酸、胀痛还是刺痛。",
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
                    title = "进食与作息",
                    prompt = "哪种情况更容易触发？",
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
                    title = "鼻咽主要表现",
                    prompt = "方便判断是过敏、受凉还是感染。",
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
                    title = "可能诱因",
                    prompt = "帮我确定是否受凉或过敏。",
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
                    title = "膝盖加重场景",
                    prompt = "找找今天膝盖更不适的场景。",
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
                    title = "是否肿胀/弹响",
                    prompt = "了解是否有积液或不稳定感。",
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

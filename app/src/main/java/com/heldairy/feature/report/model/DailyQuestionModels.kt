package com.heldairy.feature.report.model

enum class DailyQuestionStep(val index: Int, val title: String, val subtitle: String) {
    Greeting(0, "Step 0 · 问候", "先了解你今天的整体感受"),
    Baseline(1, "Step 1 · 基础记录", "用 1～2 分钟完成今日基础数据"),
    FollowUp(2, "Step 2 · 追问", "根据症状补充几个问题")
}

data class QuestionOption(
    val id: String,
    val label: String,
    val helper: String? = null
)

sealed interface QuestionKind {
    data class SingleChoice(val options: List<QuestionOption>) : QuestionKind
    data class MultipleChoice(
        val options: List<QuestionOption>,
        val maxSelection: Int,
        val helper: String? = null
    ) : QuestionKind
    data class Slider(
        val valueRange: IntRange,
        val defaultValue: Int = valueRange.first,
        val valueSuffix: String? = null,
        val supportingText: String? = null
    ) : QuestionKind

    data class TextInput(
        val hint: String,
        val maxLength: Int,
        val supportingText: String? = null
    ) : QuestionKind
}

data class DailyQuestion(
    val id: String,
    val title: String,
    val prompt: String,
    val step: DailyQuestionStep,
    val order: Int,
    val required: Boolean,
    val kind: QuestionKind
)

object DailyQuestionBank {
    val questions: List<DailyQuestion> = listOf(
        DailyQuestion(
            id = "overall_feeling",
            title = "今天整体状态怎么样呀～",
            prompt = "按第一直觉选择就好，我会根据你的状态动态调整追问。",
            step = DailyQuestionStep.Greeting,
            order = 0,
            required = true,
            kind = QuestionKind.SingleChoice(
                options = listOf(
                    QuestionOption("great", "状态很好"),
                    QuestionOption("ok", "还可以"),
                    QuestionOption("unwell", "有些不适"),
                    QuestionOption("awful", "明显不舒服")
                )
            )
        ),
        DailyQuestion(
            id = "focus_priority",
            title = "今天想让 Kitty 特别关注哪里呀？",
            prompt = "常见身体不适部位都可以选，我会优先围绕你选中的区域追问。",
            step = DailyQuestionStep.Greeting,
            order = 1,
            required = true,
            kind = QuestionKind.MultipleChoice(
                options = listOf(
                    QuestionOption("head", "头部/面部不适"),
                    QuestionOption("nasal", "鼻咽/呼吸道"),
                    QuestionOption("neck_back", "颈肩/背腰"),
                    QuestionOption("knee", "关节/四肢"),
                    QuestionOption("stomach", "腹部/胃肠"),
                    QuestionOption("chest", "胸闷/心前区不适"),
                    QuestionOption("skin", "皮肤/过敏反应"),
                    QuestionOption("eyes", "眼睛疲劳/不适"),
                    QuestionOption("sleep", "睡眠"),
                    QuestionOption("emotion", "情绪/压力"),
                    QuestionOption("fatigue", "乏力/精力不足"),
                    QuestionOption("period", "经期相关"),
                    QuestionOption("none", "没有特别")
                ),
                maxSelection = 4,
                helper = "最多选择 4 个关注点"
            )
        ),
        DailyQuestion(
            id = "sleep_duration",
            title = "昨晚睡眠时长大概多久？",
            prompt = "按大概区间选就好，不需要精确到分钟。",
            step = DailyQuestionStep.Baseline,
            order = 2,
            required = true,
            kind = QuestionKind.SingleChoice(
                options = listOf(
                    QuestionOption("lt6", "少于 6 小时"),
                    QuestionOption("6_7", "6-7 小时"),
                    QuestionOption("7_8", "7-8 小时"),
                    QuestionOption("gt8", "多于 8 小时"),
                    QuestionOption("fragmented", "睡眠断续/质量较差")
                )
            )
        ),
        DailyQuestion(
            id = "nap_duration",
            title = "今天白天有小睡或闭目休息吗？",
            prompt = "没有休息就选【无】。",
            step = DailyQuestionStep.Baseline,
            order = 3,
            required = true,
            kind = QuestionKind.SingleChoice(
                options = listOf(
                    QuestionOption("none", "无"),
                    QuestionOption("lt30", "少于 30 分钟"),
                    QuestionOption("30_60", "30-60 分钟"),
                    QuestionOption("gt60", "多于 60 分钟"),
                    QuestionOption("break_only", "仅短暂放松未入睡")
                )
            )
        ),
        DailyQuestion(
            id = "daily_steps",
            title = "今天活动量大概如何？",
            prompt = "可按步数估计，或按体感选择最接近的区间。",
            step = DailyQuestionStep.Baseline,
            order = 4,
            required = true,
            kind = QuestionKind.SingleChoice(
                options = listOf(
                    QuestionOption("lt3k", "少于 3k"),
                    QuestionOption("3_6k", "3-6k"),
                    QuestionOption("6_10k", "6-10k"),
                    QuestionOption("gt10k", "多于 10k"),
                    QuestionOption("unknown", "不确定")
                )
            )
        ),
        DailyQuestion(
            id = "headache_intensity",
            title = "头部不适强度（头痛/头胀/头晕）",
            prompt = "0 表示完全没有，10 表示非常明显。",
            step = DailyQuestionStep.Baseline,
            order = 5,
            required = true,
            kind = QuestionKind.Slider(
                valueRange = 0..10,
                defaultValue = 0,
                valueSuffix = " / 10",
                supportingText = "轻轻拖动刻度即可记录强度"
            )
        ),
        DailyQuestion(
            id = "neck_back_intensity",
            title = "颈肩背腰不适强度",
            prompt = "包含僵硬、酸痛、牵拉感等。",
            step = DailyQuestionStep.Baseline,
            order = 6,
            required = true,
            kind = QuestionKind.Slider(
                valueRange = 0..10,
                defaultValue = 0,
                valueSuffix = " / 10"
            )
        ),
        DailyQuestion(
            id = "stomach_intensity",
            title = "腹部/胃肠不适强度",
            prompt = "如胃痛、腹胀、反酸、恶心等可综合评估。",
            step = DailyQuestionStep.Baseline,
            order = 7,
            required = true,
            kind = QuestionKind.Slider(
                valueRange = 0..10,
                defaultValue = 0,
                valueSuffix = " / 10"
            )
        ),
        DailyQuestion(
            id = "nasal_intensity",
            title = "鼻咽/呼吸道不适强度",
            prompt = "如鼻塞、流涕、咽痛、咳嗽、呼吸不畅等。",
            step = DailyQuestionStep.Baseline,
            order = 8,
            required = true,
            kind = QuestionKind.Slider(
                valueRange = 0..10,
                defaultValue = 0,
                valueSuffix = " / 10"
            )
        ),
        DailyQuestion(
            id = "knee_intensity",
            title = "关节/四肢不适强度",
            prompt = "膝盖、踝、手腕或肌肉酸痛都可按整体感受打分。",
            step = DailyQuestionStep.Baseline,
            order = 9,
            required = true,
            kind = QuestionKind.Slider(
                valueRange = 0..10,
                defaultValue = 0,
                valueSuffix = " / 10"
            )
        ),
        DailyQuestion(
            id = "mood_irritability",
            title = "情绪压力/烦躁强度",
            prompt = "0 是很平稳，10 是明显焦虑或压力很大。",
            step = DailyQuestionStep.Baseline,
            order = 10,
            required = true,
            kind = QuestionKind.Slider(
                valueRange = 0..10,
                defaultValue = 0,
                valueSuffix = " / 10"
            )
        ),
        DailyQuestion(
            id = "chill_exposure",
            title = "今天有明显外界刺激吗？",
            prompt = "如受凉、淋雨、温差大、空调直吹等。",
            step = DailyQuestionStep.Baseline,
            order = 11,
            required = true,
            kind = QuestionKind.SingleChoice(
                options = listOf(
                    QuestionOption("yes", "有"),
                    QuestionOption("no", "没有"),
                    QuestionOption("unsure", "不确定")
                )
            )
        ),
        DailyQuestion(
            id = "medication_adherence",
            title = "今天的用药/补充剂执行情况如何？",
            prompt = "包含处方药、OTC、保健补充剂等。",
            step = DailyQuestionStep.Baseline,
            order = 12,
            required = true,
            kind = QuestionKind.SingleChoice(
                options = listOf(
                    QuestionOption("on_time", "按时"),
                    QuestionOption("missed", "有遗漏"),
                    QuestionOption("na", "无需/未用"),
                    QuestionOption("adjusted", "有临时调整")
                )
            )
        ),
        DailyQuestion(
            id = "menstrual_status",
            title = "是否有经期/激素相关不适？",
            prompt = "如不适用可选择【不适用】。",
            step = DailyQuestionStep.Baseline,
            order = 13,
            required = true,
            kind = QuestionKind.SingleChoice(
                options = listOf(
                    QuestionOption("period", "经期"),
                    QuestionOption("non_period", "非经期"),
                    QuestionOption("irregular", "有异常"),
                    QuestionOption("na", "不适用")
                )
            )
        ),
        DailyQuestion(
            id = "daily_notes",
            title = "还有什么补充信息想告诉 Kitty？",
            prompt = "可记录诱因、缓解方式、药物变化、就医计划等。",
            step = DailyQuestionStep.Baseline,
            order = 14,
            required = false,
            kind = QuestionKind.TextInput(
                hint = "可留空",
                maxLength = 240,
                supportingText = "最多 240 字"
            )
        )
    )
}

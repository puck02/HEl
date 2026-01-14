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
            title = "今天整体感觉如何？",
            prompt = "我会根据你的状态调整今天的问题数量。",
            step = DailyQuestionStep.Greeting,
            order = 0,
            required = true,
            kind = QuestionKind.SingleChoice(
                options = listOf(
                    QuestionOption("great", "很好"),
                    QuestionOption("ok", "还行"),
                    QuestionOption("unwell", "有点不舒服"),
                    QuestionOption("awful", "很难受")
                )
            )
        ),
        DailyQuestion(
            id = "focus_priority",
            title = "今天最希望关注哪里？",
            prompt = "我可以把更多注意力放在你指定的部位或主题。",
            step = DailyQuestionStep.Greeting,
            order = 1,
            required = true,
            kind = QuestionKind.MultipleChoice(
                options = listOf(
                    QuestionOption("head", "头痛"),
                    QuestionOption("neck_back", "颈肩腰"),
                    QuestionOption("stomach", "胃"),
                    QuestionOption("nasal", "鼻咽"),
                    QuestionOption("knee", "膝盖"),
                    QuestionOption("emotion", "情绪"),
                    QuestionOption("sleep", "睡眠"),
                    QuestionOption("period", "经期"),
                    QuestionOption("none", "没有特别")
                ),
                maxSelection = 3,
                helper = "最多选择 3 个关注点"
            )
        ),
        DailyQuestion(
            id = "sleep_duration",
            title = "昨晚睡了多久？",
            prompt = "主观感受即可。",
            step = DailyQuestionStep.Baseline,
            order = 2,
            required = true,
            kind = QuestionKind.SingleChoice(
                options = listOf(
                    QuestionOption("lt6", "少于 6 小时"),
                    QuestionOption("6_7", "6-7 小时"),
                    QuestionOption("7_8", "7-8 小时"),
                    QuestionOption("gt8", "多于 8 小时")
                )
            )
        ),
        DailyQuestion(
            id = "nap_duration",
            title = "午休情况",
            prompt = "没有午休就选“无”。",
            step = DailyQuestionStep.Baseline,
            order = 3,
            required = true,
            kind = QuestionKind.SingleChoice(
                options = listOf(
                    QuestionOption("none", "无"),
                    QuestionOption("lt30", "少于 30 分钟"),
                    QuestionOption("30_60", "30-60 分钟"),
                    QuestionOption("gt60", "多于 60 分钟")
                )
            )
        ),
        DailyQuestion(
            id = "daily_steps",
            title = "今天步数大约多少？",
            prompt = "估算即可，不需要精确。",
            step = DailyQuestionStep.Baseline,
            order = 4,
            required = true,
            kind = QuestionKind.SingleChoice(
                options = listOf(
                    QuestionOption("lt3k", "少于 3k"),
                    QuestionOption("3_6k", "3-6k"),
                    QuestionOption("6_10k", "6-10k"),
                    QuestionOption("gt10k", "多于 10k")
                )
            )
        ),
        DailyQuestion(
            id = "headache_intensity",
            title = "头痛强度",
            prompt = "0 表示没有不适，10 表示非常明显。",
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
            title = "颈肩腰紧张/疼痛",
            prompt = "0-10 自评。",
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
            title = "胃部不适",
            prompt = "如果今天特别平稳，也可以记录 0。",
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
            title = "鼻咽不适",
            prompt = "包括干痒、堵塞、喉咙异物感等。",
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
            title = "膝盖不适",
            prompt = "运动或天气变化带来的酸胀，也可以写在补充说明。",
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
            title = "情绪烦躁程度",
            prompt = "0 代表平稳，10 代表特别烦躁。",
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
            title = "今天有没有受凉？",
            prompt = "比如空调直吹、湿冷环境等。",
            step = DailyQuestionStep.Baseline,
            order = 11,
            required = true,
            kind = QuestionKind.SingleChoice(
                options = listOf(
                    QuestionOption("yes", "有"),
                    QuestionOption("no", "没有")
                )
            )
        ),
        DailyQuestion(
            id = "medication_adherence",
            title = "用药是否按计划？",
            prompt = "没有用药计划就选“无需”。",
            step = DailyQuestionStep.Baseline,
            order = 12,
            required = true,
            kind = QuestionKind.SingleChoice(
                options = listOf(
                    QuestionOption("on_time", "按时"),
                    QuestionOption("missed", "有遗漏"),
                    QuestionOption("na", "无需/未用")
                )
            )
        ),
        DailyQuestion(
            id = "menstrual_status",
            title = "经期状态",
            prompt = "如果不适用，直接选择“非经期”。",
            step = DailyQuestionStep.Baseline,
            order = 13,
            required = true,
            kind = QuestionKind.SingleChoice(
                options = listOf(
                    QuestionOption("period", "经期"),
                    QuestionOption("non_period", "非经期"),
                    QuestionOption("irregular", "有异常")
                )
            )
        ),
        DailyQuestion(
            id = "daily_notes",
            title = "今日补充说明",
            prompt = "想记的细节、药物调整或明天的计划都可以写。",
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

package com.heldairy.feature.report.model

sealed interface DailyAnswerPayload {
    data class Choice(val optionId: String) : DailyAnswerPayload
    data class MultiChoice(val optionIds: Set<String>) : DailyAnswerPayload
    data class Slider(val value: Int) : DailyAnswerPayload
    data class Text(val value: String) : DailyAnswerPayload
}

package com.heldairy.feature.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.heldairy.HElDairyApplication
import com.heldairy.core.data.DailyAnswerRecord
import com.heldairy.core.data.DailyReportRepository
import com.heldairy.feature.report.model.DailyAnswerPayload
import com.heldairy.feature.report.model.DailyQuestion
import com.heldairy.feature.report.model.DailyQuestionBank
import com.heldairy.feature.report.model.DailyQuestionStep
import com.heldairy.feature.report.model.QuestionKind
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class DailyReportViewModel(
    private val repository: DailyReportRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    private val questions = DailyQuestionBank.questions.sortedBy { it.order }
    private val questionById = questions.associateBy { it.id }

    private val answers = MutableStateFlow<Map<String, DailyAnswerPayload>>(emptyMap())
    private val sliderDrafts = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val textDrafts = MutableStateFlow<Map<String, String>>(emptyMap())

    private val _uiState = MutableStateFlow(DailyReportUiState())
    val uiState: StateFlow<DailyReportUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DailyReportEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    init {
        refreshUi()
    }

    fun onOptionSelected(questionId: String, optionId: String) {
        answers.update { current -> current + (questionId to DailyAnswerPayload.Choice(optionId)) }
        refreshUi()
    }

    fun onSliderValueChanged(questionId: String, value: Float) {
        val question = questionById[questionId] ?: return
        val sliderKind = question.kind as? QuestionKind.Slider ?: return
        val coerced = value.roundToInt().coerceIn(sliderKind.valueRange.first, sliderKind.valueRange.last)
        sliderDrafts.update { current -> current + (questionId to coerced) }
        refreshUi()
    }

    fun onSliderValueChangeFinished(questionId: String) {
        val value = sliderDrafts.value[questionId] ?: return
        answers.update { current -> current + (questionId to DailyAnswerPayload.Slider(value)) }
        refreshUi()
    }

    fun onTextChanged(questionId: String, text: String) {
        val question = questionById[questionId] ?: return
        val textKind = question.kind as? QuestionKind.TextInput ?: return
        val clipped = text.take(textKind.maxLength)
        textDrafts.update { current -> current + (questionId to clipped) }
        answers.update { current ->
            if (clipped.isBlank()) {
                current - questionId
            } else {
                current + (questionId to DailyAnswerPayload.Text(clipped.trim()))
            }
        }
        refreshUi()
    }

    fun submitDailyReport() {
        val currentState = _uiState.value
        if (!currentState.canSubmit || currentState.isSaving) return
        val answersSnapshot = answers.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val now = Instant.now(clock)
            val entryDate = LocalDate.now(clock).toString()
            val timezoneId = clock.zone.id
            val answerRecords = questions.mapNotNull { question ->
                val answer = answersSnapshot[question.id] ?: return@mapNotNull null
                question.toAnswerRecord(answer)
            }
            val result = repository.recordDailyReport(
                entryDate = entryDate,
                timezoneId = timezoneId,
                answers = answerRecords,
                createdAtMillis = now.toEpochMilli()
            )
            result
                .onSuccess {
                    resetSession()
                    _events.emit(DailyReportEvent.Snackbar("今日基础记录已保存，感谢你的分享。"))
                }
                .onFailure { throwable ->
                    refreshUi(isSaving = false)
                    val message = throwable.message?.takeIf { it.isNotBlank() }
                        ?: "保存失败，请稍后重试。"
                    _events.emit(DailyReportEvent.Snackbar(message))
                }
        }
    }

    private fun resetSession() {
        answers.value = emptyMap()
        sliderDrafts.value = emptyMap()
        textDrafts.value = emptyMap()
        refreshUi(isSaving = false)
    }

    private fun refreshUi(isSaving: Boolean = _uiState.value.isSaving) {
        val questionStates = mutableListOf<QuestionUiState>()
        var allowNext = true
        val answersSnapshot = answers.value
        val sliderSnapshot = sliderDrafts.value
        val textSnapshot = textDrafts.value
        questions.forEach { question ->
            val answer = answersSnapshot[question.id]
            val isAnswered = isQuestionAnswered(question, answer)
            val isVisible = allowNext
            val uiState = when (val kind = question.kind) {
                is QuestionKind.SingleChoice -> {
                    val selected = (answer as? DailyAnswerPayload.Choice)?.optionId
                    QuestionUiState.SingleChoice(
                        question = question,
                        isVisible = isVisible,
                        isAnswered = isAnswered,
                        selectedOptionId = selected
                    )
                }
                is QuestionKind.Slider -> {
                    val committed = (answer as? DailyAnswerPayload.Slider)?.value
                    val draft = sliderSnapshot[question.id]
                    val currentValue = committed ?: draft ?: kind.defaultValue
                    QuestionUiState.Slider(
                        question = question,
                        isVisible = isVisible,
                        isAnswered = isAnswered,
                        currentValue = currentValue
                    )
                }
                is QuestionKind.TextInput -> {
                    val committed = (answer as? DailyAnswerPayload.Text)?.value
                    val draft = textSnapshot[question.id]
                    val textValue = committed ?: draft.orEmpty()
                    QuestionUiState.TextInput(
                        question = question,
                        isVisible = isVisible,
                        isAnswered = isAnswered,
                        text = textValue
                    )
                }
            }
            questionStates.add(uiState)
            allowNext = isVisible && (isAnswered || !question.required)
        }
        val stepProgress = DailyQuestionStep.entries.map { step ->
            val requiredQuestions = questionStates.filter { it.question.step == step && it.question.required }
            StepProgress(
                step = step,
                isComplete = requiredQuestions.all { it.isAnswered }
            )
        }
        val canSubmit = questionStates.filter { it.question.required }.all { it.isAnswered }
        _uiState.value = DailyReportUiState(
            questions = questionStates,
            stepProgress = stepProgress,
            canSubmit = canSubmit,
            isSaving = isSaving
        )
    }

    private fun isQuestionAnswered(question: DailyQuestion, answer: DailyAnswerPayload?): Boolean {
        if (answer == null) {
            return !question.required
        }
        return when (question.kind) {
            is QuestionKind.SingleChoice -> (answer as? DailyAnswerPayload.Choice) != null
            is QuestionKind.Slider -> (answer as? DailyAnswerPayload.Slider) != null
            is QuestionKind.TextInput -> {
                val textValue = (answer as? DailyAnswerPayload.Text)?.value.orEmpty()
                textValue.isNotBlank()
            }
        }
    }

    private fun DailyQuestion.toAnswerRecord(answer: DailyAnswerPayload): DailyAnswerRecord? {
        return when (val kind = kind) {
            is QuestionKind.SingleChoice -> {
                val choiceAnswer = answer as? DailyAnswerPayload.Choice ?: return null
                val option = kind.options.firstOrNull { it.id == choiceAnswer.optionId } ?: return null
                DailyAnswerRecord(
                    questionId = id,
                    stepIndex = step.index,
                    order = order,
                    answerType = "single_choice",
                    answerValue = option.id,
                    answerLabel = option.label,
                    metadataJson = option.helper
                )
            }

            is QuestionKind.Slider -> {
                val sliderAnswer = answer as? DailyAnswerPayload.Slider ?: return null
                DailyAnswerRecord(
                    questionId = id,
                    stepIndex = step.index,
                    order = order,
                    answerType = "slider",
                    answerValue = sliderAnswer.value.toString(),
                    answerLabel = sliderAnswer.value.toString() + (kind.valueSuffix ?: "")
                )
            }

            is QuestionKind.TextInput -> {
                val textAnswer = (answer as? DailyAnswerPayload.Text)?.value?.takeIf { it.isNotBlank() }
                    ?: return null
                DailyAnswerRecord(
                    questionId = id,
                    stepIndex = step.index,
                    order = order,
                    answerType = "text",
                    answerValue = textAnswer,
                    answerLabel = textAnswer
                )
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HElDairyApplication)
                DailyReportViewModel(app.container.dailyReportRepository)
            }
        }
    }
}

sealed interface QuestionUiState {
    val question: DailyQuestion
    val isVisible: Boolean
    val isAnswered: Boolean

    data class SingleChoice(
        override val question: DailyQuestion,
        override val isVisible: Boolean,
        override val isAnswered: Boolean,
        val selectedOptionId: String?
    ) : QuestionUiState

    data class Slider(
        override val question: DailyQuestion,
        override val isVisible: Boolean,
        override val isAnswered: Boolean,
        val currentValue: Int
    ) : QuestionUiState

    data class TextInput(
        override val question: DailyQuestion,
        override val isVisible: Boolean,
        override val isAnswered: Boolean,
        val text: String
    ) : QuestionUiState
}

data class StepProgress(
    val step: DailyQuestionStep,
    val isComplete: Boolean
)

data class DailyReportUiState(
    val questions: List<QuestionUiState> = emptyList(),
    val stepProgress: List<StepProgress> = emptyList(),
    val canSubmit: Boolean = false,
    val isSaving: Boolean = false
)

sealed interface DailyReportEvent {
    data class Snackbar(val message: String) : DailyReportEvent
}

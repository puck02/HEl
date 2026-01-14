package com.heldairy.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.heldairy.HElDairyApplication
import com.heldairy.core.data.DailyReportRepository
import com.heldairy.core.database.entity.DailyEntryWithResponses
import com.heldairy.core.database.entity.QuestionResponseEntity
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeDashboardViewModel(
    private val repository: DailyReportRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    val uiState: StateFlow<HomeDashboardUiState> = repository.latestEntry()
        .map { entry -> mapToState(entry) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeDashboardUiState()
        )

    private fun mapToState(entry: DailyEntryWithResponses?): HomeDashboardUiState {
        val today = LocalDate.now(clock).toString()
        if (entry == null || entry.entry.entryDate != today) {
            return HomeDashboardUiState()
        }
        val responses = entry.responses.associateBy { it.questionId }
        return HomeDashboardUiState(
            hasTodayEntry = true,
            steps = responses["daily_steps"]?.let(::mapSteps),
            sleep = responses["sleep_duration"]?.let(::mapSleep),
            mood = responses["mood_irritability"]?.let(::mapMood),
            energy = responses["overall_feeling"]?.let(::mapEnergy)
        )
    }

    private fun mapSteps(response: QuestionResponseEntity): MetricDisplay {
        val hint = when (response.answerValue) {
            "gt10k" -> "已达标"
            "6_10k" -> "接近目标"
            "3_6k" -> "再多走走"
            "lt3k" -> "需要活动"
            else -> null
        }
        val displayValue = response.answerLabel.ifBlank { response.answerValue }
        return MetricDisplay(value = displayValue, hint = hint)
    }

    private fun mapSleep(response: QuestionResponseEntity): MetricDisplay {
        val hint = when (response.answerValue) {
            "gt8" -> "充足"
            "7_8" -> "稳定"
            "6_7" -> "略短"
            "lt6" -> "偏少"
            else -> null
        }
        val value = response.answerLabel.ifBlank { response.answerValue }
        return MetricDisplay(value = value, hint = hint)
    }

    private fun mapMood(response: QuestionResponseEntity): MetricDisplay {
        val score = response.answerValue.toIntOrNull()
        val value = score?.let { "$it / 10" } ?: response.answerLabel.ifBlank { response.answerValue }
        val hint = when {
            score == null -> null
            score <= 3 -> "平稳"
            score <= 6 -> "略烦躁"
            else -> "明显紧绷"
        }
        return MetricDisplay(value = value, hint = hint)
    }

    private fun mapEnergy(response: QuestionResponseEntity): MetricDisplay {
        val hint = when (response.answerValue) {
            "great" -> "充沛"
            "ok" -> "正常"
            "unwell" -> "低迷"
            "awful" -> "需要休息"
            else -> null
        }
        val value = response.answerLabel.ifBlank { response.answerValue }
        return MetricDisplay(value = value, hint = hint)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HElDairyApplication)
                val container = app.container
                HomeDashboardViewModel(
                    repository = container.dailyReportRepository
                )
            }
        }
    }
}

data class HomeDashboardUiState(
    val hasTodayEntry: Boolean = false,
    val steps: MetricDisplay? = null,
    val sleep: MetricDisplay? = null,
    val mood: MetricDisplay? = null,
    val energy: MetricDisplay? = null
)

data class MetricDisplay(
    val value: String,
    val hint: String?
)

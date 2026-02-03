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
import com.heldairy.core.preferences.UserProfileStore
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeDashboardViewModel(
    private val repository: DailyReportRepository,
    userProfileStore: UserProfileStore,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    val uiState: StateFlow<HomeDashboardUiState> = combine(
        repository.latestEntry(),
        repository.entriesForRange(LocalDate.now(clock).minusDays(6), LocalDate.now(clock)),
        userProfileStore.profileFlow
    ) { entry, weeklyEntries, profile ->
        // 优化：只计算一次weeklyResponsesMap和last7Days
        val today = LocalDate.now(clock).toString()
        val last7Days = (0..6).map { LocalDate.now(clock).minusDays(it.toLong()).toString() }.reversed()
        val weeklyResponsesMap = weeklyEntries.associate { 
            it.entry.entryDate to it.responses.associateBy { r -> r.questionId } 
        }
        
        mapToState(entry, today, last7Days, weeklyResponsesMap).copy(
            userName = profile.userName, 
            avatarUri = profile.avatarUri
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeDashboardUiState()
    )

    private fun mapToState(
        entry: DailyEntryWithResponses?, 
        today: String,
        last7Days: List<String>,
        weeklyResponsesMap: Map<String, Map<String, QuestionResponseEntity>>
    ): HomeDashboardUiState {
        if (entry == null || entry.entry.entryDate != today) {
            return HomeDashboardUiState()
        }
        
        val responses = entry.responses.associateBy { it.questionId }
        
        return HomeDashboardUiState(
            hasTodayEntry = true,
            steps = responses["daily_steps"]?.let { mapSteps(it, last7Days, weeklyResponsesMap) },
            sleep = responses["sleep_duration"]?.let { mapSleep(it, last7Days, weeklyResponsesMap) },
            mood = responses["mood_irritability"]?.let { mapMood(it, last7Days, weeklyResponsesMap) },
            energy = responses["overall_feeling"]?.let { mapEnergy(it, last7Days, weeklyResponsesMap) }
        )
    }

    private fun mapSteps(response: QuestionResponseEntity, last7Days: List<String>, weeklyData: Map<String, Map<String, QuestionResponseEntity>>): MetricDisplay {
        val hint = when (response.answerValue) {
            "gt10k" -> "已达标"
            "6_10k" -> "接近目标"
            "3_6k" -> "再多走走"
            "lt3k" -> "需要活动"
            else -> null
        }
        val displayValue = response.answerLabel.ifBlank { response.answerValue }
        
        // 获取7天数据
        val weeklyValues = last7Days.map { date ->
            val value = weeklyData[date]?.get("daily_steps")?.answerValue
            when (value) {
                "gt10k" -> 1.0f
                "6_10k" -> 0.75f
                "3_6k" -> 0.5f
                "lt3k" -> 0.25f
                else -> 0f
            }
        }
        
        return MetricDisplay(value = displayValue, hint = hint, weeklyData = weeklyValues)
    }

    private fun mapSleep(response: QuestionResponseEntity, last7Days: List<String>, weeklyData: Map<String, Map<String, QuestionResponseEntity>>): MetricDisplay {
        val hint = when (response.answerValue) {
            "gt8" -> "充足"
            "7_8" -> "稳定"
            "6_7" -> "略短"
            "lt6" -> "偏少"
            else -> null
        }
        val value = response.answerLabel.ifBlank { response.answerValue }
        
        // 获取7天数据
        val weeklyValues = last7Days.map { date ->
            val val1 = weeklyData[date]?.get("sleep_duration")?.answerValue
            when (val1) {
                "gt8" -> 1.0f
                "7_8" -> 0.85f
                "6_7" -> 0.6f
                "lt6" -> 0.3f
                else -> 0f
            }
        }
        
        return MetricDisplay(value = value, hint = hint, weeklyData = weeklyValues)
    }

    private fun mapMood(response: QuestionResponseEntity, last7Days: List<String>, weeklyData: Map<String, Map<String, QuestionResponseEntity>>): MetricDisplay {
        val score = response.answerValue.toIntOrNull()
        val value = score?.let { "$it / 10" } ?: response.answerLabel.ifBlank { response.answerValue }
        val hint = when {
            score == null -> null
            score <= 3 -> "平稳"
            score <= 6 -> "略烦躁"
            else -> "明显紧绷"
        }
        
        // 获取7天数据（反转：10分最好=1.0，0分最差=0）
        val weeklyValues = last7Days.map { date ->
            val scoreValue = weeklyData[date]?.get("mood_irritability")?.answerValue?.toIntOrNull()
            if (scoreValue != null) {
                1.0f - (scoreValue / 10f)  // 反转，因为情绪值越低越好
            } else {
                0f
            }
        }
        
        return MetricDisplay(value = value, hint = hint, weeklyData = weeklyValues)
    }

    private fun mapEnergy(response: QuestionResponseEntity, last7Days: List<String>, weeklyData: Map<String, Map<String, QuestionResponseEntity>>): MetricDisplay {
        val hint = when (response.answerValue) {
            "great" -> "充沛"
            "ok" -> "正常"
            "unwell" -> "低迷"
            "awful" -> "需要休息"
            else -> null
        }
        val value = response.answerLabel.ifBlank { response.answerValue }
        
        // 获取7天数据
        val weeklyValues = last7Days.map { date ->
            val energyValue = weeklyData[date]?.get("overall_feeling")?.answerValue
            when (energyValue) {
                "great" -> 1.0f
                "ok" -> 0.7f
                "unwell" -> 0.4f
                "awful" -> 0.15f
                else -> 0f
            }
        }
        
        return MetricDisplay(value = value, hint = hint, weeklyData = weeklyValues)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HElDairyApplication)
                val container = app.appContainer
                HomeDashboardViewModel(
                    repository = container.dailyReportRepository,
                    userProfileStore = container.userProfileStore
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
    val energy: MetricDisplay? = null,
    val userName: String = "Kitty宝贝",
    val avatarUri: String? = null
)

data class MetricDisplay(
    val value: String,
    val hint: String?,
    val weeklyData: List<Float> = emptyList()  // 近7天数据用于柱状图
)

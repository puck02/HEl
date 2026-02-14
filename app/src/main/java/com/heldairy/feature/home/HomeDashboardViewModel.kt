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
            // 修复：今日心情从 overall_feeling 获取（很好/还行/不舒服/难受）
            mood = responses["overall_feeling"]?.let { mapMood(it, last7Days, weeklyResponsesMap) },
            // 修复：身体能量综合睡眠和运动计算
            energy = calculateEnergyScore(responses, last7Days, weeklyResponsesMap)
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
        // 从 overall_feeling 获取心情（great/ok/unwell/awful）
        val moodValue = response.answerValue
        val displayValue = response.answerLabel.ifBlank { response.answerValue }
        
        val hint = when (moodValue) {
            "great" -> "心情很好"
            "ok" -> "还不错"
            "unwell" -> "有点低落"
            "awful" -> "需要关怀"
            else -> null
        }
        
        // 获取7天数据（great=1.0最好，awful=0.15最差）
        val weeklyValues = last7Days.map { date ->
            val feelingValue = weeklyData[date]?.get("overall_feeling")?.answerValue
            when (feelingValue) {
                "great" -> 1.0f
                "ok" -> 0.7f
                "unwell" -> 0.4f
                "awful" -> 0.15f
                else -> 0f
            }
        }
        
        return MetricDisplay(value = displayValue, hint = hint, weeklyData = weeklyValues)
    }

    /**
     * 综合计算身体能量分数
     * 基于睡眠质量（60%）+ 运动量（40%）
     */
    private fun calculateEnergyScore(
        responses: Map<String, QuestionResponseEntity>,
        last7Days: List<String>,
        weeklyData: Map<String, Map<String, QuestionResponseEntity>>
    ): MetricDisplay? {
        val sleepResponse = responses["sleep_duration"] ?: return null
        val stepsResponse = responses["daily_steps"]
        
        // 计算睡眠得分 (0-100)
        val sleepScore = when (sleepResponse.answerValue) {
            "gt8" -> 100
            "7_8" -> 95
            "6_7" -> 70
            "lt6" -> 40
            else -> 50
        }
        
        // 计算运动得分 (0-100)
        val stepsScore = when (stepsResponse?.answerValue) {
            "gt10k" -> 100
            "6_10k" -> 80
            "3_6k" -> 60
            "lt3k" -> 30
            null -> 50  // 无运动数据时给中等分
            else -> 50
        }
        
        // 综合能量分数（睡眠60% + 运动40%）
        val totalScore = (sleepScore * 0.6 + stepsScore * 0.4).toInt()
        
        val hint = when {
            totalScore >= 90 -> "精力充沛"
            totalScore >= 75 -> "状态良好"
            totalScore >= 60 -> "略感疲惫"
            totalScore >= 45 -> "需要休息"
            else -> "严重不足"
        }
        
        // 获取7天数据（能量趋势）
        val weeklyValues = last7Days.map { date ->
            val sleepVal = weeklyData[date]?.get("sleep_duration")?.answerValue
            val stepsVal = weeklyData[date]?.get("daily_steps")?.answerValue
            
            val daySleepScore = when (sleepVal) {
                "gt8" -> 100
                "7_8" -> 95
                "6_7" -> 70
                "lt6" -> 40
                else -> 0
            }
            
            val dayStepsScore = when (stepsVal) {
                "gt10k" -> 100
                "6_10k" -> 80
                "3_6k" -> 60
                "lt3k" -> 30
                else -> 0
            }
            
            if (daySleepScore == 0 && dayStepsScore == 0) {
                0f
            } else {
                ((daySleepScore * 0.6 + dayStepsScore * 0.4) / 100).toFloat()
            }
        }
        
        return MetricDisplay(
            value = "$totalScore 分",
            hint = hint,
            weeklyData = weeklyValues
        )
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

@androidx.compose.runtime.Immutable
data class HomeDashboardUiState(
    val hasTodayEntry: Boolean = false,
    val steps: MetricDisplay? = null,
    val sleep: MetricDisplay? = null,
    val mood: MetricDisplay? = null,
    val energy: MetricDisplay? = null,
    val userName: String = "Kitty宝贝",
    val avatarUri: String? = null
)

@androidx.compose.runtime.Immutable
data class MetricDisplay(
    val value: String,
    val hint: String?,
    val weeklyData: List<Float> = emptyList()  // 近7天数据用于柱状图
)

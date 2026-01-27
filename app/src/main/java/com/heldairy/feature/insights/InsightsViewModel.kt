package com.heldairy.feature.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.heldairy.HElDairyApplication
import com.heldairy.core.data.InsightLocalSummary
import com.heldairy.core.data.InsightRepository
import com.heldairy.core.data.WeeklyInsightCoordinator
import com.heldairy.core.data.WeeklyInsightResult
import com.heldairy.core.data.WeeklyInsightStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class InsightWindowType { Seven, Thirty }

data class WeeklyInsightUi(
    val status: WeeklyInsightStatus = WeeklyInsightStatus.Pending,
    val result: WeeklyInsightResult? = null
)

data class InsightsUiState(
    val isLoading: Boolean = true,
    val selectedWindow: InsightWindowType = InsightWindowType.Seven,
    val summary: InsightLocalSummary? = null,
    val weeklyInsight: WeeklyInsightUi = WeeklyInsightUi(),
    val error: String? = null
)

class InsightsViewModel(
    private val insightRepository: InsightRepository,
    private val weeklyInsightCoordinator: WeeklyInsightCoordinator
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState

    init {
        refreshData()
    }

    fun selectWindow(type: InsightWindowType) {
        _uiState.update { it.copy(selectedWindow = type) }
    }

    fun refreshWeekly(force: Boolean = false) {
        viewModelScope.launch {
            val weekly = weeklyInsightCoordinator.getWeeklyInsight(force)
            _uiState.update { state ->
                state.copy(weeklyInsight = WeeklyInsightUi(status = weekly.status, result = weekly))
            }
        }
    }

    private fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val summary = insightRepository.buildLocalSummary()
                val weekly = weeklyInsightCoordinator.getWeeklyInsight()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        summary = summary,
                        weeklyInsight = WeeklyInsightUi(status = weekly.status, result = weekly)
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, error = throwable.message) }
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HElDairyApplication)
                val container = app.container
                InsightsViewModel(
                    insightRepository = container.insightRepository,
                    weeklyInsightCoordinator = container.weeklyInsightCoordinator
                )
            }
        }
    }
}

package com.heldairy.feature.medication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.heldairy.HElDairyApplication
import com.heldairy.feature.medication.ui.MedicationStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class MedFilterStatus {
    ALL, ACTIVE, PAUSED, ENDED
}

enum class MedSortBy {
    NAME, CREATED_TIME, START_DATE
}

data class MedicationListUiState(
    val meds: List<Med> = emptyList(),
    val displayedMeds: List<Med> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val filterStatus: MedFilterStatus = MedFilterStatus.ALL,
    val sortBy: MedSortBy = MedSortBy.CREATED_TIME,
    val stats: MedicationStats = MedicationStats(0, 0, 0, 0)
)

class MedicationListViewModel(
    private val repository: MedicationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedicationListUiState(isLoading = true))
    val uiState: StateFlow<MedicationListUiState> = _uiState.asStateFlow()

    init {
        loadMeds()
    }

    private fun loadMeds() {
        viewModelScope.launch {
            repository.getAllMeds().collect { allMeds ->
                _uiState.update { state ->
                    val filtered = applyFiltersAndSort(
                        meds = allMeds,
                        query = state.searchQuery,
                        filter = state.filterStatus,
                        sortBy = state.sortBy
                    )
                    val stats = calculateStats(allMeds)
                    state.copy(
                        meds = allMeds,
                        displayedMeds = filtered,
                        isLoading = false,
                        stats = stats
                    )
                }
            }
        }
    }

    private fun calculateStats(meds: List<Med>): MedicationStats {
        val active = meds.count { it.currentCourse?.status == CourseStatus.ACTIVE }
        val paused = meds.count { it.currentCourse?.status == CourseStatus.PAUSED }
        val ended = meds.count { 
            it.currentCourse?.status == CourseStatus.ENDED || it.currentCourse == null
        }
        return MedicationStats(
            total = meds.size,
            active = active,
            paused = paused,
            ended = ended
        )
    }

    private fun applyFiltersAndSort(
        meds: List<Med>,
        query: String,
        filter: MedFilterStatus,
        sortBy: MedSortBy
    ): List<Med> {
        var result = meds

        // 搜索过滤
        if (query.isNotBlank()) {
            result = result.filter { med ->
                med.name.contains(query, ignoreCase = true) ||
                med.aliases.orEmpty().contains(query, ignoreCase = true)
            }
        }

        // 状态过滤
        result = when (filter) {
            MedFilterStatus.ALL -> result
            MedFilterStatus.ACTIVE -> result.filter { it.currentCourse?.status == CourseStatus.ACTIVE }
            MedFilterStatus.PAUSED -> result.filter { it.currentCourse?.status == CourseStatus.PAUSED }
            MedFilterStatus.ENDED -> result.filter { 
                it.currentCourse?.status == CourseStatus.ENDED || it.currentCourse == null
            }
        }

        // 排序
        result = when (sortBy) {
            MedSortBy.NAME -> result.sortedBy { it.name }
            MedSortBy.CREATED_TIME -> result.sortedByDescending { it.createdAt }
            MedSortBy.START_DATE -> result.sortedByDescending { 
                it.currentCourse?.startDate ?: it.createdAt
            }
        }

        return result
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            val filtered = applyFiltersAndSort(
                meds = state.meds,
                query = query,
                filter = state.filterStatus,
                sortBy = state.sortBy
            )
            state.copy(searchQuery = query, displayedMeds = filtered)
        }
    }

    fun onFilterStatusChange(status: MedFilterStatus) {
        _uiState.update { state ->
            val filtered = applyFiltersAndSort(
                meds = state.meds,
                query = state.searchQuery,
                filter = status,
                sortBy = state.sortBy
            )
            state.copy(filterStatus = status, displayedMeds = filtered)
        }
    }

    fun onSortByChange(sortBy: MedSortBy) {
        _uiState.update { state ->
            val filtered = applyFiltersAndSort(
                meds = state.meds,
                query = state.searchQuery,
                filter = state.filterStatus,
                sortBy = sortBy
            )
            state.copy(sortBy = sortBy, displayedMeds = filtered)
        }
    }

    fun addNewMed(
        name: String,
        startDate: LocalDate,
        frequencyText: String,
        doseText: String? = null,
        timeHints: String? = null
    ) {
        viewModelScope.launch {
            val medId = repository.addMed(name = name)
            repository.addCourse(
                medId = medId,
                startDate = startDate,
                status = CourseStatus.ACTIVE,
                frequencyText = frequencyText,
                doseText = doseText,
                timeHints = timeHints
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HElDairyApplication
                val repository = MedicationRepository(application.appContainer.database.medicationDao())
                return MedicationListViewModel(repository) as T
            }
        }
    }
}

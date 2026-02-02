package com.heldairy.feature.medication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.heldairy.HElDairyApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MedicationListUiState(
    val meds: List<Med> = emptyList(),
    val isLoading: Boolean = false
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
            repository.getAllMeds().collect { meds ->
                _uiState.value = MedicationListUiState(
                    meds = meds,
                    isLoading = false
                )
            }
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

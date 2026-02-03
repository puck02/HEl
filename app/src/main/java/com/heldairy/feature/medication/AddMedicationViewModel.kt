package com.heldairy.feature.medication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.heldairy.HElDairyApplication
import com.heldairy.core.preferences.AiPreferencesStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class AddMedicationViewModel(
    private val repository: MedicationRepository,
    private val nlpParser: MedicationNlpParser,
    private val preferencesStore: AiPreferencesStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddUiState())
    val uiState: StateFlow<AddUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddEvent>()
    val events: SharedFlow<AddEvent> = _events.asSharedFlow()

    fun onNaturalInput(text: String) {
        _uiState.update { it.copy(naturalInput = text) }
    }

    fun parseNaturalInput() {
        val input = _uiState.value.naturalInput.trim()
        if (input.isBlank()) {
            viewModelScope.launch {
                _events.emit(AddEvent.ShowMessage("请输入药物描述"))
            }
            return
        }

        _uiState.update { it.copy(isParsing = true, parseError = null) }

        viewModelScope.launch {
            val settings = preferencesStore.currentSettings()
            val apiKey = settings.apiKey
            
            if (apiKey.isBlank()) {
                _uiState.update { it.copy(isParsing = false) }
                _events.emit(AddEvent.ShowMessage("请先在设置中配置 AI API Key"))
                return@launch
            }
            
            nlpParser.parseInput(apiKey, input).fold(
                onSuccess = { result ->
                    _uiState.update {
                        it.copy(
                            isParsing = false,
                            parseError = null,
                            name = result.name,
                            aliases = result.aliases.joinToString("，"),
                            frequency = result.frequency ?: "",
                            dose = result.dose ?: "",
                            timeHints = result.timeHints ?: "",
                            note = result.note ?: ""
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isParsing = false,
                            parseError = error.message ?: "解析失败"
                        )
                    }
                }
            )
        }
    }

    fun onNameChange(text: String) {
        _uiState.update { it.copy(name = text) }
    }

    fun onAliasesChange(text: String) {
        _uiState.update { it.copy(aliases = text) }
    }

    fun onFrequencyChange(text: String) {
        _uiState.update { it.copy(frequency = text) }
    }

    fun onDoseChange(text: String) {
        _uiState.update { it.copy(dose = text) }
    }

    fun onTimeHintsChange(text: String) {
        _uiState.update { it.copy(timeHints = text) }
    }

    fun onNoteChange(text: String) {
        _uiState.update { it.copy(note = text) }
    }

    fun onStartDateChange(date: LocalDate) {
        _uiState.update { it.copy(startDate = date) }
    }

    fun saveMedication() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            viewModelScope.launch {
                _events.emit(AddEvent.ShowMessage("药物名称不能为空"))
            }
            return
        }

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                // 检查药品是否已存在
                val existingMed = repository.getMedByName(state.name.trim())
                
                val medId = if (existingMed != null) {
                    // 药品已存在，检查是否有正在进行的疗程
                    if (existingMed.currentCourse?.status == CourseStatus.ACTIVE) {
                        // 有正在进行的疗程，需要询问用户
                        _uiState.update { it.copy(isSaving = false) }
                        _events.emit(AddEvent.ShowConfirmDialog(
                            existingMedId = existingMed.id,
                            existingCourseName = existingMed.name,
                            currentCourseId = existingMed.currentCourse.id
                        ))
                        return@launch
                    } else {
                        // 没有正在进行的疗程，直接复用
                        existingMed.id
                    }
                } else {
                    // 新药品，创建
                    repository.addMed(
                        name = state.name.trim(),
                        aliases = state.aliases.takeIf { it.isNotBlank() }?.trim(),
                        note = state.note.takeIf { it.isNotBlank() }?.trim()
                    )
                }
                
                // 如果填写了疗程信息，创建疗程
                if (state.frequency.isNotBlank()) {
                    repository.addCourse(
                        medId = medId,
                        startDate = state.startDate,
                        status = CourseStatus.ACTIVE,
                        frequencyText = state.frequency.trim(),
                        doseText = state.dose.takeIf { it.isNotBlank() }?.trim(),
                        timeHints = state.timeHints.takeIf { it.isNotBlank() }?.trim()
                    )
                }
                
                _events.emit(AddEvent.NavigateBack)
            } catch (e: Exception) {
                _events.emit(AddEvent.ShowMessage("保存失败：${e.message}"))
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    fun confirmEndCurrentAndStartNew(existingMedId: Long, currentCourseId: Long) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSaving = true) }
                
                // 结束当前疗程
                repository.updateCourseStatus(
                    courseId = currentCourseId,
                    status = CourseStatus.ENDED,
                    endDate = LocalDate.now()
                )
                
                // 创建新疗程
                val state = _uiState.value
                if (state.frequency.isNotBlank()) {
                    repository.addCourse(
                        medId = existingMedId,
                        startDate = state.startDate,
                        status = CourseStatus.ACTIVE,
                        frequencyText = state.frequency.trim(),
                        doseText = state.dose.takeIf { it.isNotBlank() }?.trim(),
                        timeHints = state.timeHints.takeIf { it.isNotBlank() }?.trim()
                    )
                }
                
                _events.emit(AddEvent.NavigateBack)
            } catch (e: Exception) {
                _events.emit(AddEvent.ShowMessage("保存失败：${e.message}"))
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    companion object {
        fun factory(
            repository: MedicationRepository,
            nlpParser: MedicationNlpParser,
            preferencesStore: AiPreferencesStore
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AddMedicationViewModel(repository, nlpParser, preferencesStore)
            }
        }
    }
}

data class AddUiState(
    val naturalInput: String = "",
    val isParsing: Boolean = false,
    val parseError: String? = null,
    val name: String = "",
    val aliases: String = "",
    val frequency: String = "",
    val dose: String = "",
    val timeHints: String = "",
    val note: String = "",
    val startDate: LocalDate = LocalDate.now(),
    val isSaving: Boolean = false
)

sealed interface AddEvent {
    data class ShowMessage(val message: String) : AddEvent
    data object NavigateBack : AddEvent
    data class ShowConfirmDialog(
        val existingMedId: Long,
        val existingCourseName: String,
        val currentCourseId: Long
    ) : AddEvent
}

package com.heldairy.feature.medication

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.heldairy.HElDairyApplication
import com.heldairy.core.preferences.AiPreferencesStore
import com.heldairy.feature.medication.reminder.ReminderScheduler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class MedicationDetailViewModel(
    private val repository: MedicationRepository,
    private val summaryGenerator: MedicationInfoSummaryGenerator,
    private val preferencesStore: AiPreferencesStore,
    private val context: Context,
    private val medId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DetailEvent>()
    val events: SharedFlow<DetailEvent> = _events.asSharedFlow()

    init {
        loadMed()
    }

    fun loadMed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val med = repository.getMedById(medId)
                if (med == null) {
                    _uiState.update { it.copy(isLoading = false, error = "药品不存在") }
                    return@launch
                }

                val courses = repository.getCoursesByMedId(medId).first()

                // 找到最近的疗程作为当前疗程（无论状态）
                // 优先级: ACTIVE > PAUSED > 最近的ENDED
                val activeCourse = courses.firstOrNull { it.status == CourseStatus.ACTIVE }
                val pausedCourse = courses.firstOrNull { it.status == CourseStatus.PAUSED }
                val latestEndedCourse = courses
                    .filter { it.status == CourseStatus.ENDED }
                    .maxByOrNull { it.endDate ?: it.startDate }
                
                val currentCourse = activeCourse ?: pausedCourse ?: latestEndedCourse
                
                // 历史疗程：除了当前疗程外的所有已结束疗程，按结束日期倒序
                val historyCourses = courses
                    .filter { it.status == CourseStatus.ENDED && it.id != currentCourse?.id }
                    .sortedByDescending { it.endDate ?: it.startDate }

                // Load reminders
                val reminders = repository.getRemindersByMedId(medId).first()

                _uiState.update {
                    it.copy(
                        med = med,
                        currentCourse = currentCourse,
                        historyCourses = historyCourses,
                        reminders = reminders,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun pauseCourse(courseId: Long) {
        viewModelScope.launch {
            try {
                repository.updateCourseStatus(
                    courseId,
                    CourseStatus.PAUSED,
                    endDate = LocalDate.now()
                )
                _events.emit(DetailEvent.ShowMessage("疗程已暂停"))
                loadMed()
            } catch (e: Exception) {
                _events.emit(DetailEvent.ShowMessage("暂停失败: ${e.message}"))
            }
        }
    }

    fun endCourse(courseId: Long) {
        viewModelScope.launch {
            try {
                repository.updateCourseStatus(
                    courseId,
                    CourseStatus.ENDED,
                    endDate = LocalDate.now()
                )
                _events.emit(DetailEvent.ShowMessage("疗程已结束"))
                loadMed()
            } catch (e: Exception) {
                _events.emit(DetailEvent.ShowMessage("结束失败: ${e.message}"))
            }
        }
    }

    fun resumeCourse(medId: Long, previousCourse: MedCourse) {
        viewModelScope.launch {
            try {
                repository.addCourse(
                    medId = medId,
                    startDate = LocalDate.now(),
                    status = CourseStatus.ACTIVE,
                    frequencyText = previousCourse.frequencyText,
                    doseText = previousCourse.doseText,
                    timeHints = previousCourse.timeHints
                )
                _events.emit(DetailEvent.ShowMessage("疗程已恢复"))
                loadMed()
            } catch (e: Exception) {
                _events.emit(DetailEvent.ShowMessage("恢复失败: ${e.message}"))
            }
        }
    }

    fun deleteMed() {
        viewModelScope.launch {
            try {
                repository.deleteMed(medId)
                _events.emit(DetailEvent.NavigateBack)
            } catch (e: Exception) {
                _events.emit(DetailEvent.ShowMessage("删除失败: ${e.message}"))
            }
        }
    }

    fun enterEditMode() {
        val med = _uiState.value.med ?: return
        val course = _uiState.value.currentCourse
        _uiState.update {
            it.copy(
                editMode = EditModeState(
                    isEditing = true,
                    editName = med.name,
                    editAliases = med.aliases ?: "",
                    editNote = med.note ?: "",
                    editFrequency = course?.frequencyText ?: "",
                    editDose = course?.doseText ?: "",
                    editTimeHints = course?.timeHints ?: ""
                )
            )
        }
    }

    fun exitEditMode() {
        _uiState.update { it.copy(editMode = EditModeState()) }
    }

    fun onEditNameChanged(value: String) {
        _uiState.update {
            it.copy(editMode = it.editMode.copy(editName = value))
        }
    }

    fun onEditAliasesChanged(value: String) {
        _uiState.update {
            it.copy(editMode = it.editMode.copy(editAliases = value))
        }
    }

    fun onEditNoteChanged(value: String) {
        _uiState.update {
            it.copy(editMode = it.editMode.copy(editNote = value))
        }
    }

    fun onEditFrequencyChanged(value: String) {
        _uiState.update {
            it.copy(editMode = it.editMode.copy(editFrequency = value))
        }
    }

    fun onEditDoseChanged(value: String) {
        _uiState.update {
            it.copy(editMode = it.editMode.copy(editDose = value))
        }
    }

    fun onEditTimeHintsChanged(value: String) {
        _uiState.update {
            it.copy(editMode = it.editMode.copy(editTimeHints = value))
        }
    }

    fun saveEdits() {
        viewModelScope.launch {
            try {
                val editMode = _uiState.value.editMode
                if (editMode.editName.isBlank()) {
                    _events.emit(DetailEvent.ShowMessage("药品名称不能为空"))
                    return@launch
                }

                repository.updateMed(
                    id = medId,
                    name = editMode.editName,
                    aliases = editMode.editAliases.takeIf { it.isNotBlank() },
                    note = editMode.editNote.takeIf { it.isNotBlank() }
                )

                // Update current course if exists
                val currentCourse = _uiState.value.currentCourse
                if (currentCourse != null && editMode.editFrequency.isNotBlank()) {
                    val course = repository.getCoursesByMedId(medId)
                    // This is simplified - in full implementation we'd need getCourseById
                }

                _events.emit(DetailEvent.ShowMessage("保存成功"))
                exitEditMode()
                loadMed()
            } catch (e: Exception) {
                _events.emit(DetailEvent.ShowMessage("保存失败: ${e.message}"))
            }
        }
    }

    fun deleteCourse(courseId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteCourse(courseId)
                _events.emit(DetailEvent.ShowMessage("疗程已删除"))
                loadMed()
            } catch (e: Exception) {
                _events.emit(DetailEvent.ShowMessage("删除失败: ${e.message}"))
            }
        }
    }

    fun generateInfoSummary() {
        viewModelScope.launch {
            try {
                val med = _uiState.value.med ?: return@launch
                if (med.infoSummary != null) {
                    // Already has summary, just show it
                    return@launch
                }

                _events.emit(DetailEvent.ShowMessage("正在生成药品简介..."))
                
                val settings = preferencesStore.currentSettings()
                val apiKey = settings.apiKey
                
                if (apiKey.isBlank()) {
                    _events.emit(DetailEvent.ShowMessage("请先在设置中配置 AI API Key"))
                    return@launch
                }
                
                summaryGenerator.generateSummary(
                    apiKey = apiKey,
                    medName = med.name,
                    aliases = med.aliases
                ).fold(
                    onSuccess = { summary ->
                        repository.updateMed(
                            id = med.id,
                            name = med.name,
                            aliases = med.aliases,
                            note = med.note,
                            infoSummary = summary
                        )
                        loadMed()
                        _events.emit(DetailEvent.ShowMessage("简介生成成功"))
                    },
                    onFailure = { error ->
                        _events.emit(DetailEvent.ShowMessage("生成失败：${error.message}"))
                    }
                )
            } catch (e: Exception) {
                _events.emit(DetailEvent.ShowMessage("生成失败: ${e.message}"))
            }
        }
    }

    // ========== Reminder Operations ==========
    
    fun openReminderDialog(reminderId: Long? = null) {
        viewModelScope.launch {
            if (reminderId != null) {
                // Edit existing reminder
                val reminder = repository.getReminderById(reminderId)
                if (reminder != null) {
                    _uiState.update {
                        it.copy(
                            reminderDialogState = ReminderDialogState(
                                reminderId = reminder.id,
                                hour = reminder.hour,
                                minute = reminder.minute,
                                repeatType = reminder.repeatType,
                                weekDays = reminder.weekDays?.toSet() ?: emptySet(),
                                startDate = reminder.startDate,
                                endDate = reminder.endDate,
                                title = reminder.title ?: "",
                                message = reminder.message ?: ""
                            )
                        )
                    }
                }
            } else {
                // Create new reminder
                val medName = _uiState.value.med?.name ?: "药品"
                _uiState.update {
                    it.copy(
                        reminderDialogState = ReminderDialogState(
                            title = "用药提醒",
                            message = "该吃${medName}了"
                        )
                    )
                }
            }
        }
    }

    fun closeReminderDialog() {
        _uiState.update { it.copy(reminderDialogState = null) }
    }

    fun updateReminderDialogState(update: (ReminderDialogState) -> ReminderDialogState) {
        _uiState.update {
            it.copy(reminderDialogState = it.reminderDialogState?.let(update))
        }
    }

    fun saveReminder() {
        viewModelScope.launch {
            val dialogState = _uiState.value.reminderDialogState ?: return@launch
            
            try {
                // Validate date range
                if (dialogState.repeatType == com.heldairy.feature.medication.RepeatType.DATE_RANGE) {
                    val start = dialogState.startDate
                    val end = dialogState.endDate
                    if (start == null || end == null) {
                        _events.emit(DetailEvent.ShowMessage("请选择开始和结束日期"))
                        return@launch
                    }
                    if (end.isBefore(start)) {
                        _events.emit(DetailEvent.ShowMessage("结束日期不能早于开始日期"))
                        return@launch
                    }
                }

                if (dialogState.reminderId != null) {
                    // Update existing reminder
                    val reminder = MedicationReminder(
                        id = dialogState.reminderId,
                        medId = medId,
                        hour = dialogState.hour,
                        minute = dialogState.minute,
                        repeatType = dialogState.repeatType,
                        weekDays = when (dialogState.repeatType) {
                            com.heldairy.feature.medication.RepeatType.WEEKLY -> dialogState.weekDays.toList()
                            else -> null
                        },
                        startDate = dialogState.startDate,
                        endDate = dialogState.endDate,
                        enabled = true,
                        title = dialogState.title.ifBlank { null },
                        message = dialogState.message.ifBlank { null },
                        createdAt = 0L,  // Will be set from existing
                        updatedAt = System.currentTimeMillis()
                    )
                    repository.updateReminder(reminder)
                    
                    // Reschedule notification
                    val updatedReminder = repository.getReminderById(dialogState.reminderId)
                    updatedReminder?.let { ReminderScheduler.scheduleReminder(context, it) }
                    
                    _events.emit(DetailEvent.ShowMessage("提醒已更新"))
                } else {
                    // Create new reminder
                    val newReminderId = repository.addReminder(
                        medId = medId,
                        hour = dialogState.hour,
                        minute = dialogState.minute,
                        repeatType = dialogState.repeatType,
                        weekDays = when (dialogState.repeatType) {
                            com.heldairy.feature.medication.RepeatType.WEEKLY -> dialogState.weekDays.toList()
                            else -> null
                        },
                        startDate = dialogState.startDate,
                        endDate = dialogState.endDate,
                        title = dialogState.title.ifBlank { null },
                        message = dialogState.message.ifBlank { null }
                    )
                    
                    // Schedule notification for new reminder
                    val newReminder = repository.getReminderById(newReminderId)
                    newReminder?.let { ReminderScheduler.scheduleReminder(context, it) }
                    
                    _events.emit(DetailEvent.ShowMessage("提醒已添加"))
                }
                
                closeReminderDialog()
                loadMed()  // Refresh reminders list
            } catch (e: Exception) {
                _events.emit(DetailEvent.ShowMessage("保存失败: ${e.message}"))
            }
        }
    }

    fun deleteReminder(reminderId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteReminder(reminderId)
                
                // Cancel scheduled notification
                ReminderScheduler.cancelReminder(context, reminderId)
                
                _events.emit(DetailEvent.ShowMessage("提醒已删除"))
                loadMed()
            } catch (e: Exception) {
                _events.emit(DetailEvent.ShowMessage("删除失败: ${e.message}"))
            }
        }
    }

    fun toggleReminderEnabled(reminderId: Long, enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.toggleReminderEnabled(reminderId, enabled)
                
                // Schedule or cancel notification based on enabled state
                val reminder = repository.getReminderById(reminderId)
                if (reminder != null) {
                    if (enabled) {
                        ReminderScheduler.scheduleReminder(context, reminder)
                    } else {
                        ReminderScheduler.cancelReminder(context, reminderId)
                    }
                }
                
                loadMed()
            } catch (e: Exception) {
                _events.emit(DetailEvent.ShowMessage("更新失败: ${e.message}"))
            }
        }
    }

    companion object {
        fun factory(medId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as HElDairyApplication
                MedicationDetailViewModel(
                    repository = app.appContainer.medicationRepository,
                    summaryGenerator = app.appContainer.medicationInfoSummaryGenerator,
                    preferencesStore = app.appContainer.aiPreferencesStore,
                    context = app.applicationContext,
                    medId = medId
                )
            }
        }
    }
}

data class DetailUiState(
    val med: Med? = null,
    val currentCourse: MedCourse? = null,
    val historyCourses: List<MedCourse> = emptyList(),
    val reminders: List<MedicationReminder> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val editMode: EditModeState = EditModeState(),
    val reminderDialogState: ReminderDialogState? = null
)

data class EditModeState(
    val isEditing: Boolean = false,
    val editName: String = "",
    val editAliases: String = "",
    val editNote: String = "",
    val editFrequency: String = "",
    val editDose: String = "",
    val editTimeHints: String = ""
)

data class ReminderDialogState(
    val reminderId: Long? = null,  // null means creating new
    val hour: Int = 8,
    val minute: Int = 0,
    val repeatType: com.heldairy.feature.medication.RepeatType = com.heldairy.feature.medication.RepeatType.DAILY,
    val weekDays: Set<Int> = setOf(1, 2, 3, 4, 5),  // 1=Mon, 7=Sun
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val title: String = "",
    val message: String = ""
)

sealed interface DetailEvent {
    data class ShowMessage(val message: String) : DetailEvent
    data object NavigateBack : DetailEvent
}

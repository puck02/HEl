package com.heldairy.feature.medication.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.heldairy.feature.medication.CourseStatus
import com.heldairy.feature.medication.DetailEvent
import com.heldairy.feature.medication.MedCourse
import com.heldairy.feature.medication.MedicationDetailViewModel
import com.heldairy.feature.medication.reminder.NotificationPermissionHelper
import com.heldairy.ui.theme.CornerRadius
import com.heldairy.ui.theme.Elevation
import com.heldairy.ui.theme.Spacing
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationDetailRoute(
    medId: Long,
    paddingValues: PaddingValues = PaddingValues(),
    onBack: () -> Unit
) {
    val viewModel: MedicationDetailViewModel = viewModel(
        key = "medication_detail_$medId",
        factory = MedicationDetailViewModel.factory(medId)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEndCourseDialog by remember { mutableStateOf(false) }
    var showDeleteCourseDialog by remember { mutableStateOf<Long?>(null) }
    var showSummaryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is DetailEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is DetailEvent.NavigateBack -> onBack()
            }
        }
    }

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(uiState.med?.name ?: "药品详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (!uiState.editMode.isEditing) {
                        IconButton(onClick = { viewModel.enterEditMode() }) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        MedicationDetailScreen(
            uiState = uiState,
            onPauseCourse = viewModel::pauseCourse,
            onEndCourse = { showEndCourseDialog = true },
            onResumeCourse = { viewModel.resumeCourse(medId, it) },
            onEditNameChanged = viewModel::onEditNameChanged,
            onEditAliasesChanged = viewModel::onEditAliasesChanged,
            onEditNoteChanged = viewModel::onEditNoteChanged,
            onEditFrequencyChanged = viewModel::onEditFrequencyChanged,
            onEditDoseChanged = viewModel::onEditDoseChanged,
            onEditTimeHintsChanged = viewModel::onEditTimeHintsChanged,
            onSaveEdits = viewModel::saveEdits,
            onCancelEdit = viewModel::exitEditMode,
            onDeleteCourse = { showDeleteCourseDialog = it },
            onShowSummary = { showSummaryDialog = true },
            onGenerateSummary = viewModel::generateInfoSummary,
            onAddReminder = { viewModel.openReminderDialog(null) },
            onEditReminder = { viewModel.openReminderDialog(it) },
            onToggleReminder = viewModel::toggleReminderEnabled,
            onDeleteReminder = viewModel::deleteReminder,
            modifier = Modifier.padding(innerPadding)
        )
    }

    // Reminder Edit Dialog
    uiState.reminderDialogState?.let { dialogState ->
        ReminderEditDialog(
            dialogState = dialogState,
            onDismiss = { viewModel.closeReminderDialog() },
            onConfirm = { viewModel.saveReminder() },
            onUpdateState = { viewModel.updateReminderDialogState(it) }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("删除后将无法恢复，确定要删除这个药品吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMed()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showEndCourseDialog) {
        AlertDialog(
            onDismissRequest = { showEndCourseDialog = false },
            title = { Text("结束疗程") },
            text = { Text("确定要结束当前疗程吗？结束后可以重新开始新疗程。") },
            confirmButton = {
                Button(
                    onClick = {
                        uiState.currentCourse?.let { viewModel.endCourse(it.id) }
                        showEndCourseDialog = false
                    }
                ) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndCourseDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    showDeleteCourseDialog?.let { courseId ->
        AlertDialog(
            onDismissRequest = { showDeleteCourseDialog = null },
            title = { Text("删除疗程") },
            text = { Text("确定要删除此疗程记录吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCourse(courseId)
                        showDeleteCourseDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCourseDialog = null }) {
                    Text("取消")
                }
            }
        )
    }

    if (showSummaryDialog) {
        AlertDialog(
            onDismissRequest = { showSummaryDialog = false },
            title = { Text("药品简介") },
            text = {
                Text(
                    text = uiState.med?.infoSummary ?: "暂无简介",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showSummaryDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }
}

@Composable
private fun MedicationDetailScreen(
    uiState: com.heldairy.feature.medication.DetailUiState,
    onPauseCourse: (Long) -> Unit,
    onEndCourse: () -> Unit,
    onResumeCourse: (MedCourse) -> Unit,
    onEditNameChanged: (String) -> Unit,
    onEditAliasesChanged: (String) -> Unit,
    onEditNoteChanged: (String) -> Unit,
    onEditFrequencyChanged: (String) -> Unit,
    onEditDoseChanged: (String) -> Unit,
    onEditTimeHintsChanged: (String) -> Unit,
    onSaveEdits: () -> Unit,
    onCancelEdit: () -> Unit,
    onDeleteCourse: (Long) -> Unit,
    onShowSummary: () -> Unit,
    onGenerateSummary: () -> Unit,
    onAddReminder: () -> Unit,
    onEditReminder: (Long) -> Unit,
    onToggleReminder: (Long, Boolean) -> Unit,
    onDeleteReminder: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        uiState.error != null -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = uiState.error, color = MaterialTheme.colorScheme.error)
            }
        }
        uiState.med != null -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Spacing.M),
                verticalArrangement = Arrangement.spacedBy(Spacing.M)
            ) {
                if (uiState.editMode.isEditing) {
                    EditModeContent(
                        editMode = uiState.editMode,
                        onNameChanged = onEditNameChanged,
                        onAliasesChanged = onEditAliasesChanged,
                        onNoteChanged = onEditNoteChanged,
                        onFrequencyChanged = onEditFrequencyChanged,
                        onDoseChanged = onEditDoseChanged,
                        onTimeHintsChanged = onEditTimeHintsChanged,
                        onSave = onSaveEdits,
                        onCancel = onCancelEdit
                    )
                } else {
                    // Basic Info Card
                    BasicInfoCard(
                        med = uiState.med,
                        onShowSummary = onShowSummary,
                        onGenerateSummary = onGenerateSummary
                    )

                    // Current Course Card
                    if (uiState.currentCourse != null) {
                        CurrentCourseCard(
                            course = uiState.currentCourse,
                            onPause = onPauseCourse,
                            onEnd = onEndCourse,
                            onResume = onResumeCourse
                        )
                    }

                    // Medication Reminders (移到历史疗程上面)
                    RemindersCard(
                        reminders = uiState.reminders,
                        onAddReminder = onAddReminder,
                        onEditReminder = onEditReminder,
                        onToggleReminder = onToggleReminder,
                        onDeleteReminder = onDeleteReminder
                    )

                    // History Courses
                    if (uiState.historyCourses.isNotEmpty()) {
                        HistoryCoursesCard(
                            courses = uiState.historyCourses,
                            onDeleteCourse = onDeleteCourse
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BasicInfoCard(
    med: com.heldairy.feature.medication.Med,
    onShowSummary: () -> Unit,
    onGenerateSummary: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(CornerRadius.Medium),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.Low),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Spacing.M)) {
            Text(
                text = med.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            // 药品简介入口
            Spacer(modifier = Modifier.height(Spacing.XXS))
            Text(
                text = "点击查看简介",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = Spacing.XXS)
                    .clickable { 
                        if (med.infoSummary != null) {
                            onShowSummary()
                        } else {
                            onGenerateSummary()
                        }
                    }
            )

            if (!med.aliases.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(Spacing.XS))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.XS)) {
                    med.aliases.split(",").forEach { alias ->
                        AssistChip(
                            onClick = { },
                            label = { Text(alias.trim(), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            if (!med.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(Spacing.S))
                Text(
                    text = med.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CurrentCourseCard(
    course: MedCourse,
    onPause: (Long) -> Unit,
    onEnd: () -> Unit,
    onResume: (MedCourse) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (course.status) {
                CourseStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                CourseStatus.PAUSED -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape(CornerRadius.Medium),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.None),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Spacing.M)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "当前疗程",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .background(
                            brush = when (course.status) {
                                CourseStatus.ACTIVE -> Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)
                                    )
                                )
                                CourseStatus.PAUSED -> Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.10f)
                                    )
                                )
                                else -> Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.10f)
                                    )
                                )
                            },
                            shape = RoundedCornerShape(CornerRadius.XLarge)
                        )
                        .border(
                            width = 1.dp,
                            color = when (course.status) {
                                CourseStatus.ACTIVE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                CourseStatus.PAUSED -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(CornerRadius.XLarge)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = when (course.status) {
                            CourseStatus.ACTIVE -> "🟢 正在服用"
                            CourseStatus.PAUSED -> "🟡 已暂停"
                            CourseStatus.ENDED -> "⚫ 已停用"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = when (course.status) {
                            CourseStatus.ACTIVE -> MaterialTheme.colorScheme.primary
                            CourseStatus.PAUSED -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.S))

            val endDateForCalculation = when (course.status) {
                CourseStatus.ACTIVE -> LocalDate.now()
                CourseStatus.PAUSED, CourseStatus.ENDED -> course.endDate ?: LocalDate.now()
            }
            val daysElapsed = ChronoUnit.DAYS.between(course.startDate, endDateForCalculation)
            Text(
                text = "开始日期: ${course.startDate} (已持续 $daysElapsed 天)",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(Spacing.XS))
            Text(
                text = "频率: ${course.frequencyText}",
                style = MaterialTheme.typography.bodyMedium
            )

            if (!course.doseText.isNullOrBlank()) {
                Text(
                    text = "剂量: ${course.doseText}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (!course.timeHints.isNullOrBlank()) {
                Text(
                    text = "用药时间: ${course.timeHints}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(Spacing.M))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.S)
            ) {
                when (course.status) {
                    CourseStatus.ACTIVE -> {
                        OutlinedButton(
                            onClick = { onPause(course.id) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Pause,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("暂停")
                        }
                        Button(
                            onClick = { onEnd() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("结束")
                        }
                    }
                    CourseStatus.PAUSED -> {
                        Button(
                            onClick = { onResume(course) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("恢复")
                        }
                        OutlinedButton(
                            onClick = { onEnd() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("结束")
                        }
                    }
                    CourseStatus.ENDED -> {
                        OutlinedButton(
                            onClick = { },
                            enabled = false,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("已停用")
                        }
                        Button(
                            onClick = { onResume(course) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("开启新疗程")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCoursesCard(
    courses: List<MedCourse>,
    onDeleteCourse: (Long) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(CornerRadius.Medium),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.Low),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(Spacing.M)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "历史疗程",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "暂停不计入疗程天数",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(Spacing.S))

            courses.forEach { course ->
                HistoryCourseItem(
                    course = course,
                    onDelete = { onDeleteCourse(course.id) }
                )
                if (course != courses.last()) {
                    Spacer(modifier = Modifier.height(Spacing.S))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryCourseItem(
    course: MedCourse,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
            }
            false // Don't remove item, dialog will handle it
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            // Red delete background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(CornerRadius.Small)
                    )
                    .padding(horizontal = Spacing.M),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.onError
                )
            }
        },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(CornerRadius.Small)
                    )
                    .padding(Spacing.S)
            ) {
                val duration = if (course.endDate != null) {
                    ChronoUnit.DAYS.between(course.startDate, course.endDate)
                } else {
                    ChronoUnit.DAYS.between(course.startDate, LocalDate.now())
                }

                Text(
                    text = "${course.startDate} - ${course.endDate ?: "进行中"} | $duration 天",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${course.frequencyText}${course.doseText?.let { " · $it" } ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
private fun EditModeContent(
    editMode: com.heldairy.feature.medication.EditModeState,
    onNameChanged: (String) -> Unit,
    onAliasesChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onFrequencyChanged: (String) -> Unit,
    onDoseChanged: (String) -> Unit,
    onTimeHintsChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(CornerRadius.Medium),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.Low),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Spacing.M),
            verticalArrangement = Arrangement.spacedBy(Spacing.S)
        ) {
            Text(
                text = "编辑药品信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = editMode.editName,
                onValueChange = onNameChanged,
                label = { Text("药品名称 *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = editMode.editAliases,
                onValueChange = onAliasesChanged,
                label = { Text("别名（多个用逗号分隔）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = editMode.editNote,
                onValueChange = onNoteChanged,
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            OutlinedTextField(
                value = editMode.editFrequency,
                onValueChange = onFrequencyChanged,
                label = { Text("频率（例如：每日3次）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = editMode.editDose,
                onValueChange = onDoseChanged,
                label = { Text("剂量（例如：每次1片）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = editMode.editTimeHints,
                onValueChange = onTimeHintsChanged,
                label = { Text("用药时间（例如：早中晚）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(Spacing.S))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCancel) {
                    Text("取消")
                }
                Spacer(modifier = Modifier.width(Spacing.XS))
                Button(onClick = onSave) {
                    Text("保存")
                }
            }
        }
    }
}

// ========== Reminder Components ==========

@Composable
private fun RemindersCard(
    reminders: List<com.heldairy.feature.medication.MedicationReminder>,
    onAddReminder: () -> Unit,
    onEditReminder: (Long) -> Unit,
    onToggleReminder: (Long, Boolean) -> Unit,
    onDeleteReminder: (Long) -> Unit
) {
    val context = LocalContext.current
    var reminderToDelete by remember { mutableStateOf<Long?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var pendingToggle by remember { mutableStateOf<Pair<Long, Boolean>?>(null) }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, proceed with the toggle
            pendingToggle?.let { (id, enabled) ->
                onToggleReminder(id, enabled)
            }
        } else {
            // Permission denied, show explanation dialog
            showPermissionDialog = true
        }
        pendingToggle = null
    }
    
    val handleToggleWithPermission: (Long, Boolean) -> Unit = { id, enabled ->
        if (enabled) {
            // Enabling reminder - check permission
            if (NotificationPermissionHelper.hasNotificationPermission(context)) {
                onToggleReminder(id, enabled)
            } else {
                // Request permission
                pendingToggle = id to enabled
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    // Below Android 13, no permission needed
                    onToggleReminder(id, enabled)
                }
            }
        } else {
            // Disabling reminder - no permission needed
            onToggleReminder(id, enabled)
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(CornerRadius.Medium),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.Low),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(Spacing.M)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "用药提醒",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                AssistChip(
                    onClick = onAddReminder,
                    label = { Text("添加提醒") }
                )
            }

            if (reminders.isEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.S))
                Text(
                    text = "还没有设置提醒",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(modifier = Modifier.height(Spacing.S))
                reminders.forEach { reminder ->
                    ReminderItem(
                        reminder = reminder,
                        onEdit = { onEditReminder(reminder.id) },
                        onToggle = { handleToggleWithPermission(reminder.id, it) },
                        onDelete = { reminderToDelete = reminder.id }
                    )
                    Spacer(modifier = Modifier.height(Spacing.S))
                }
            }
        }
    }

    // Delete confirmation dialog
    if (reminderToDelete != null) {
        AlertDialog(
            onDismissRequest = { reminderToDelete = null },
            title = { Text("删除提醒") },
            text = { Text("确定要删除这个用药提醒吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteReminder(reminderToDelete!!)
                        reminderToDelete = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { reminderToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
    
    // Permission explanation dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("需要通知权限") },
            text = { Text("为了在指定时间提醒您用药，应用需要发送通知的权限。请在系统设置中手动开启通知权限。") },
            confirmButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("知道了")
                }
            }
        )
    }
}

@Composable
private fun ReminderItem(
    reminder: com.heldairy.feature.medication.MedicationReminder,
    onEdit: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(CornerRadius.Small),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.M),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = reminder.getTimeString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(Spacing.S))
                    Text(
                        text = reminder.getRepeatDescription(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (reminder.title?.isNotBlank() == true) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                if (reminder.message?.isNotBlank() == true) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = reminder.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Switch(
                    checked = reminder.enabled,
                    onCheckedChange = onToggle
                )
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "编辑")
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberWheelPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val rangeSize = range.count()
    val infiniteMultiplier = 1000
    val startIndex = infiniteMultiplier * rangeSize + (value - range.first)
    
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = startIndex
    )
    
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerIndex = listState.firstVisibleItemIndex + 2
            val selectedValue = range.first + (centerIndex % rangeSize)
            if (selectedValue != value) {
                onValueChange(selectedValue)
            }
        }
    }
    
    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        // Selection indicator
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    RoundedCornerShape(CornerRadius.Small)
                )
        )
        
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxHeight(),
            contentPadding = PaddingValues(vertical = 66.dp)
        ) {
            items(infiniteMultiplier * rangeSize * 2) { index ->
                val itemValue = range.first + (index % rangeSize)
                val isSelected = itemValue == value
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { onValueChange(itemValue) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%02d", itemValue),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderEditDialog(
    dialogState: com.heldairy.feature.medication.ReminderDialogState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onUpdateState: ((com.heldairy.feature.medication.ReminderDialogState) -> com.heldairy.feature.medication.ReminderDialogState) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (dialogState.reminderId == null) "添加提醒" else "编辑提醒") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Time Picker
                Text(
                    text = "提醒时间",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(Spacing.XS))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour Wheel
                    NumberWheelPicker(
                        value = dialogState.hour,
                        range = 0..23,
                        onValueChange = { onUpdateState { state -> state.copy(hour = it) } },
                        modifier = Modifier.weight(1f)
                    )
                    
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(horizontal = Spacing.S)
                    )
                    
                    // Minute Wheel
                    NumberWheelPicker(
                        value = dialogState.minute,
                        range = 0..59,
                        onValueChange = { onUpdateState { state -> state.copy(minute = it) } },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.M))

                // Repeat Type
                Text(
                    text = "重复模式",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(Spacing.XS))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.XS)
                ) {
                    listOf(
                        com.heldairy.feature.medication.RepeatType.DAILY to "每天",
                        com.heldairy.feature.medication.RepeatType.WEEKLY to "每周",
                        com.heldairy.feature.medication.RepeatType.DATE_RANGE to "日期范围"
                    ).forEach { (type, label) ->
                        AssistChip(
                            onClick = { onUpdateState { it.copy(repeatType = type) } },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f),
                            colors = if (dialogState.repeatType == type) {
                                androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            } else {
                                androidx.compose.material3.AssistChipDefaults.assistChipColors()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.M))

                // Week Days Selector (only for WEEKLY)
                if (dialogState.repeatType == com.heldairy.feature.medication.RepeatType.WEEKLY) {
                    Text(
                        text = "选择星期",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(Spacing.XS))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(1 to "一", 2 to "二", 3 to "三", 4 to "四", 5 to "五", 6 to "六", 7 to "日").forEach { (day, label) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (dialogState.weekDays.contains(day)) 
                                            MaterialTheme.colorScheme.primaryContainer
                                        else 
                                            MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(CornerRadius.Small)
                                    )
                                    .clickable {
                                        onUpdateState { state ->
                                            val newDays = if (state.weekDays.contains(day)) {
                                                state.weekDays - day
                                            } else {
                                                state.weekDays + day
                                            }
                                            state.copy(weekDays = newDays)
                                        }
                                    }
                                    .padding(vertical = Spacing.S),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (dialogState.weekDays.contains(day))
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.M))
                }

                // Date Range Selector (only for DATE_RANGE)
                if (dialogState.repeatType == com.heldairy.feature.medication.RepeatType.DATE_RANGE) {
                    Text(
                        text = "日期范围",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(Spacing.XS))
                    
                    OutlinedTextField(
                        value = dialogState.startDate?.toString() ?: "",
                        onValueChange = { 
                            try {
                                val date = if (it.isNotBlank()) LocalDate.parse(it) else null
                                onUpdateState { state -> state.copy(startDate = date) }
                            } catch (_: Exception) {}
                        },
                        label = { Text("开始日期 (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(Spacing.S))
                    
                    OutlinedTextField(
                        value = dialogState.endDate?.toString() ?: "",
                        onValueChange = { 
                            try {
                                val date = if (it.isNotBlank()) LocalDate.parse(it) else null
                                onUpdateState { state -> state.copy(endDate = date) }
                            } catch (_: Exception) {}
                        },
                        label = { Text("结束日期 (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(Spacing.M))
                }

                // Title and Message
                OutlinedTextField(
                    value = dialogState.title,
                    onValueChange = { onUpdateState { state -> state.copy(title = it) } },
                    label = { Text("提醒标题（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(Spacing.S))

                OutlinedTextField(
                    value = dialogState.message,
                    onValueChange = { onUpdateState { state -> state.copy(message = it) } },
                    label = { Text("提醒消息（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

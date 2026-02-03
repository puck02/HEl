# 三个问题修复实现指南

## 问题1: 时间选择器循环滚动

**文件**: `app/src/main/java/com/heldairy/feature/medication/ui/MedicationDetailScreen.kt`  
**位置**: 行 1021-1088 (NumberWheelPicker函数)

**修改前**:
```kotlin
@Composable
private fun NumberWheelPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (value - range.first).coerceIn(0, range.count() - 1)
    )
    
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerIndex = listState.firstVisibleItemIndex + 2
            val selectedValue = range.first + centerIndex.coerceIn(0, range.count() - 1)
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
            items(range.count()) { index ->
                val itemValue = range.first + index
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
```

**修改后**:
```kotlin
@Composable
private fun NumberWheelPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val rangeSize = range.count()
    val infiniteMultiplier = 1000 // Large enough for infinite scroll feel
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
```

**关键变更**:
1. 添加 `rangeSize` 和 `infiniteMultiplier` 变量
2. 修改 `initialFirstVisibleItemIndex` 从中间开始
3. `items(infiniteMultiplier * rangeSize * 2)` 创建足够多的项目实现循环
4. 使用 `centerIndex % rangeSize` 计算实际值

---

## 问题2: 清空数据二次确认对话框

**文件**: `app/src/main/java/com/heldairy/feature/settings/ui/SettingsScreen.kt`  
**位置**: 行 170-318

**在 SettingsScreen 函数开始处添加状态**:
```kotlin
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onApiKeyChanged: (String) -> Unit,
    onSaveApiKey: () -> Unit,
    onClearApiKey: () -> Unit,
    onAiEnabledChanged: (Boolean) -> Unit,
    onUserNameChanged: (String) -> Unit,
    onSaveUserName: () -> Unit,
    onAvatarSelected: (Uri?) -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onClearAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showApiKey by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }  // 添加这行
```

**修改 "数据管理" 部分的 OutlinedActionButton**:
```kotlin
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.S)) {
            Text(text = "数据管理", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text(
                text = "清空所有日报记录和用药记录，但会保留用户名、头像和 API Key。清空后数据无法恢复，建议先导出备份。",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedActionButton(
                text = "清空所有数据",
                icon = Icons.Outlined.DeleteSweep,
                onClick = { showClearDataDialog = true },  // 修改这里
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // 在 SettingsScreen 函数的最后（} 之前）添加确认对话框
        if (showClearDataDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showClearDataDialog = false },
                icon = { 
                    Icon(
                        imageVector = Icons.Outlined.DeleteSweep,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = { Text("确认清空所有数据？") },
                text = {
                    Column {
                        Text("此操作将清空：")
                        Text("• 所有日报记录及相关数据", style = MaterialTheme.typography.bodyMedium)
                        Text("• 所有用药记录及提醒", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "将保留：用户名、头像、API Key",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "⚠️ 此操作无法撤销！建议先导出备份。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showClearDataDialog = false
                            onClearAllData()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("确认清空")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDataDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}
```

---

## 问题3: 用药数据导出/导入

### 3.1 添加备份模型

**文件**: `app/src/main/java/com/heldairy/core/data/BackupModels.kt`  
**在文件末尾添加**:

```kotlin
@Serializable
data class BackupMedication(
    val id: Long,
    val name: String,
    val aliases: String? = null,
    val note: String? = null,
    @SerialName("info_summary") val infoSummary: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    val courses: List<BackupCourse> = emptyList(),
    val reminders: List<BackupReminder> = emptyList()
)

@Serializable
data class BackupCourse(
    val id: Long,
    @SerialName("med_id") val medId: Long,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String? = null,
    val status: String,
    @SerialName("frequency_text") val frequencyText: String,
    @SerialName("dose_text") val doseText: String? = null,
    @SerialName("time_hints") val timeHints: String? = null
)

@Serializable
data class BackupReminder(
    val id: Long,
    @SerialName("med_id") val medId: Long,
    val hour: Int,
    val minute: Int,
    @SerialName("repeat_type") val repeatType: String,
    @SerialName("week_days") val weekDays: String? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    val enabled: Boolean,
    val title: String? = null,
    val message: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)
```

### 3.2 更新 BackupPayload

**文件**: `app/src/main/java/com/heldairy/core/data/BackupModels.kt`  
**修改 BackupPayload (行 6-11)**:

```kotlin
@Serializable
data class BackupPayload(
    @SerialName("schema_version") val schemaVersion: Int = 3,  // 改为 3
    val entries: List<BackupEntry>,
    val insights: List<BackupInsight> = emptyList(),
    val medications: List<BackupMedication> = emptyList()  // 添加这行
)
```

### 3.3 更新 BackupManager 导出

**文件**: `app/src/main/java/com/heldairy/core/data/BackupManager.kt`  
**修改 exportJson 函数 (行 18-26)**:

```kotlin
    suspend fun exportJson(): String {
        val snapshots = repository.loadAllSnapshots()
        val meds = medicationRepository.getAllMeds().first()
        val medications = meds.map { it.toBackupMedication() }
        
        val payload = BackupPayload(
            schemaVersion = SCHEMA_VERSION,
            entries = snapshots.map { it.toBackupEntry() },
            insights = insightRepository.loadAllInsights().map { it.toBackupInsight() },
            medications = medications
        )
        return json.encodeToString(payload)
    }
```

### 3.4 更新 BackupManager 导入

**文件**: `app/src/main/java/com/heldairy/core/data/BackupManager.kt`  
**修改 importJson 函数，在恢复insights之后添加medications恢复 (约行 80之后)**:

```kotlin
            payload.insights.forEach { insight ->
                insightRepository.restoreInsight(
                    InsightReportEntity(
                        weekStartDate = insight.weekStartDate,
                        weekEndDate = insight.weekEndDate,
                        generatedAt = insight.generatedAt,
                        window7Json = insight.window7Json,
                        window30Json = insight.window30Json,
                        aiResultJson = insight.aiResultJson,
                        status = insight.status,
                        errorMessage = insight.errorMessage
                    )
                )
            }
            
            // 恢复用药数据
            payload.medications?.forEach { backupMed ->
                val medId = medicationRepository.addMed(
                    name = backupMed.name,
                    aliases = backupMed.aliases,
                    note = backupMed.note,
                    infoSummary = backupMed.infoSummary
                )
                
                // 恢复疗程
                backupMed.courses.forEach { backupCourse ->
                    medicationRepository.addCourse(
                        medId = medId,
                        startDate = java.time.LocalDate.parse(backupCourse.startDate),
                        endDate = backupCourse.endDate?.let { java.time.LocalDate.parse(it) },
                        status = com.heldairy.feature.medication.CourseStatus.fromDbString(backupCourse.status),
                        frequencyText = backupCourse.frequencyText,
                        doseText = backupCourse.doseText,
                        timeHints = backupCourse.timeHints
                    )
                }
                
                // 恢复提醒
                backupMed.reminders.forEach { backupReminder ->
                    medicationRepository.addReminder(
                        medId = medId,
                        hour = backupReminder.hour,
                        minute = backupReminder.minute,
                        repeatType = com.heldairy.feature.medication.RepeatType.valueOf(backupReminder.repeatType),
                        weekDays = backupReminder.weekDays?.split(",")?.mapNotNull { it.toIntOrNull() },
                        startDate = backupReminder.startDate?.let { java.time.LocalDate.parse(it) },
                        endDate = backupReminder.endDate?.let { java.time.LocalDate.parse(it) },
                        enabled = backupReminder.enabled,
                        title = backupReminder.title,
                        message = backupReminder.message
                    )
                }
            }
        }
    }
```

### 3.5 添加转换函数

**文件**: `app/src/main/java/com/heldairy/core/data/BackupManager.kt`  
**在文件末尾添加 (约行 195之后)**:

```kotlin
private suspend fun Med.toBackupMedication(): BackupMedication {
    val courses = medicationRepository.getCoursesByMedId(this.id).first()
    val reminders = medicationRepository.getRemindersByMedId(this.id).first()
    
    return BackupMedication(
        id = this.id,
        name = this.name,
        aliases = this.aliases,
        note = this.note,
        infoSummary = this.infoSummary,
        createdAt = this.createdAt.toEpochDay() * 86400000L,
        updatedAt = this.updatedAt.toEpochDay() * 86400000L,
        courses = courses.map { it.toBackupCourse() },
        reminders = reminders.map { it.toBackupReminder() }
    )
}

private fun MedCourse.toBackupCourse(): BackupCourse {
    return BackupCourse(
        id = this.id,
        medId = this.medId,
        startDate = this.startDate.toString(),
        endDate = this.endDate?.toString(),
        status = this.status.toDbString(),
        frequencyText = this.frequencyText,
        doseText = this.doseText,
        timeHints = this.timeHints
    )
}

private fun com.heldairy.feature.medication.MedicationReminder.toBackupReminder(): BackupReminder {
    return BackupReminder(
        id = this.id,
        medId = this.medId,
        hour = this.hour,
        minute = this.minute,
        repeatType = this.repeatType.name,
        weekDays = this.weekDays?.sorted()?.joinToString(","),
        startDate = this.startDate?.toString(),
        endDate = this.endDate?.toString(),
        enabled = this.enabled,
        title = this.title,
        message = this.message,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
```

### 3.6 更新 Schema 版本常量

**文件**: `app/src/main/java/com/heldairy/core/data/BackupManager.kt`  
**修改 Companion object (约行 144-147)**:

```kotlin
    companion object {
        private const val SCHEMA_VERSION = 3  // 改为 3
        private val supportedSchemas = setOf(1, 2, 3)  // 添加 3
    }
```

### 3.7 更新测试文件

**文件**: `app/src/test/java/com/heldairy/core/data/BackupManagerTest.kt`  
无需修改 - FakeMedicationRepository 已经存在并且 clearAll() 已实现为no-op，导入导出测试只测试日报数据。

---

## 实施步骤

1. **时间选择器**：修改 MedicationDetailScreen.kt 中的 NumberWheelPicker 函数
2. **确认对话框**：修改 SettingsScreen.kt 添加状态和对话框
3. **备份模型**：在 BackupModels.kt 添加三个新数据类
4. **导出导入**：修改 BackupManager.kt 的多个函数和添加转换函数

## 验证步骤

1. **时间选择器**：
   - 打开用药提醒编辑对话框
   - 向下滚动小时选择器到 23，继续滚动应该回到 00
   - 向上滚动到 00，继续滚动应该回到 23

2. **确认对话框**：
   - 进入设置页面
   - 点击"清空所有数据"按钮
   - 应显示确认对话框，包含详细说明和警告
   - 点击"取消"应关闭对话框不执行操作
   - 点击"确认清空"应执行清空操作

3. **数据导出导入**：
   - 添加一些用药记录、疗程和提醒
   - 导出JSON备份
   - 检查JSON文件应包含 medications 字段
   - 清空所有数据
   - 导入之前的备份
   - 验证用药记录、疗程和提醒都已恢复

## 构建验证

```bash
./gradlew build
```

所有修改完成后运行构建确保没有编译错误。

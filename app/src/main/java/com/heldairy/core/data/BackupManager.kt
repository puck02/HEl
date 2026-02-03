package com.heldairy.core.data

import com.heldairy.core.database.entity.DailyAdviceEntity
import com.heldairy.core.database.entity.DailyEntryEntity
import com.heldairy.core.database.entity.DailyEntrySnapshot
import com.heldairy.core.database.entity.DailySummaryEntity
import com.heldairy.core.database.entity.QuestionResponseEntity
import com.heldairy.core.database.entity.InsightReportEntity
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.first

class BackupManager(
    private val repository: DailyReportRepository,
    private val insightRepository: InsightRepository,
    private val medicationRepository: com.heldairy.feature.medication.MedicationRepository,
    private val json: Json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
) {
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

    suspend fun exportCsv(): String {
        val snapshots = repository.loadAllSnapshots()
        return CsvExporter.buildCsv(snapshots)
    }

    suspend fun importJson(raw: String): Result<Unit> {
        return runCatching {
            val payload = json.decodeFromString<BackupPayload>(raw)
            if (payload.schemaVersion !in supportedSchemas) {
                throw IllegalArgumentException("不支持的备份版本: ${payload.schemaVersion}")
            }
            repository.clearAll()
            payload.entries.sortedBy { it.createdAt }.forEach { entry ->
                val entryId = repository.recordDailyReport(
                    entryDate = entry.entryDate,
                    timezoneId = entry.timezoneId,
                    answers = entry.responses.map { it.toRecord() },
                    createdAtMillis = entry.createdAt
                ).getOrThrow()
                entry.advice?.let { advice ->
                    repository.saveAdvice(
                        DailyAdviceEntity(
                            entryId = entryId,
                            entryDate = entry.entryDate,
                            model = advice.model,
                            adviceJson = advice.adviceJson,
                            promptHash = advice.promptHash,
                            generatedAt = advice.generatedAt
                        )
                    )
                }
                entry.summary?.let { summary ->
                    repository.saveSummary(
                        DailySummaryEntity(
                            entryId = entryId,
                            entryDate = entry.entryDate,
                            window7Json = summary.window7Json,
                            window30Json = summary.window30Json,
                            computedAt = summary.computedAt
                        )
                    )
                }
            }
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
            payload.medications.forEach { backupMed ->
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
                        title = backupReminder.title,
                        message = backupReminder.message
                    )
                }
            }
        }
    }

    suspend fun clearAllData() {
        repository.clearAll()
        insightRepository.clearAll()
        medicationRepository.clearAll()
    }

    private fun DailyEntrySnapshot.toBackupEntry(): BackupEntry {
        return BackupEntry(
            entryDate = entry.entryDate,
            createdAt = entry.createdAt,
            timezoneId = entry.timezoneId,
            responses = responses.map { response ->
                BackupResponse(
                    questionId = response.questionId,
                    stepIndex = response.stepIndex,
                    order = response.questionOrder,
                    answerType = response.answerType,
                    answerValue = response.answerValue,
                    answerLabel = response.answerLabel,
                    metadataJson = response.metadataJson,
                    answeredAt = response.answeredAt
                )
            },
            advice = advice?.let {
                BackupAdvice(
                    model = it.model,
                    adviceJson = it.adviceJson,
                    promptHash = it.promptHash,
                    generatedAt = it.generatedAt
                )
            },
            summary = summary?.let {
                BackupSummary(
                    window7Json = it.window7Json,
                    window30Json = it.window30Json,
                    computedAt = it.computedAt
                )
            }
        )
    }

    private fun BackupResponse.toRecord(): DailyAnswerRecord {
        return DailyAnswerRecord(
            questionId = questionId,
            stepIndex = stepIndex,
            order = order,
            answerType = answerType,
            answerValue = answerValue,
            answerLabel = answerLabel,
            metadataJson = metadataJson
        )
    }

    private suspend fun com.heldairy.feature.medication.Med.toBackupMedication(): BackupMedication {
        val courses = medicationRepository.getCoursesByMedId(this.id).first()
        val reminders = medicationRepository.getRemindersByMedId(this.id).first()
        
        return BackupMedication(
            id = this.id,
            name = this.name,
            aliases = this.aliases,
            note = this.note,
            infoSummary = this.infoSummary,
            createdAt = this.createdAt.toEpochDay() * 86400000L, // LocalDate to millis
            updatedAt = this.updatedAt.toEpochDay() * 86400000L,
            courses = courses.map { it.toBackupCourse() },
            reminders = reminders.map { it.toBackupReminder() }
        )
    }
    
    private fun com.heldairy.feature.medication.MedCourse.toBackupCourse(): BackupCourse {
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
            weekDays = this.weekDays?.joinToString(","),
            startDate = this.startDate?.toString(),
            endDate = this.endDate?.toString(),
            enabled = this.enabled,
            title = this.title,
            message = this.message,
            createdAt = this.createdAt,
            updatedAt = this.updatedAt
        )
    }

    companion object {
        private const val SCHEMA_VERSION = 3
        private val supportedSchemas = setOf(1, 2, 3)
    }
}

internal object CsvExporter {
    private val header = listOf(
        "entry_date",
        "question_id",
        "answer_label",
        "answer_value",
        "step_index",
        "answer_type",
        "answered_at"
    )

    fun buildCsv(snapshots: List<DailyEntrySnapshot>): String {
        val rows = mutableListOf(header)
        snapshots.sortedBy { it.entry.createdAt }.forEach { snapshot ->
            snapshot.responses.forEach { response ->
                rows += listOf(
                    snapshot.entry.entryDate,
                    response.questionId,
                    escape(response.answerLabel),
                    escape(response.answerValue),
                    response.stepIndex.toString(),
                    response.answerType,
                    response.answeredAt.toString()
                )
            }
        }
        return rows.joinToString(separator = "\n") { row -> row.joinToString(separator = ",") }
    }

    private fun escape(value: String): String {
        val needsQuote = value.contains(',') || value.contains('"') || value.contains('\n')
        if (!needsQuote) return value
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}

private fun InsightReportEntity.toBackupInsight(): BackupInsight {
    return BackupInsight(
        weekStartDate = weekStartDate,
        weekEndDate = weekEndDate,
        generatedAt = generatedAt,
        window7Json = window7Json,
        window30Json = window30Json,
        aiResultJson = aiResultJson,
        status = status,
        errorMessage = errorMessage
    )
}

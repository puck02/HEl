package com.heldairy.core.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BackupPayload(
    @SerialName("schema_version") val schemaVersion: Int = 3,
    val entries: List<BackupEntry>,
    val insights: List<BackupInsight> = emptyList(),
    val medications: List<BackupMedication> = emptyList()
)

@Serializable
data class BackupEntry(
    @SerialName("entry_date") val entryDate: String,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("timezone_id") val timezoneId: String,
    val responses: List<BackupResponse>,
    val advice: BackupAdvice? = null,
    val summary: BackupSummary? = null
)

@Serializable
data class BackupResponse(
    @SerialName("question_id") val questionId: String,
    @SerialName("step_index") val stepIndex: Int,
    val order: Int,
    @SerialName("answer_type") val answerType: String,
    @SerialName("answer_value") val answerValue: String,
    @SerialName("answer_label") val answerLabel: String,
    @SerialName("metadata_json") val metadataJson: String? = null,
    @SerialName("answered_at") val answeredAt: Long
)

@Serializable
data class BackupAdvice(
    val model: String,
    @SerialName("advice_json") val adviceJson: String,
    @SerialName("prompt_hash") val promptHash: String,
    @SerialName("generated_at") val generatedAt: Long
)

@Serializable
data class BackupSummary(
    @SerialName("window_7_json") val window7Json: String?,
    @SerialName("window_30_json") val window30Json: String?,
    @SerialName("computed_at") val computedAt: Long
)

@Serializable
data class BackupInsight(
    @SerialName("week_start_date") val weekStartDate: String,
    @SerialName("week_end_date") val weekEndDate: String,
    @SerialName("generated_at") val generatedAt: Long,
    @SerialName("window_7_json") val window7Json: String?,
    @SerialName("window_30_json") val window30Json: String?,
    @SerialName("ai_result_json") val aiResultJson: String?,
    val status: String,
    @SerialName("error_message") val errorMessage: String? = null
)

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

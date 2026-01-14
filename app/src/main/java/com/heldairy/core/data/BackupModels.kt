package com.heldairy.core.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BackupPayload(
    @SerialName("schema_version") val schemaVersion: Int = 1,
    val entries: List<BackupEntry>
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

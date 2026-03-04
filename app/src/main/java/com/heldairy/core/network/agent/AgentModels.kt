package com.heldairy.core.network.agent

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════
// Agent API 请求 / 响应数据模型
// 与 hel-agent 后端 schemas 一一对应
// ═══════════════════════════════════════════════════════════

// ── Auth ──────────────────────────────────────────────────

@Serializable
data class AgentRegisterRequest(
    val username: String,
    val password: String,
    val email: String? = null,
    @SerialName("display_name") val displayName: String? = null
)

@Serializable
data class AgentLoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class AgentRefreshRequest(
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
data class AgentTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String = "bearer",
    @SerialName("expires_in") val expiresIn: Int = 3600
)

@Serializable
data class AgentUserResponse(
    val id: String,
    val username: String,
    val email: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
)

// ── Chat ─────────────────────────────────────────────────

@Serializable
data class AgentChatRequest(
    val message: String,
    @SerialName("session_id") val sessionId: String? = null
)

@Serializable
data class AgentChatResponse(
    val response: String,
    @SerialName("session_id") val sessionId: String,
    @SerialName("agent_used") val agentUsed: String? = null,
    @SerialName("model_used") val modelUsed: String? = null
)

// ── Health — Daily Advice ────────────────────────────────

@Serializable
data class AgentDailyAdviceRequest(
    @SerialName("today_answers") val todayAnswers: Map<String, String>,
    @SerialName("summary_7d") val summary7d: Map<String, String>? = null,
    @SerialName("active_meds_summary") val activeMedsSummary: List<String>? = null,
    @SerialName("today_med_changes") val todayMedChanges: String? = null,
    @SerialName("adherence_hint") val adherenceHint: String? = null
)

@Serializable
data class AgentDailyAdviceResponse(
    val observations: List<String> = emptyList(),
    val actions: List<String> = emptyList(),
    @SerialName("tomorrow_focus") val tomorrowFocus: List<String> = emptyList(),
    @SerialName("red_flags") val redFlags: List<String> = emptyList(),
    val model: String? = null
)

// ── Health — Follow-Up Questions ─────────────────────────

@Serializable
data class AgentFollowUpRequest(
    @SerialName("today_answers") val todayAnswers: Map<String, String>,
    @SerialName("summary_7d") val summary7d: Map<String, String>? = null,
    @SerialName("triggered_symptoms") val triggeredSymptoms: List<String> = emptyList()
)

@Serializable
data class AgentFollowUpQuestion(
    val text: String,
    val type: String,
    val options: List<String>? = null,
    @SerialName("min_value") val minValue: Int? = null,
    @SerialName("max_value") val maxValue: Int? = null
)

@Serializable
data class AgentFollowUpResponse(
    val questions: List<AgentFollowUpQuestion> = emptyList(),
    val model: String? = null
)

// ── Health — Weekly Insight ──────────────────────────────

@Serializable
data class AgentWeeklyInsightRequest(
    @SerialName("week_start_date") val weekStartDate: String,
    @SerialName("week_end_date") val weekEndDate: String,
    @SerialName("summary_7d") val summary7d: Map<String, String>,
    @SerialName("summary_30d") val summary30d: Map<String, String>? = null,
    @SerialName("active_meds_summary") val activeMedsSummary: List<String>? = null
)

@Serializable
data class AgentWeeklyInsightResponse(
    @SerialName("schema_version") val schemaVersion: Int = 1,
    @SerialName("week_start_date") val weekStartDate: String,
    @SerialName("week_end_date") val weekEndDate: String,
    val summary: String = "",
    val highlights: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val cautions: List<String> = emptyList(),
    val confidence: String = "medium",
    val model: String? = null
)

// ── Medication — NLP Parse ───────────────────────────────

@Serializable
data class AgentMedNlpParseRequest(
    @SerialName("raw_text") val rawText: String
)

@Serializable
data class AgentMedNlpParseResponse(
    val name: String,
    val aliases: List<String> = emptyList(),
    val frequency: String? = null,
    val dose: String? = null,
    @SerialName("time_hints") val timeHints: String? = null,
    val note: String? = null,
    val model: String? = null
)

// ── Medication — Info Summary ────────────────────────────

@Serializable
data class AgentMedInfoSummaryRequest(
    @SerialName("med_name") val medName: String,
    val aliases: List<String> = emptyList(),
    @SerialName("current_dose") val currentDose: String? = null,
    @SerialName("current_frequency") val currentFrequency: String? = null
)

@Serializable
data class AgentMedInfoSummaryResponse(
    val summary: String,
    val model: String? = null
)

// ── Sync ─────────────────────────────────────────────────

@Serializable
data class QuestionResponseSync(
    @SerialName("question_id") val questionId: String,
    @SerialName("step_index") val stepIndex: Int,
    @SerialName("answer_type") val answerType: String,
    @SerialName("answer_value") val answerValue: String? = null,
    @SerialName("answer_label") val answerLabel: String? = null,
    @SerialName("metadata_json") val metadataJson: Map<String, String>? = null
)

@Serializable
data class DailyAdviceSync(
    val model: String? = null,
    @SerialName("advice_json") val adviceJson: Map<String, String>? = null,
    @SerialName("prompt_hash") val promptHash: String? = null,
    @SerialName("generated_at") val generatedAt: Long? = null
)

@Serializable
data class DailySummarySync(
    @SerialName("window_7d_json") val window7dJson: Map<String, String>? = null,
    @SerialName("window_30d_json") val window30dJson: Map<String, String>? = null,
    @SerialName("computed_at") val computedAt: Long? = null
)

@Serializable
data class HealthEntrySync(
    @SerialName("android_id") val androidId: Long,
    @SerialName("entry_date") val entryDate: String,
    @SerialName("timezone_id") val timezoneId: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("question_responses") val questionResponses: List<QuestionResponseSync> = emptyList(),
    @SerialName("daily_advice") val dailyAdvice: DailyAdviceSync? = null,
    @SerialName("daily_summary") val dailySummary: DailySummarySync? = null
)

@Serializable
data class MedicationSync(
    @SerialName("android_id") val androidId: Long,
    val name: String,
    val aliases: String? = null,
    val note: String? = null,
    @SerialName("info_summary") val infoSummary: String? = null
)

@Serializable
data class MedicationCourseSync(
    @SerialName("med_android_id") val medAndroidId: Long,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String? = null,
    val status: String = "active",
    @SerialName("frequency_text") val frequencyText: String? = null,
    @SerialName("dose_text") val doseText: String? = null,
    @SerialName("time_hints") val timeHints: String? = null
)

@Serializable
data class SyncUploadRequest(
    @SerialName("last_sync_timestamp") val lastSyncTimestamp: Long = 0,
    val entries: List<HealthEntrySync> = emptyList(),
    val medications: List<MedicationSync> = emptyList(),
    @SerialName("medication_courses") val medicationCourses: List<MedicationCourseSync> = emptyList()
)

@Serializable
data class SyncResponse(
    val message: String,
    @SerialName("entries_synced") val entriesSynced: Int = 0,
    @SerialName("medications_synced") val medicationsSynced: Int = 0,
    @SerialName("server_timestamp") val serverTimestamp: Long = 0
)

@Serializable
data class SyncStatusResponse(
    @SerialName("last_sync_timestamp") val lastSyncTimestamp: Long = 0,
    @SerialName("total_entries") val totalEntries: Int = 0,
    @SerialName("total_medications") val totalMedications: Int = 0
)

package com.heldairy.core.network.agent

import android.util.Log
import com.heldairy.core.data.AdvicePayload
import com.heldairy.core.data.AdviceSource
import com.heldairy.core.data.AiFollowUpQuestionDto
import com.heldairy.core.data.WeeklyInsightPayload
import com.heldairy.core.network.NetworkMonitor
import com.heldairy.core.network.NetworkUnavailableException
import com.heldairy.core.preferences.AgentPreferencesStore
import com.heldairy.feature.medication.MedicationNlpResult
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException

/**
 * hel-agent 后端高级封装
 *
 * 与 [DeepSeekClient] 对等的角色：Coordinator 可择一调用。
 * - 自动检查网络
 * - 自动检查登录状态
 * - 将 Agent 响应转换为业务模型（AdvicePayload、WeeklyInsightPayload 等）
 */
class AgentClient(
    private val api: AgentApi,
    private val networkMonitor: NetworkMonitor,
    private val agentPrefs: AgentPreferencesStore
) {
    companion object {
        private const val TAG = "AgentClient"
    }

    // ── 前置检查 ──────────────────────────────────────────

    private suspend fun ensureReady() {
        if (!networkMonitor.isCurrentlyConnected()) {
            throw NetworkUnavailableException("无网络连接")
        }
        val settings = agentPrefs.currentSettings()
        if (!settings.isReady) {
            throw AgentNotReadyException(
                when {
                    !settings.isServerConfigured -> "未配置 Agent 服务器地址"
                    !settings.isLoggedIn -> "未登录 Agent"
                    !settings.agentEnabled -> "Agent 功能已关闭"
                    else -> "Agent 未就绪"
                }
            )
        }
    }

    /** Agent 是否可用（快速、非挂起检查） */
    fun isAvailable(): Boolean {
        return try {
            networkMonitor.isCurrentlyConnected()
        } catch (_: Exception) {
            false
        }
    }

    // ── Auth ──────────────────────────────────────────────

    suspend fun register(
        username: String,
        password: String,
        email: String,
        displayName: String? = null
    ): Result<AgentTokenResponse> {
        return try {
            Result.success(
                api.register(
                    AgentRegisterRequest(username, password, email, displayName)
                )
            )
        } catch (t: Throwable) {
            Result.failure(mapAuthThrowable(t))
        }
    }

    suspend fun login(username: String, password: String): Result<AgentTokenResponse> {
        return try {
            Result.success(api.login(AgentLoginRequest(username, password)))
        } catch (t: Throwable) {
            Result.failure(mapAuthThrowable(t))
        }
    }

    private fun mapAuthThrowable(t: Throwable): Throwable {
        if (t is HttpException) {
            val body = runCatching { t.response()?.errorBody()?.string().orEmpty() }.getOrDefault("")
            val msg = when (t.code()) {
                401 -> "用户名或密码错误"
                409 -> when {
                    body.contains("Username already exists", ignoreCase = true) -> "用户名已存在，请直接登录"
                    body.contains("Email already registered", ignoreCase = true) -> "邮箱已被注册，请直接登录或更换邮箱"
                    else -> "账号已存在，请直接登录"
                }
                503 -> "服务器暂时不可用，请稍后重试"
                500 -> "服务器内部错误，请稍后重试"
                else -> "请求失败（HTTP ${t.code()}）"
            }
            return IllegalStateException(msg, t)
        }

        if (t is ConnectException || t is SocketTimeoutException || t is UnknownHostException) {
            val raw = t.message.orEmpty()
            val hint = if (raw.contains("10.0.2.2")) {
                "连接失败：10.0.2.2 仅用于 Android 模拟器；真机请使用电脑局域网 IP（如 http://172.20.x.x:8011）"
            } else {
                "连接失败：请检查服务器地址与端口，确保后端已启动"
            }
            return IllegalStateException(hint, t)
        }

        return IllegalStateException(t.message ?: "请求失败", t)
    }

    // ── Chat ──────────────────────────────────────────────

    suspend fun chat(message: String, sessionId: String? = null): Result<AgentChatResponse> {
        ensureReady()
        return try {
            Result.success(api.chat(AgentChatRequest(message, sessionId)))
        } catch (t: Throwable) {
            if (t is HttpException && t.code() == 401) {
                runCatching { agentPrefs.clearLogin() }
                Result.failure(IllegalStateException("登录已失效，请重新登录 Agent", t))
            } else {
                Result.failure(t)
            }
        }
    }

    // ── Health — Daily Advice ────────────────────────────

    /**
     * 通过 Agent 生成每日建议，返回与 DeepSeekClient 兼容的 [AdvicePayload]。
     */
    suspend fun fetchAdvice(
        todayAnswers: Map<String, String>,
        summary7d: Map<String, String>? = null,
        activeMedsSummary: List<String>? = null,
        todayMedChanges: String? = null,
        adherenceHint: String? = null
    ): AdvicePayload {
        ensureReady()
        val resp = api.dailyAdvice(
            AgentDailyAdviceRequest(
                todayAnswers = todayAnswers,
                summary7d = summary7d,
                activeMedsSummary = activeMedsSummary,
                todayMedChanges = todayMedChanges,
                adherenceHint = adherenceHint
            )
        )
        return AdvicePayload(
            observations = resp.observations,
            actions = resp.actions,
            tomorrowFocus = resp.tomorrowFocus,
            redFlags = resp.redFlags,
            source = AdviceSource.AI
        )
    }

    // ── Health — Follow-Up ───────────────────────────────

    suspend fun fetchFollowUpQuestions(
        todayAnswers: Map<String, String>,
        summary7d: Map<String, String>? = null,
        triggeredSymptoms: List<String> = emptyList()
    ): List<AiFollowUpQuestionDto> {
        ensureReady()
        val resp = api.followUp(
            AgentFollowUpRequest(todayAnswers, summary7d, triggeredSymptoms)
        )
        return resp.questions.map { q ->
            AiFollowUpQuestionDto(
                text = q.text,
                type = q.type,
                options = q.options
            )
        }
    }

    // ── Health — Weekly Insight ──────────────────────────

    suspend fun fetchWeeklyInsight(
        weekStartDate: String,
        weekEndDate: String,
        summary7d: Map<String, String>,
        summary30d: Map<String, String>? = null,
        activeMedsSummary: List<String>? = null
    ): WeeklyInsightPayload {
        ensureReady()
        val resp = api.weeklyInsight(
            AgentWeeklyInsightRequest(
                weekStartDate = weekStartDate,
                weekEndDate = weekEndDate,
                summary7d = summary7d,
                summary30d = summary30d,
                activeMedsSummary = activeMedsSummary
            )
        )
        return WeeklyInsightPayload(
            summary = resp.summary,
            highlights = resp.highlights,
            suggestions = resp.suggestions,
            cautions = resp.cautions,
            confidence = resp.confidence
        )
    }

    // ── Medication — NLP Parse ───────────────────────────

    suspend fun parseMedicationNlp(rawText: String): MedicationNlpResult {
        ensureReady()
        val resp = api.medicationParseNlp(AgentMedNlpParseRequest(rawText))

        val primaryAction = resp.actions.firstOrNull {
            it.actionType == "start_course" || it.actionType == "update_course" || it.actionType == "add_med"
        } ?: resp.actions.firstOrNull()

        val medName = primaryAction?.medName ?: resp.mentionedMeds.firstOrNull()?.name.orEmpty()
        if (medName.isBlank()) {
            throw IllegalStateException("Agent 未识别到药物名称")
        }

        val courseFields = primaryAction?.courseFields.orEmpty()
        val frequency = courseFields["frequencyText"] ?: courseFields["frequency_text"]
        val dose = courseFields["doseText"] ?: courseFields["dose_text"]
        val timeHints = courseFields["timeHints"] ?: courseFields["time_hints"]
        val note = resp.questions.firstOrNull()?.text

        return MedicationNlpResult(
            name = medName,
            aliases = emptyList(),
            frequency = frequency,
            dose = dose,
            timeHints = timeHints,
            note = note
        )
    }

    // ── Medication — Info Summary ────────────────────────

    suspend fun fetchMedInfoSummary(
        medName: String,
        aliases: List<String> = emptyList(),
        currentDose: String? = null,
        currentFrequency: String? = null
    ): String {
        ensureReady()
        val text = buildString {
            append("药品名称：")
            append(medName)
            if (aliases.isNotEmpty()) {
                append("\n别名：")
                append(aliases.joinToString("，"))
            }
            if (!currentDose.isNullOrBlank()) {
                append("\n当前剂量：")
                append(currentDose)
            }
            if (!currentFrequency.isNullOrBlank()) {
                append("\n当前频率：")
                append(currentFrequency)
            }
        }
        val resp = api.medicationInfoSummary(
            AgentMedInfoSummaryRequest(text = text, medName = medName)
        )
        return listOfNotNull(
            resp.dosageSummary?.takeIf { it.isNotBlank() }?.let { "用法用量：$it" },
            resp.cautionsSummary?.takeIf { it.isNotBlank() }?.let { "注意事项：$it" },
            resp.adverseSummary?.takeIf { it.isNotBlank() }?.let { "不良反应：$it" }
        ).joinToString("\n")
    }

    // ── Sync ─────────────────────────────────────────────

    suspend fun syncUpload(request: SyncUploadRequest): SyncResponse {
        ensureReady()
        return api.syncUpload(request)
    }

    suspend fun syncPush(request: SyncPushRequest): SyncPushResponse {
        ensureReady()
        return api.syncPush(request)
    }

    suspend fun syncPull(since: Long, limit: Int = 200): SyncPullResponse {
        ensureReady()
        return api.syncPull(since = since, limit = limit)
    }

    suspend fun syncStatus(): SyncStatusResponse {
        ensureReady()
        return api.syncStatus()
    }

    suspend fun upsertPushToken(token: String, platform: String = "android"): Result<Unit> {
        ensureReady()
        return runCatching {
            api.upsertPushToken(PushTokenUpsertRequest(token = token, platform = platform))
        }.map { }
    }

    suspend fun deletePushToken(token: String): Result<Unit> {
        ensureReady()
        return runCatching {
            api.deletePushToken(PushTokenDeleteRequest(token = token))
        }.map { }
    }

    // ── Clear Data ───────────────────────────────────────

    /** 清除服务器上当前用户的所有数据 */
    suspend fun clearUserData(): Result<Unit> {
        ensureReady()
        return runCatching { api.clearUserData() }.map { }
    }

    // ── Health Check ─────────────────────────────────────

    /** 检测 Agent 服务器是否可达 */
    suspend fun ping(): Boolean {
        return try {
            api.healthCheck()
            true
        } catch (e: Exception) {
            Log.w(TAG, "Agent ping failed: ${e.message}")
            false
        }
    }
}

class AgentNotReadyException(message: String) : RuntimeException(message)

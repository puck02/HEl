package com.heldairy.core.network.agent

import com.heldairy.core.network.agent.AgentChatRequest
import com.heldairy.core.network.agent.AgentChatResponse
import com.heldairy.core.network.agent.AgentDailyAdviceRequest
import com.heldairy.core.network.agent.AgentDailyAdviceResponse
import com.heldairy.core.network.agent.AgentFollowUpRequest
import com.heldairy.core.network.agent.AgentFollowUpResponse
import com.heldairy.core.network.agent.AgentLoginRequest
import com.heldairy.core.network.agent.AgentMedInfoSummaryRequest
import com.heldairy.core.network.agent.AgentMedInfoSummaryResponse
import com.heldairy.core.network.agent.AgentMedNlpParseRequest
import com.heldairy.core.network.agent.AgentMedNlpParseResponse
import com.heldairy.core.network.agent.AgentRefreshRequest
import com.heldairy.core.network.agent.AgentRegisterRequest
import com.heldairy.core.network.agent.AgentTokenResponse
import com.heldairy.core.network.agent.AgentUserResponse
import com.heldairy.core.network.agent.AgentWeeklyInsightRequest
import com.heldairy.core.network.agent.AgentWeeklyInsightResponse
import com.heldairy.core.network.agent.SyncResponse
import com.heldairy.core.network.agent.SyncStatusResponse
import com.heldairy.core.network.agent.SyncUploadRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * hel-agent 后端 REST API 定义
 *
 * Base URL 由用户在设置中配置（默认 http://server-ip:8000）。
 * JWT token 由 [AgentAuthInterceptor] 自动注入，此处不声明 Header。
 */
interface AgentApi {

    // ── Auth（不需要 JWT） ────────────────────────────────

    @POST("auth/register")
    suspend fun register(@Body body: AgentRegisterRequest): AgentTokenResponse

    @POST("auth/login")
    suspend fun login(@Body body: AgentLoginRequest): AgentTokenResponse

    @POST("auth/refresh")
    suspend fun refreshToken(@Body body: AgentRefreshRequest): AgentTokenResponse

    @GET("auth/me")
    suspend fun me(): AgentUserResponse

    // ── Chat ─────────────────────────────────────────────

    @POST("api/v1/chat")
    suspend fun chat(@Body body: AgentChatRequest): AgentChatResponse

    // ── Health ───────────────────────────────────────────

    @POST("api/v1/health/daily-advice")
    suspend fun dailyAdvice(@Body body: AgentDailyAdviceRequest): AgentDailyAdviceResponse

    @POST("api/v1/health/follow-up")
    suspend fun followUp(@Body body: AgentFollowUpRequest): AgentFollowUpResponse

    @POST("api/v1/health/weekly-insight")
    suspend fun weeklyInsight(@Body body: AgentWeeklyInsightRequest): AgentWeeklyInsightResponse

    // ── Medication ───────────────────────────────────────

    @POST("api/v1/medication/parse-nlp")
    suspend fun medicationParseNlp(@Body body: AgentMedNlpParseRequest): AgentMedNlpParseResponse

    @POST("api/v1/medication/info-summary")
    suspend fun medicationInfoSummary(@Body body: AgentMedInfoSummaryRequest): AgentMedInfoSummaryResponse

    // ── Sync ─────────────────────────────────────────────

    @POST("api/v1/sync/upload")
    suspend fun syncUpload(@Body body: SyncUploadRequest): SyncResponse

    @POST("api/v1/sync/push")
    suspend fun syncPush(@Body body: SyncPushRequest): SyncPushResponse

    @GET("api/v1/sync/pull")
    suspend fun syncPull(
        @Query("since") since: Long,
        @Query("limit") limit: Int = 200
    ): SyncPullResponse

    @GET("api/v1/sync/status")
    suspend fun syncStatus(): SyncStatusResponse

    // ── Health Check ─────────────────────────────────────

    @GET("health")
    suspend fun healthCheck(): Map<String, String>
}

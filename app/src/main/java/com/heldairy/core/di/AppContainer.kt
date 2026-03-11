package com.heldairy.core.di

import android.content.Context
import com.heldairy.core.data.DailyAdviceCoordinator
import com.heldairy.core.data.AiFollowUpCoordinator
import com.heldairy.core.data.DailyReportRepository
import com.heldairy.core.data.DailySummaryManager
import com.heldairy.core.data.BackupManager
import com.heldairy.core.data.DataSyncManager
import com.heldairy.core.data.InsightRepository
import com.heldairy.core.data.WeeklyInsightCoordinator
import com.heldairy.core.data.AdviceTrackingRepository
import com.heldairy.core.data.LocalAdvisorEngine
import com.heldairy.core.data.DoctorReportRepository
import com.heldairy.core.database.DailyReportDatabase
import com.heldairy.core.network.DeepSeekApi
import com.heldairy.core.network.DeepSeekClient
import com.heldairy.core.network.NetworkMonitor
import com.heldairy.core.network.RetryInterceptor
import com.heldairy.core.network.agent.AgentApi
import com.heldairy.core.network.agent.AgentAuthInterceptor
import com.heldairy.core.network.agent.AgentClient
import com.heldairy.core.preferences.AgentPreferencesStore
import com.heldairy.core.preferences.AiPreferencesStore
import com.heldairy.core.preferences.DailyReportPreferencesStore
import com.heldairy.core.preferences.UserProfileStore
import com.heldairy.core.preferences.SecurePreferencesStore
import com.heldairy.core.util.Constants
import com.heldairy.feature.medication.MedicationRepository
import com.heldairy.feature.medication.MedicationNlpParser
import com.heldairy.feature.medication.MedicationInfoSummaryGenerator
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

interface AppContainer {
    val database: DailyReportDatabase
    val dailyReportRepository: DailyReportRepository
    val dailySummaryManager: DailySummaryManager
    val adviceCoordinator: DailyAdviceCoordinator
    val followUpCoordinator: AiFollowUpCoordinator
    val insightRepository: InsightRepository
    val weeklyInsightCoordinator: WeeklyInsightCoordinator
    val backupManager: BackupManager
    val aiPreferencesStore: AiPreferencesStore
    val userProfileStore: UserProfileStore
    val medicationRepository: MedicationRepository
    val medicationNlpParser: MedicationNlpParser
    val medicationInfoSummaryGenerator: MedicationInfoSummaryGenerator
    val doctorReportRepository: DoctorReportRepository
    val dailyReportPreferencesStore: DailyReportPreferencesStore
    val networkMonitor: NetworkMonitor
    val securePreferencesStore: SecurePreferencesStore

    // ── Agent (hel-agent) ────────────────────────────────
    val agentPreferencesStore: AgentPreferencesStore
    val agentClient: AgentClient?        // null if server URL not configured
    val dataSyncManager: DataSyncManager?
}

class AppContainerImpl(context: Context) : AppContainer {
    private val appContext: Context = context.applicationContext

    override val database = DailyReportDatabase.build(context)
    private val json = Json { ignoreUnknownKeys = true }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(Constants.Network.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(Constants.Network.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(Constants.Network.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(Constants.Network.CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(RetryInterceptor(maxRetries = Constants.Network.RETRY_MAX_ATTEMPTS, baseDelayMs = Constants.Network.RETRY_BASE_DELAY_MS))
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(Constants.Network.DEEPSEEK_BASE_URL)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .client(okHttpClient)
        .build()

    private val deepSeekApi: DeepSeekApi = retrofit.create(DeepSeekApi::class.java)

    override val dailyReportRepository: DailyReportRepository =
        DailyReportRepository(database.dailyReportDao())

    override val dailySummaryManager: DailySummaryManager =
        DailySummaryManager(dailyReportRepository)

    override val medicationRepository: MedicationRepository =
        MedicationRepository(database.medicationDao())

    override val medicationNlpParser: MedicationNlpParser =
        MedicationNlpParser(deepSeekApi, json)
    
    override val medicationInfoSummaryGenerator: MedicationInfoSummaryGenerator =
        MedicationInfoSummaryGenerator(deepSeekApi, json)
    
    override val userProfileStore: UserProfileStore = UserProfileStore(context)
    override val aiPreferencesStore: AiPreferencesStore = AiPreferencesStore(context)
    override val networkMonitor: NetworkMonitor = NetworkMonitor(context)
    override val securePreferencesStore: SecurePreferencesStore = SecurePreferencesStore(context)

    private val deepSeekClient = DeepSeekClient(deepSeekApi, networkMonitor)

    private val adviceTrackingRepository: AdviceTrackingRepository = AdviceTrackingRepository(
        dao = database.dailyReportDao()
    )

    private val localEngine = LocalAdvisorEngine()

    override val adviceCoordinator: DailyAdviceCoordinator = DailyAdviceCoordinator(
        repository = dailyReportRepository,
        summaryManager = dailySummaryManager,
        preferencesStore = aiPreferencesStore,
        deepSeekClient = deepSeekClient,
        trackingRepository = adviceTrackingRepository,
        localEngine = localEngine,
        agentClient = agentClient   // Agent 优先路径
    )

    override val followUpCoordinator: AiFollowUpCoordinator = AiFollowUpCoordinator(
        preferencesStore = aiPreferencesStore,
        deepSeekClient = deepSeekClient,
        agentClient = agentClient   // Agent 优先路径
    )

    override val insightRepository: InsightRepository = InsightRepository(
        dailyReportRepository = dailyReportRepository,
        dao = database.dailyReportDao()
    )

    override val weeklyInsightCoordinator: WeeklyInsightCoordinator = WeeklyInsightCoordinator(
        insightRepository = insightRepository,
        preferencesStore = aiPreferencesStore,
        deepSeekClient = deepSeekClient,
        agentClient = agentClient   // Agent 优先路径
    )

    override val backupManager: BackupManager = BackupManager(
        repository = dailyReportRepository,
        insightRepository = insightRepository,
        medicationRepository = medicationRepository,
        json = json
    )

    override val doctorReportRepository: DoctorReportRepository = DoctorReportRepository(
        insightRepository = insightRepository,
        medicationRepository = medicationRepository,
        medicationDao = database.medicationDao(),
        dailyReportDao = database.dailyReportDao()
    )

    override val dailyReportPreferencesStore: DailyReportPreferencesStore = DailyReportPreferencesStore(context)

    // ── Agent (hel-agent) 基础设施 ──────────────────────

    override val agentPreferencesStore: AgentPreferencesStore = AgentPreferencesStore(context)

    /**
     * Agent HTTP 客户端 — 独立于 DeepSeek OkHttpClient。
     * 内置 JWT 自动注入 + 401 刷新逻辑。
     * agentApi 通过 lateinit + lazy 解决 Interceptor↔Api 循环依赖。
     */
    private lateinit var _agentApi: AgentApi

    private val agentOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(Constants.Network.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Constants.Network.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(Constants.Network.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(Constants.Network.CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(AgentAuthInterceptor(agentPreferencesStore) { _agentApi })
            .addInterceptor(RetryInterceptor(maxRetries = Constants.Network.RETRY_MAX_ATTEMPTS, baseDelayMs = Constants.Network.RETRY_BASE_DELAY_MS))
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            .build()
    }

    private fun buildAgentRetrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .client(agentOkHttpClient)
        .build()

    /**
     * AgentApi + AgentClient — 仅在服务器 URL 已配置时创建。
     * 运行时可通过 [rebuildAgentClient] 重建（用户更改服务器地址后）。
     */
    private var _agentClient: AgentClient? = null

    override val agentClient: AgentClient?
        get() = _agentClient

    override val dataSyncManager: DataSyncManager?
        get() = _agentClient?.let { client ->
            DataSyncManager(
                database = database,
                agentClient = client,
                agentPrefs = agentPreferencesStore,
                appContext = appContext
            )
        }

    /** 当用户配置服务器地址后调用，创建/重建 AgentClient */
    fun rebuildAgentClient(serverUrl: String) {
        if (serverUrl.isBlank()) {
            _agentClient = null
            return
        }
        val agentRetrofit = buildAgentRetrofit(serverUrl.trimEnd('/') + "/")
        _agentApi = agentRetrofit.create(AgentApi::class.java)
        _agentClient = AgentClient(_agentApi, networkMonitor, agentPreferencesStore)
    }

    init {
        // 尝试用已保存的 URL 初始化 Agent
        kotlinx.coroutines.runBlocking {
            val savedUrl = agentPreferencesStore.currentSettings().serverUrl
            if (savedUrl.isNotBlank()) {
                rebuildAgentClient(savedUrl)
            }
        }
    }
}

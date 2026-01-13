package com.heldairy.core.di

import android.content.Context
import com.heldairy.core.data.DailyAdviceCoordinator
import com.heldairy.core.data.DailyReportRepository
import com.heldairy.core.data.DailySummaryManager
import com.heldairy.core.database.DailyReportDatabase
import com.heldairy.core.network.DeepSeekApi
import com.heldairy.core.network.DeepSeekClient
import com.heldairy.core.preferences.AiPreferencesStore
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

interface AppContainer {
    val dailyReportRepository: DailyReportRepository
    val dailySummaryManager: DailySummaryManager
    val adviceCoordinator: DailyAdviceCoordinator
    val aiPreferencesStore: AiPreferencesStore
}

class AppContainerImpl(context: Context) : AppContainer {
    private val database = DailyReportDatabase.build(context)
    private val json = Json { ignoreUnknownKeys = true }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://api.deepseek.com/")
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .client(okHttpClient)
        .build()

    private val deepSeekApi: DeepSeekApi = retrofit.create(DeepSeekApi::class.java)

    override val dailyReportRepository: DailyReportRepository =
        DailyReportRepository(database.dailyReportDao())

    override val dailySummaryManager: DailySummaryManager =
        DailySummaryManager(dailyReportRepository)

    override val aiPreferencesStore: AiPreferencesStore = AiPreferencesStore(context)

    private val deepSeekClient = DeepSeekClient(deepSeekApi)

    override val adviceCoordinator: DailyAdviceCoordinator = DailyAdviceCoordinator(
        repository = dailyReportRepository,
        summaryManager = dailySummaryManager,
        preferencesStore = aiPreferencesStore,
        deepSeekClient = deepSeekClient
    )
}

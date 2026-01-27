package com.heldairy.core.data

import com.heldairy.core.database.DailyReportDao
import com.heldairy.core.database.entity.InsightReportEntity
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class InsightRepository(
    private val dailyReportRepository: DailyReportRepository,
    private val dao: DailyReportDao,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val json: Json = Json { encodeDefaults = true }
) {

    suspend fun buildLocalSummary(endDate: LocalDate = LocalDate.now(clock)): InsightLocalSummary = withContext(ioDispatcher) {
        val entries = dailyReportRepository.loadRecentEntries(limit = 40)
        InsightCalculator.buildSummary(entries, endDate)
    }

    suspend fun saveInsightReport(
        weekStart: LocalDate,
        weekEnd: LocalDate,
        summary: InsightLocalSummary?,
        aiPayload: WeeklyInsightPayload?,
        status: String,
        errorMessage: String? = null
    ) = withContext(ioDispatcher) {
        val entity = InsightReportEntity(
            weekStartDate = weekStart.toString(),
            weekEndDate = weekEnd.toString(),
            generatedAt = clock.instant().toEpochMilli(),
            window7Json = summary?.window7?.let { json.encodeToString(InsightWindow.serializer(), it) },
            window30Json = summary?.window30?.let { json.encodeToString(InsightWindow.serializer(), it) },
            aiResultJson = aiPayload?.let { json.encodeToString(WeeklyInsightPayload.serializer(), it) },
            status = status,
            errorMessage = errorMessage
        )
        dao.upsertInsight(entity)
    }

    suspend fun findInsightForWeek(weekStart: LocalDate): InsightReportEntity? = withContext(ioDispatcher) {
        dao.findInsightByWeekStart(weekStart.toString())
    }

    suspend fun latestInsight(): InsightReportEntity? = withContext(ioDispatcher) {
        dao.latestInsight()
    }

    suspend fun loadAllInsights(): List<InsightReportEntity> = withContext(ioDispatcher) {
        dao.loadAllInsights()
    }

    suspend fun restoreInsight(report: InsightReportEntity) = withContext(ioDispatcher) {
        dao.upsertInsight(report)
    }
}

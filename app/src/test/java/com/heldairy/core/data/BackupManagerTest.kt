package com.heldairy.core.data

import com.heldairy.core.database.DailyReportDao
import com.heldairy.core.database.entity.DailyAdviceEntity
import com.heldairy.core.database.entity.DailyEntryEntity
import com.heldairy.core.database.entity.DailyEntrySnapshot
import com.heldairy.core.database.entity.DailyEntryWithResponses
import com.heldairy.core.database.entity.DailySummaryEntity
import com.heldairy.core.database.entity.QuestionResponseEntity
import com.heldairy.core.database.entity.InsightReportEntity
import java.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManagerTest {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    @Test
    fun exportThenImportRestoresSnapshots() = runBlocking {
        val sourceDao = FakeDailyReportDao()
        val sourceRepo = DailyReportRepository(sourceDao, Dispatchers.Unconfined)
        val sourceInsightRepo = InsightRepository(
            dailyReportRepository = sourceRepo,
            dao = sourceDao,
            clock = Clock.systemUTC(),
            ioDispatcher = Dispatchers.Unconfined,
            json = json
        )
        val manager = BackupManager(sourceRepo, sourceInsightRepo, json)

        val entryId = sourceRepo.recordDailyReport(
            entryDate = "2026-01-14",
            timezoneId = "UTC",
            answers = listOf(
                DailyAnswerRecord(
                    questionId = "q1",
                    stepIndex = 0,
                    order = 0,
                    answerType = "single",
                    answerValue = "walk",
                    answerLabel = "Walked 20min"
                ),
                DailyAnswerRecord(
                    questionId = "q2",
                    stepIndex = 1,
                    order = 1,
                    answerType = "text",
                    answerValue = "note",
                    answerLabel = "note"
                )
            ),
            createdAtMillis = 100L
        ).getOrThrow()
        sourceRepo.saveAdvice(
            DailyAdviceEntity(
                entryId = entryId,
                entryDate = "2026-01-14",
                model = "model-x",
                adviceJson = "{\"actions\":[]}",
                promptHash = "hash123",
                generatedAt = 200L
            )
        )
        sourceRepo.saveSummary(
            DailySummaryEntity(
                entryId = entryId,
                entryDate = "2026-01-14",
                window7Json = "win7",
                window30Json = "win30",
                computedAt = 250L
            )
        )

        val exported = manager.exportJson()

        val targetDao = FakeDailyReportDao()
        val targetRepo = DailyReportRepository(targetDao, Dispatchers.Unconfined)
        val targetInsightRepo = InsightRepository(
            dailyReportRepository = targetRepo,
            dao = targetDao,
            clock = Clock.systemUTC(),
            ioDispatcher = Dispatchers.Unconfined,
            json = json
        )
        val targetManager = BackupManager(targetRepo, targetInsightRepo, json)

        val result = targetManager.importJson(exported)
        assertTrue(result.isSuccess)

        val restored = targetRepo.loadAllSnapshots()
        assertEquals(1, restored.size)
        val snapshot = restored.first()
        assertEquals("2026-01-14", snapshot.entry.entryDate)
        assertEquals("UTC", snapshot.entry.timezoneId)
        val responses = snapshot.responses.sortedBy { it.questionOrder }
        assertEquals("q1", responses[0].questionId)
        assertEquals("walk", responses[0].answerValue)
        assertEquals("q2", responses[1].questionId)
        assertEquals("note", responses[1].answerLabel)
        assertEquals("model-x", snapshot.advice?.model)
        assertEquals("win7", snapshot.summary?.window7Json)
    }

    @Test
    fun importClearsExistingDataBeforeRestore() = runBlocking {
        val dao = FakeDailyReportDao()
        val repo = DailyReportRepository(dao, Dispatchers.Unconfined)
        val insightRepo = InsightRepository(
            dailyReportRepository = repo,
            dao = dao,
            clock = Clock.systemUTC(),
            ioDispatcher = Dispatchers.Unconfined,
            json = json
        )
        val manager = BackupManager(repo, insightRepo, json)

        repo.recordDailyReport(
            entryDate = "2026-01-10",
            timezoneId = "UTC",
            answers = listOf(
                DailyAnswerRecord(
                    questionId = "old",
                    stepIndex = 0,
                    order = 0,
                    answerType = "text",
                    answerValue = "before",
                    answerLabel = "before"
                )
            ),
            createdAtMillis = 10L
        )

        val payload = BackupPayload(
            entries = listOf(
                BackupEntry(
                    entryDate = "2026-02-01",
                    createdAt = 50L,
                    timezoneId = "UTC",
                    responses = listOf(
                        BackupResponse(
                            questionId = "new",
                            stepIndex = 0,
                            order = 0,
                            answerType = "text",
                            answerValue = "after",
                            answerLabel = "after",
                            answeredAt = 50L
                        )
                    )
                )
            )
        )
        val raw = json.encodeToString(payload)

        val result = manager.importJson(raw)
        assertTrue(result.isSuccess)

        val snapshots = repo.loadAllSnapshots()
        assertEquals(1, snapshots.size)
        assertEquals("2026-02-01", snapshots.first().entry.entryDate)
        assertEquals("after", snapshots.first().responses.first().answerValue)
    }
}

private class FakeDailyReportDao : DailyReportDao {
    private val entries = mutableListOf<DailyEntryEntity>()
    private val responses = mutableListOf<QuestionResponseEntity>()
    private val advice = mutableListOf<DailyAdviceEntity>()
    private val summaries = mutableListOf<DailySummaryEntity>()
    private val insights = mutableListOf<InsightReportEntity>()

    private var nextEntryId = 1L
    private var nextResponseId = 1L
    private var nextAdviceId = 1L
    private var nextSummaryId = 1L
    private var nextInsightId = 1L

    private val latestEntryFlow = MutableStateFlow<DailyEntryWithResponses?>(null)
    private val latestSnapshotFlow = MutableStateFlow<DailyEntrySnapshot?>(null)

    override suspend fun insertEntry(entry: DailyEntryEntity): Long {
        val withId = entry.copy(id = nextEntryId++)
        entries += withId
        refreshLatest()
        return withId.id
    }

    override suspend fun upsertResponses(responses: List<QuestionResponseEntity>) {
        responses.forEach { response ->
            val responseId = if (response.id == 0L) nextResponseId++ else response.id
            this.responses.removeAll { it.id == responseId }
            this.responses += response.copy(id = responseId)
        }
        refreshLatest()
    }

    override suspend fun upsertAdvice(advice: DailyAdviceEntity) {
        val adviceId = if (advice.id == 0L) nextAdviceId++ else advice.id
        this.advice.removeAll { it.entryId == advice.entryId }
        this.advice += advice.copy(id = adviceId)
        refreshLatest()
    }

    override suspend fun upsertSummary(summary: DailySummaryEntity) {
        val summaryId = if (summary.id == 0L) nextSummaryId++ else summary.id
        summaries.removeAll { it.entryId == summary.entryId }
        summaries += summary.copy(id = summaryId)
        refreshLatest()
    }

    override suspend fun upsertInsight(report: InsightReportEntity) {
        val insightId = if (report.id == 0L) nextInsightId++ else report.id
        insights.removeAll { it.weekStartDate == report.weekStartDate }
        insights += report.copy(id = insightId)
    }

    override suspend fun findEntryIdByDate(entryDate: String): Long? {
        return entries.firstOrNull { it.entryDate == entryDate }?.id
    }

    override suspend fun deleteEntryById(entryId: Long) {
        entries.removeAll { it.id == entryId }
        responses.removeAll { it.entryId == entryId }
        advice.removeAll { it.entryId == entryId }
        summaries.removeAll { it.entryId == entryId }
        refreshLatest()
    }

    override fun observeLatestEntry(): Flow<DailyEntryWithResponses?> = latestEntryFlow

    override fun observeLatestSnapshot(): Flow<DailyEntrySnapshot?> = latestSnapshotFlow

    override suspend fun loadRecentEntries(limit: Int): List<DailyEntryWithResponses> {
        return entries
            .sortedByDescending { it.createdAt }
            .take(limit)
            .map { entry -> DailyEntryWithResponses(entry, responsesFor(entry.id)) }
    }

    override suspend fun getEntryWithResponses(entryId: Long): DailyEntryWithResponses? {
        return entries.firstOrNull { it.id == entryId }?.let { entry ->
            DailyEntryWithResponses(entry, responsesFor(entry.id))
        }
    }

    override suspend fun findInsightByWeekStart(weekStart: String): InsightReportEntity? {
        return insights.firstOrNull { it.weekStartDate == weekStart }
    }

    override suspend fun latestInsight(): InsightReportEntity? {
        return insights.maxByOrNull { it.weekEndDate }
    }

    override suspend fun loadAllInsights(): List<InsightReportEntity> {
        return insights.sortedByDescending { it.weekEndDate }
    }

    override suspend fun loadAllSnapshots(): List<DailyEntrySnapshot> {
        return entries
            .sortedByDescending { it.createdAt }
            .map { entry ->
                DailyEntrySnapshot(
                    entry = entry,
                    responses = responsesFor(entry.id),
                    advice = advice.firstOrNull { it.entryId == entry.id },
                    summary = summaries.firstOrNull { it.entryId == entry.id }
                )
            }
    }

    override suspend fun clearAdvice() {
        advice.clear()
        refreshLatest()
    }

    override suspend fun clearSummaries() {
        summaries.clear()
        refreshLatest()
    }

    override suspend fun clearInsights() {
        insights.clear()
    }

    override suspend fun clearResponses() {
        responses.clear()
        refreshLatest()
    }

    override suspend fun clearEntries() {
        entries.clear()
        refreshLatest()
    }

    private fun responsesFor(entryId: Long): List<QuestionResponseEntity> {
        return responses.filter { it.entryId == entryId }.sortedBy { it.questionOrder }
    }

    private fun refreshLatest() {
        val latest = entries.maxByOrNull { it.createdAt }
        val latestResponses = latest?.let { responsesFor(it.id) } ?: emptyList()
        latestEntryFlow.value = latest?.let { DailyEntryWithResponses(it, latestResponses) }
        latestSnapshotFlow.value = latest?.let { entry ->
            DailyEntrySnapshot(
                entry = entry,
                responses = latestResponses,
                advice = advice.firstOrNull { it.entryId == entry.id },
                summary = summaries.firstOrNull { it.entryId == entry.id }
            )
        }
    }
}

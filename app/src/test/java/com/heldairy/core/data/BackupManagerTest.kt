package com.heldairy.core.data

import com.heldairy.core.database.DailyReportDao
import com.heldairy.core.database.entity.DailyAdviceEntity
import com.heldairy.core.database.entity.DailyEntryEntity
import com.heldairy.core.database.entity.DailyEntrySnapshot
import com.heldairy.core.database.entity.DailyEntryWithResponses
import com.heldairy.core.database.entity.DailySummaryEntity
import com.heldairy.core.database.entity.QuestionResponseEntity
import com.heldairy.core.database.entity.InsightReportEntity
import com.heldairy.core.database.entity.AdviceTrackingEntity
import java.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManagerTest {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    private val fakeMedicationDao = FakeMedicationDao()
    private val fakeMedicationRepo = com.heldairy.feature.medication.MedicationRepository(fakeMedicationDao)

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
        val manager = BackupManager(sourceRepo, sourceInsightRepo, fakeMedicationRepo, json)

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
        val targetManager = BackupManager(targetRepo, targetInsightRepo, fakeMedicationRepo, json)

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
        val manager = BackupManager(repo, insightRepo, fakeMedicationRepo, json)

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

    override fun observeEntriesInRange(startDate: String, endDate: String): Flow<List<DailyEntryWithResponses>> {
        return flowOf(
            entries.filter { it.entryDate >= startDate && it.entryDate <= endDate }
                .map { entry -> DailyEntryWithResponses(entry, responses.filter { it.entryId == entry.id }) }
        )
    }

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

    override suspend fun clearAllInsights() {
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

    // Phase 2: Advice Tracking methods
    override suspend fun insertAdviceTracking(tracking: AdviceTrackingEntity): Long = 0L
    override suspend fun insertAdviceTrackings(trackings: List<AdviceTrackingEntity>) {}
    override suspend fun updateAdviceTracking(tracking: AdviceTrackingEntity) {}
    override suspend fun getTrackingsForEntry(entryId: Long): List<AdviceTrackingEntity> = emptyList()
    override suspend fun getTrackingsInDateRange(startDate: String, endDate: String): List<AdviceTrackingEntity> = emptyList()
    override suspend fun getTrackingsByFeedback(feedback: String, limit: Int): List<AdviceTrackingEntity> = emptyList()
    override suspend fun getExecutedWithScore(limit: Int): List<AdviceTrackingEntity> = emptyList()
    override suspend fun getTrackingsByCategory(category: String, limit: Int): List<AdviceTrackingEntity> = emptyList()
    override suspend fun deleteTrackingsForEntry(entryId: Long) {}
    override suspend fun clearAllTrackings() {}

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

private class FakeMedicationDao : com.heldairy.core.database.MedicationDao {
    override suspend fun insertMed(med: com.heldairy.core.database.entity.MedEntity): Long = 0L
    override suspend fun updateMed(med: com.heldairy.core.database.entity.MedEntity) {}
    override suspend fun deleteMed(med: com.heldairy.core.database.entity.MedEntity) {}
    override fun getAllMeds(): Flow<List<com.heldairy.core.database.entity.MedEntity>> = MutableStateFlow(emptyList())
    override suspend fun getMedById(medId: Long): com.heldairy.core.database.entity.MedEntity? = null
    override fun getAllMedsWithCourses(): Flow<List<com.heldairy.core.database.entity.MedWithCourses>> = MutableStateFlow(emptyList())
    override suspend fun getMedWithCoursesById(medId: Long): com.heldairy.core.database.entity.MedWithCourses? = null
    override suspend fun insertCourse(course: com.heldairy.core.database.entity.MedCourseEntity): Long = 0L
    override suspend fun updateCourse(course: com.heldairy.core.database.entity.MedCourseEntity) {}
    override suspend fun deleteCourse(course: com.heldairy.core.database.entity.MedCourseEntity) {}
    override fun getCoursesByMedId(medId: Long): Flow<List<com.heldairy.core.database.entity.MedCourseEntity>> = MutableStateFlow(emptyList())
    override suspend fun getCourseById(courseId: Long): com.heldairy.core.database.entity.MedCourseEntity? = null
    override fun getCoursesByStatus(status: String): Flow<List<com.heldairy.core.database.entity.MedCourseEntity>> = MutableStateFlow(emptyList())
    override fun getActiveMeds(): Flow<List<com.heldairy.core.database.entity.MedEntity>> = MutableStateFlow(emptyList())
    override suspend fun insertEvent(event: com.heldairy.core.database.entity.MedEventEntity): Long = 0L
    override fun getRecentEvents(limit: Int): Flow<List<com.heldairy.core.database.entity.MedEventEntity>> = MutableStateFlow(emptyList())
    override suspend fun getEventById(eventId: Long): com.heldairy.core.database.entity.MedEventEntity? = null
    override suspend fun insertReminder(reminder: com.heldairy.core.database.entity.MedicationReminderEntity): Long = 0L
    override suspend fun updateReminder(reminder: com.heldairy.core.database.entity.MedicationReminderEntity) {}
    override suspend fun deleteReminder(reminderId: Long) {}
    override fun getRemindersByMedId(medId: Long): Flow<List<com.heldairy.core.database.entity.MedicationReminderEntity>> = MutableStateFlow(emptyList())
    override suspend fun getReminderById(reminderId: Long): com.heldairy.core.database.entity.MedicationReminderEntity? = null
    override fun getAllEnabledReminders(): Flow<List<com.heldairy.core.database.entity.MedicationReminderEntity>> = MutableStateFlow(emptyList())
    override suspend fun updateReminderEnabled(reminderId: Long, enabled: Boolean, updatedAt: Long) {}
    override suspend fun clearAllReminders() {}
    override suspend fun clearAllEvents() {}
    override suspend fun clearAllCourses() {}
    override suspend fun clearAllMeds() {}
}

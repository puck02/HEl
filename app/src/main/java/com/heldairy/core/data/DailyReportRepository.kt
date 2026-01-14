package com.heldairy.core.data

import com.heldairy.core.database.DailyReportDao
import com.heldairy.core.database.entity.DailyAdviceEntity
import com.heldairy.core.database.entity.DailyEntryEntity
import com.heldairy.core.database.entity.DailyEntrySnapshot
import com.heldairy.core.database.entity.DailyEntryWithResponses
import com.heldairy.core.database.entity.DailySummaryEntity
import com.heldairy.core.database.entity.QuestionResponseEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DailyReportRepository(
    private val dailyReportDao: DailyReportDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    fun latestEntry(): Flow<DailyEntryWithResponses?> = dailyReportDao.observeLatestEntry()

    fun latestSnapshot(): Flow<DailyEntrySnapshot?> = dailyReportDao.observeLatestSnapshot()

    suspend fun recordDailyReport(
        entryDate: String,
        timezoneId: String,
        answers: List<DailyAnswerRecord>,
        createdAtMillis: Long
    ): Result<Long> = withContext(ioDispatcher) {
        if (answers.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("Daily report requires at least one answer."))
        }
        val entry = DailyEntryEntity(
            entryDate = entryDate,
            createdAt = createdAtMillis,
            timezoneId = timezoneId
        )
        val responseEntities = answers.map { record ->
            QuestionResponseEntity(
                entryId = 0L,
                questionId = record.questionId,
                stepIndex = record.stepIndex,
                questionOrder = record.order,
                answerType = record.answerType,
                answerValue = record.answerValue,
                answerLabel = record.answerLabel,
                metadataJson = record.metadataJson,
                answeredAt = createdAtMillis
            )
        }
        try {
            val entryId = dailyReportDao.replaceEntryWithResponses(entry, responseEntities)
            Result.success(entryId)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    suspend fun loadRecentEntries(limit: Int): List<DailyEntryWithResponses> = withContext(ioDispatcher) {
        dailyReportDao.loadRecentEntries(limit)
    }

    suspend fun getEntry(entryId: Long): DailyEntryWithResponses? = withContext(ioDispatcher) {
        dailyReportDao.getEntryWithResponses(entryId)
    }

    suspend fun loadAllSnapshots(): List<DailyEntrySnapshot> = withContext(ioDispatcher) {
        dailyReportDao.loadAllSnapshots()
    }

    suspend fun clearAll() = withContext(ioDispatcher) {
        dailyReportDao.clearAdvice()
        dailyReportDao.clearSummaries()
        dailyReportDao.clearResponses()
        dailyReportDao.clearEntries()
    }

    suspend fun saveSummary(summary: DailySummaryEntity) = withContext(ioDispatcher) {
        dailyReportDao.upsertSummary(summary)
    }

    suspend fun saveAdvice(advice: DailyAdviceEntity) = withContext(ioDispatcher) {
        dailyReportDao.upsertAdvice(advice)
    }
}

data class DailyAnswerRecord(
    val questionId: String,
    val stepIndex: Int,
    val order: Int,
    val answerType: String,
    val answerValue: String,
    val answerLabel: String,
    val metadataJson: String? = null
)

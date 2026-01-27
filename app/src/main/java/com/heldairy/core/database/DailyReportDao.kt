package com.heldairy.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.heldairy.core.database.entity.DailyAdviceEntity
import com.heldairy.core.database.entity.DailyEntryEntity
import com.heldairy.core.database.entity.DailyEntrySnapshot
import com.heldairy.core.database.entity.DailyEntryWithResponses
import com.heldairy.core.database.entity.DailySummaryEntity
import com.heldairy.core.database.entity.QuestionResponseEntity
import com.heldairy.core.database.entity.InsightReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyReportDao {
    @Insert
    suspend fun insertEntry(entry: DailyEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertResponses(responses: List<QuestionResponseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAdvice(advice: DailyAdviceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSummary(summary: DailySummaryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInsight(report: InsightReportEntity)

    @Query("SELECT id FROM daily_entries WHERE entry_date = :entryDate LIMIT 1")
    suspend fun findEntryIdByDate(entryDate: String): Long?

    @Query("DELETE FROM daily_entries WHERE id = :entryId")
    suspend fun deleteEntryById(entryId: Long)

    @Transaction
    suspend fun replaceEntryWithResponses(
        entry: DailyEntryEntity,
        responses: List<QuestionResponseEntity>
    ): Long {
        findEntryIdByDate(entry.entryDate)?.let { deleteEntryById(it) }
        return insertEntryWithResponses(entry, responses)
    }

    @Transaction
    suspend fun insertEntryWithResponses(
        entry: DailyEntryEntity,
        responses: List<QuestionResponseEntity>
    ): Long {
        val entryId = insertEntry(entry)
        val responsesWithEntry = responses.map { it.copy(entryId = entryId) }
        upsertResponses(responsesWithEntry)
        return entryId
    }

    @Transaction
    @Query("SELECT * FROM daily_entries ORDER BY created_at DESC LIMIT 1")
    fun observeLatestEntry(): Flow<DailyEntryWithResponses?>

    @Transaction
    @Query("SELECT * FROM daily_entries ORDER BY created_at DESC LIMIT 1")
    fun observeLatestSnapshot(): Flow<DailyEntrySnapshot?>

    @Transaction
    @Query("SELECT * FROM daily_entries ORDER BY created_at DESC LIMIT :limit")
    suspend fun loadRecentEntries(limit: Int): List<DailyEntryWithResponses>

    @Query("SELECT * FROM insight_reports WHERE week_start_date = :weekStart LIMIT 1")
    suspend fun findInsightByWeekStart(weekStart: String): InsightReportEntity?

    @Query("SELECT * FROM insight_reports ORDER BY week_end_date DESC LIMIT 1")
    suspend fun latestInsight(): InsightReportEntity?

    @Query("SELECT * FROM insight_reports ORDER BY week_end_date DESC")
    suspend fun loadAllInsights(): List<InsightReportEntity>

    @Transaction
    @Query("SELECT * FROM daily_entries WHERE id = :entryId LIMIT 1")
    suspend fun getEntryWithResponses(entryId: Long): DailyEntryWithResponses?

    @Transaction
    @Query("SELECT * FROM daily_entries ORDER BY created_at DESC")
    suspend fun loadAllSnapshots(): List<DailyEntrySnapshot>

    @Query("DELETE FROM daily_advice")
    suspend fun clearAdvice()

    @Query("DELETE FROM daily_summaries")
    suspend fun clearSummaries()

    @Query("DELETE FROM insight_reports")
    suspend fun clearInsights()

    @Query("DELETE FROM question_responses")
    suspend fun clearResponses()

    @Query("DELETE FROM daily_entries")
    suspend fun clearEntries()
}

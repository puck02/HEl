package com.heldairy.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.heldairy.core.database.entity.DailyEntryEntity
import com.heldairy.core.database.entity.DailyEntryWithResponses
import com.heldairy.core.database.entity.QuestionResponseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyReportDao {
    @Insert
    suspend fun insertEntry(entry: DailyEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertResponses(responses: List<QuestionResponseEntity>)

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
}

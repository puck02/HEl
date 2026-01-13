package com.heldairy.core.data

import com.heldairy.core.database.entity.DailySummaryEntity
import java.time.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DailySummaryManager(
    private val repository: DailyReportRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val json: Json = Json { encodeDefaults = true }
) {
    suspend fun regenerateForLatestEntry(entryId: Long): DailySummaryPayload? {
        val entry = repository.getEntry(entryId) ?: return null
        val recentEntries = repository.loadRecentEntries(limit = 30)
        val payload = DailySummaryCalculator.buildPayload(recentEntries)
        val summaryEntity = DailySummaryEntity(
            entryId = entry.entry.id,
            entryDate = entry.entry.entryDate,
            window7Json = payload.window7?.let { json.encodeToString(SummaryWindow.serializer(), it) },
            window30Json = payload.window30?.let { json.encodeToString(SummaryWindow.serializer(), it) },
            computedAt = clock.instant().toEpochMilli()
        )
        repository.saveSummary(summaryEntity)
        return payload
    }
}

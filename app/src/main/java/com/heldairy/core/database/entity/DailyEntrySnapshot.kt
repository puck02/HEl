package com.heldairy.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class DailyEntrySnapshot(
    @Embedded val entry: DailyEntryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "entry_id"
    )
    val responses: List<QuestionResponseEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "entry_id"
    )
    val advice: DailyAdviceEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "entry_id"
    )
    val summary: DailySummaryEntity?
)

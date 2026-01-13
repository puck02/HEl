package com.heldairy.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_summaries",
    foreignKeys = [
        ForeignKey(
            entity = DailyEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entry_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["entry_id"], unique = true)]
)
data class DailySummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "entry_id") val entryId: Long,
    @ColumnInfo(name = "entry_date") val entryDate: String,
    @ColumnInfo(name = "window_7_json") val window7Json: String?,
    @ColumnInfo(name = "window_30_json") val window30Json: String?,
    @ColumnInfo(name = "computed_at") val computedAt: Long
)

package com.heldairy.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_advice",
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
data class DailyAdviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "entry_id") val entryId: Long,
    @ColumnInfo(name = "entry_date") val entryDate: String,
    @ColumnInfo(name = "model") val model: String,
    @ColumnInfo(name = "advice_json") val adviceJson: String,
    @ColumnInfo(name = "prompt_hash") val promptHash: String,
    @ColumnInfo(name = "generated_at") val generatedAt: Long
)

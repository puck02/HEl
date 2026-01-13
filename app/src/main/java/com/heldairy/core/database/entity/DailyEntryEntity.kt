package com.heldairy.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_entries",
    indices = [Index(value = ["entry_date"], unique = true)]
)
data class DailyEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "entry_date") val entryDate: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "timezone_id") val timezoneId: String
)

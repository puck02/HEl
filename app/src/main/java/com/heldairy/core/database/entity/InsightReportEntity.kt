package com.heldairy.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "insight_reports",
    indices = [Index(value = ["week_start_date"], unique = true)]
)
data class InsightReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "week_start_date") val weekStartDate: String,
    @ColumnInfo(name = "week_end_date") val weekEndDate: String,
    @ColumnInfo(name = "generated_at") val generatedAt: Long,
    @ColumnInfo(name = "window7_json") val window7Json: String?,
    @ColumnInfo(name = "window30_json") val window30Json: String?,
    @ColumnInfo(name = "ai_result_json") val aiResultJson: String?,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "error_message") val errorMessage: String? = null
)

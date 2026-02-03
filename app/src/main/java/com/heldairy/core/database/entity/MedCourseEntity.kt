package com.heldairy.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "med_course",
    foreignKeys = [
        ForeignKey(
            entity = MedEntity::class,
            parentColumns = ["id"],
            childColumns = ["medId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("medId"),
        Index("status"),
        Index("startDate")  // 优化日期范围查询性能
    ]
)
data class MedCourseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medId: Long,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val status: String,
    val frequencyText: String,
    val doseText: String? = null,
    val timeHints: String? = null,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        const val STATUS_ACTIVE = "active"
        const val STATUS_PAUSED = "paused"
        const val STATUS_ENDED = "ended"
    }
}

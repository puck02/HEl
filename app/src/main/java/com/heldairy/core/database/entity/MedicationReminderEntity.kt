package com.heldairy.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medication_reminder",
    foreignKeys = [
        ForeignKey(
            entity = MedEntity::class,
            parentColumns = ["id"],
            childColumns = ["medId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("medId")]
)
data class MedicationReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val medId: Long,
    
    // 提醒时间（小时和分钟）
    val hour: Int, // 0-23
    val minute: Int, // 0-59
    
    // 重复模式
    val repeatType: String, // "DAILY", "WEEKLY", "DATE_RANGE"
    
    // 周几提醒（仅 WEEKLY 使用，逗号分隔：1,2,3,4,5,6,7 代表周一到周日）
    val weekDays: String? = null,
    
    // 日期范围（仅 DATE_RANGE 使用）
    val startDate: String? = null, // LocalDate.toString()
    val endDate: String? = null,
    
    // 是否启用
    val enabled: Boolean = true,
    
    // 提醒标题和内容
    val title: String? = null,
    val message: String? = null,
    
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

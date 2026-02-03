package com.heldairy.feature.medication

import java.time.LocalDate

data class Med(
    val id: Long = 0,
    val name: String,
    val aliases: String? = null,
    val note: String? = null,
    val infoSummary: String? = null,
    val imageUri: String? = null,
    val hasActiveCourse: Boolean = false,
    val currentCourse: MedCourse? = null,
    val createdAt: LocalDate = LocalDate.now(),
    val updatedAt: LocalDate = LocalDate.now()
)

data class MedCourse(
    val id: Long = 0,
    val medId: Long,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val status: CourseStatus,
    val frequencyText: String,
    val doseText: String? = null,
    val timeHints: String? = null
)

enum class CourseStatus {
    ACTIVE,
    PAUSED,
    ENDED;

    val displayName: String
        get() = when (this) {
            ACTIVE -> "正在服用"
            PAUSED -> "已暂停"
            ENDED -> "已停用"
        }

    fun toDbString(): String = name.lowercase()

    companion object {
        fun fromDbString(value: String): CourseStatus = valueOf(value.uppercase())
    }
}

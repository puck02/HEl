package com.heldairy.feature.medication

import java.time.LocalDate

data class MedicationReminder(
    val id: Long,
    val medId: Long,
    val hour: Int,
    val minute: Int,
    val repeatType: RepeatType,
    val weekDays: List<Int>? = null, // 1-7 (周一到周日)
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val enabled: Boolean,
    val title: String?,
    val message: String?,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun getRepeatDescription(): String {
        return when (repeatType) {
            RepeatType.DAILY -> "每天"
            RepeatType.WEEKLY -> {
                val days = weekDays?.sorted()?.joinToString("、") { 
                    when(it) {
                        1 -> "周一"
                        2 -> "周二"
                        3 -> "周三"
                        4 -> "周四"
                        5 -> "周五"
                        6 -> "周六"
                        7 -> "周日"
                        else -> ""
                    }
                }
                "每周$days"
            }
            RepeatType.DATE_RANGE -> {
                "$startDate 至 $endDate"
            }
        }
    }
    
    fun getTimeString(): String {
        return "%02d:%02d".format(hour, minute)
    }
}

enum class RepeatType {
    DAILY,      // 每天
    WEEKLY,     // 每周特定几天
    DATE_RANGE; // 日期范围内每天
    
    fun toDbString(): String = name
    
    companion object {
        fun fromDbString(value: String): RepeatType = valueOf(value)
    }
}

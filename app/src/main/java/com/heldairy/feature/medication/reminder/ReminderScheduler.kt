package com.heldairy.feature.medication.reminder

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.heldairy.feature.medication.MedicationReminder
import com.heldairy.feature.medication.RepeatType
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    /**
     * Schedule a reminder
     */
    fun scheduleReminder(context: Context, reminder: MedicationReminder) {
        if (!reminder.enabled) {
            android.util.Log.d("ReminderScheduler", "Reminder disabled, cancelling: ${reminder.id}")
            cancelReminder(context, reminder.id)
            return
        }

        val delay = calculateNextDelay(reminder)
        if (delay == null) {
            android.util.Log.w("ReminderScheduler", "No valid next time for reminder: ${reminder.id}")
            // Reminder has expired or no valid next time
            cancelReminder(context, reminder.id)
            return
        }

        val delayMinutes = delay.toMinutes()
        android.util.Log.d("ReminderScheduler", "Scheduling reminder ${reminder.id} in $delayMinutes minutes (${reminder.hour}:${reminder.minute})")

        val workRequest = OneTimeWorkRequestBuilder<MedicationReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putLong(MedicationReminderWorker.KEY_REMINDER_ID, reminder.id)
                    .build()
            )
            .addTag(getWorkTag(reminder.id))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            getWorkName(reminder.id),
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
        
        android.util.Log.d("ReminderScheduler", "Reminder scheduled successfully: ${reminder.id}")
    }

    /**
     * Schedule next occurrence after current reminder fires
     */
    fun scheduleNextReminder(context: Context, reminder: MedicationReminder) {
        when (reminder.repeatType) {
            RepeatType.DAILY -> {
                // Daily repeats, schedule for next day
                scheduleReminder(context, reminder)
            }
            RepeatType.WEEKLY -> {
                // Weekly repeats on specific days
                scheduleReminder(context, reminder)
            }
            RepeatType.DATE_RANGE -> {
                // Check if still within range
                val now = LocalDate.now()
                if (reminder.endDate?.isAfter(now) == true) {
                    scheduleReminder(context, reminder)
                } else {
                    cancelReminder(context, reminder.id)
                }
            }
        }
    }

    /**
     * Cancel a scheduled reminder
     */
    fun cancelReminder(context: Context, reminderId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(getWorkName(reminderId))
    }

    /**
     * Reschedule all enabled reminders (call on app startup)
     */
    suspend fun rescheduleAllReminders(context: Context, reminders: List<MedicationReminder>) {
        reminders.forEach { reminder ->
            if (reminder.enabled) {
                scheduleReminder(context, reminder)
            } else {
                cancelReminder(context, reminder.id)
            }
        }
    }

    /**
     * Calculate delay until next reminder time
     */
    private fun calculateNextDelay(reminder: MedicationReminder): Duration? {
        val now = LocalDateTime.now()
        val reminderTime = LocalTime.of(reminder.hour, reminder.minute)

        return when (reminder.repeatType) {
            RepeatType.DAILY -> {
                calculateDailyDelay(now, reminderTime)
            }
            RepeatType.WEEKLY -> {
                calculateWeeklyDelay(now, reminderTime, reminder.weekDays ?: emptyList())
            }
            RepeatType.DATE_RANGE -> {
                calculateDateRangeDelay(
                    now,
                    reminderTime,
                    reminder.startDate,
                    reminder.endDate
                )
            }
        }
    }

    private fun calculateDailyDelay(now: LocalDateTime, reminderTime: LocalTime): Duration {
        var nextReminder = now.toLocalDate().atTime(reminderTime)
        if (nextReminder.isBefore(now) || nextReminder.isEqual(now)) {
            // If time has passed today, schedule for tomorrow
            nextReminder = nextReminder.plusDays(1)
        }
        return Duration.between(now, nextReminder)
    }

    private fun calculateWeeklyDelay(
        now: LocalDateTime,
        reminderTime: LocalTime,
        weekDays: List<Int>
    ): Duration? {
        if (weekDays.isEmpty()) return null

        val currentDayOfWeek = now.dayOfWeek.value // 1=Monday, 7=Sunday
        var nextReminder: LocalDateTime? = null

        // Check today first
        if (weekDays.contains(currentDayOfWeek)) {
            val todayReminder = now.toLocalDate().atTime(reminderTime)
            if (todayReminder.isAfter(now)) {
                nextReminder = todayReminder
            }
        }

        // If not today or time passed, find next day in the week
        if (nextReminder == null) {
            for (daysAhead in 1..7) {
                val checkDate = now.toLocalDate().plusDays(daysAhead.toLong())
                val checkDayOfWeek = checkDate.dayOfWeek.value
                if (weekDays.contains(checkDayOfWeek)) {
                    nextReminder = checkDate.atTime(reminderTime)
                    break
                }
            }
        }

        return nextReminder?.let { Duration.between(now, it) }
    }

    private fun calculateDateRangeDelay(
        now: LocalDateTime,
        reminderTime: LocalTime,
        startDate: LocalDate?,
        endDate: LocalDate?
    ): Duration? {
        if (startDate == null || endDate == null) return null

        val today = now.toLocalDate()
        
        // Check if we're within the date range
        if (today.isBefore(startDate) || today.isAfter(endDate)) {
            return null
        }

        // If today is before start date, schedule for start date
        return if (today.isBefore(startDate)) {
            Duration.between(now, startDate.atTime(reminderTime))
        } else {
            // Within range, use daily calculation
            calculateDailyDelay(now, reminderTime)
        }
    }

    private fun getWorkName(reminderId: Long): String {
        return "medication_reminder_$reminderId"
    }

    private fun getWorkTag(reminderId: Long): String {
        return "reminder_$reminderId"
    }
}

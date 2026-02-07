package com.heldairy.feature.report.reminder

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Scheduler for daily report reminders
 * Schedules notifications at 20:00 (8 PM) every day
 */
object DailyReportReminderScheduler {
    
    private const val WORK_NAME = "daily_report_reminder"
    private const val WORK_TAG = "daily_report_reminder_tag"
    
    // Default reminder time: 20:00 (8 PM)
    private val DEFAULT_REMINDER_TIME = LocalTime.of(20, 0)

    /**
     * Schedule the next daily report reminder at 20:00
     */
    fun scheduleReminder(context: Context) {
        val delay = calculateDelayUntilNextReminder()
        
        android.util.Log.d(
            DailyReportReminderWorker.TAG,
            "Scheduling daily report reminder in ${delay.toMinutes()} minutes (${delay.toHours()} hours)"
        )

        val workRequest = OneTimeWorkRequestBuilder<DailyReportReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .addTag(WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
        
        android.util.Log.d(
            DailyReportReminderWorker.TAG,
            "Daily report reminder scheduled successfully"
        )
    }

    /**
     * Cancel the scheduled daily report reminder
     */
    fun cancelReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        android.util.Log.d(
            DailyReportReminderWorker.TAG,
            "Daily report reminder cancelled"
        )
    }

    /**
     * Calculate the delay until the next 20:00 reminder
     */
    private fun calculateDelayUntilNextReminder(): Duration {
        val now = LocalDateTime.now()
        val todayReminder = now.toLocalDate().atTime(DEFAULT_REMINDER_TIME)
        
        val nextReminder = if (now.isBefore(todayReminder)) {
            // Today's 20:00 hasn't passed yet
            todayReminder
        } else {
            // Today's 20:00 has passed, schedule for tomorrow
            todayReminder.plusDays(1)
        }
        
        return Duration.between(now, nextReminder)
    }

    /**
     * Check if reminder is currently scheduled
     */
    suspend fun isReminderScheduled(context: Context): Boolean {
        return try {
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(WORK_NAME)
                .get()
            workInfos.any { !it.state.isFinished }
        } catch (e: Exception) {
            android.util.Log.e(DailyReportReminderWorker.TAG, "Error checking reminder status", e)
            false
        }
    }
}

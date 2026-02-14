package com.heldairy.feature.report.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Scheduler for daily report reminders
 * Schedules notifications at 20:00 (8 PM) every day using PeriodicWorkRequest
 */
object DailyReportReminderScheduler {
    
    private const val WORK_NAME = "daily_report_reminder"
    private const val WORK_TAG = "daily_report_reminder_tag"
    private const val ALARM_REQUEST_CODE = 8890
    
    // Default reminder time: 20:00 (8 PM)
    private val DEFAULT_REMINDER_TIME = LocalTime.of(20, 0)

    /**
     * Schedule daily report reminder to run every day at 20:00
     */
    fun scheduleReminder(context: Context) {
        scheduleWithAlarmManager(context)
        scheduleWithWorkManager(context)
    }

    private fun scheduleWithWorkManager(context: Context) {
        val delay = calculateDelayUntilNextReminder()
        
        android.util.Log.d(
            DailyReportReminderWorker.TAG,
            "Scheduling daily report reminder in ${delay.toMinutes()} minutes (${delay.toHours()} hours)"
        )

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false) // 允许低电量时执行，因为这是用户期望的功能
            .build()

        val workRequest = PeriodicWorkRequestBuilder<DailyReportReminderWorker>(
            repeatInterval = 1, // 每1天重复
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .addTag(WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE, // 更新现有任务
            workRequest
        )
        
        android.util.Log.d(
            DailyReportReminderWorker.TAG,
            "Daily report reminder scheduled successfully as periodic work"
        )
    }

    private fun scheduleWithAlarmManager(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAtMillis = calculateNextReminderEpochMillis()
        val pendingIntent = createAlarmPendingIntent(context)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }

        android.util.Log.d(
            DailyReportReminderWorker.TAG,
            "Daily report reminder scheduled successfully as alarm"
        )
    }

    /**
     * Cancel the scheduled daily report reminder
     */
    fun cancelReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(createAlarmPendingIntent(context))
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

    private fun calculateNextReminderEpochMillis(): Long {
        val now = LocalDateTime.now()
        val todayReminder = now.toLocalDate().atTime(DEFAULT_REMINDER_TIME)
        val nextReminder = if (now.isBefore(todayReminder)) todayReminder else todayReminder.plusDays(1)
        val zone = java.time.ZoneId.systemDefault()
        return nextReminder.atZone(zone).toInstant().toEpochMilli()
    }

    private fun createAlarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyReportReminderReceiver::class.java).apply {
            action = DailyReportReminderReceiver.ACTION_DAILY_REPORT_ALARM
        }
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Check if reminder is currently scheduled
     */
    suspend fun isReminderScheduled(context: Context): Boolean {
        return try {
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(WORK_NAME)
                .get()
            val workScheduled = workInfos.any { !it.state.isFinished }
            val alarmIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                Intent(context, DailyReportReminderReceiver::class.java).apply {
                    action = DailyReportReminderReceiver.ACTION_DAILY_REPORT_ALARM
                },
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            workScheduled || alarmIntent != null
        } catch (e: Exception) {
            android.util.Log.e(DailyReportReminderWorker.TAG, "Error checking reminder status", e)
            false
        }
    }
}

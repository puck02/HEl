package com.heldairy.feature.report.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker for sending daily report reminders with Hello Kitty style messages
 */
class DailyReportReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        android.util.Log.d(TAG, "Daily report reminder worker started")
        
        try {
            DailyReportReminderNotificationHelper.showRandomReminderNotification(applicationContext)
            
            // PeriodicWorkRequest 会自动调度下次执行，无需手动调度
            android.util.Log.d(TAG, "Daily report reminder sent successfully")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to send daily report reminder", e)
            Result.failure()
        }
    }

    companion object {
        const val TAG = "DailyReportReminder"
        const val CHANNEL_ID = "daily_report_reminder"
        const val NOTIFICATION_ID = 8888
        const val REQUEST_CODE = 8889
        const val EXTRA_OPEN_REPORT = "open_report"
    }
}

/**
 * Data class for Hello Kitty style message
 */
data class HelloKittyMessage(
    val title: String,
    val body: String
)

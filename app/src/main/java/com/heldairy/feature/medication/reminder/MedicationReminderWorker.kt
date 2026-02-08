package com.heldairy.feature.medication.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.heldairy.HElDairyApplication
import com.heldairy.MainActivity
import com.heldairy.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MedicationReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val reminderId = inputData.getLong(KEY_REMINDER_ID, -1L)
        android.util.Log.d("MedicationReminder", "Worker started for reminder: $reminderId")
        
        if (reminderId == -1L) {
            android.util.Log.e("MedicationReminder", "Invalid reminder ID")
            return@withContext Result.failure()
        }

        try {
            val app = applicationContext as HElDairyApplication
            val repository = app.appContainer.medicationRepository
            
            val reminder = repository.getReminderById(reminderId)
            if (reminder == null) {
                android.util.Log.e("MedicationReminder", "Reminder not found: $reminderId")
                return@withContext Result.success()
            }
            
            if (!reminder.enabled) {
                android.util.Log.d("MedicationReminder", "Reminder disabled: $reminderId")
                return@withContext Result.success()
            }

            val med = repository.getMedById(reminder.medId)
            if (med == null) {
                android.util.Log.e("MedicationReminder", "Medication not found: ${reminder.medId}")
                return@withContext Result.success()
            }

            android.util.Log.d("MedicationReminder", "Showing notification for: ${med.name}")
            
            // Show notification
            showNotification(
                reminderId = reminder.id,
                medName = med.name,
                title = reminder.title ?: "💊 用药提醒",
                message = reminder.message ?: "该吃${med.name}了，请按时服用"
            )

            // Reschedule if needed
            ReminderScheduler.scheduleNextReminder(applicationContext, reminder)
            android.util.Log.d("MedicationReminder", "Reminder rescheduled successfully")

            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("MedicationReminder", "Worker failed", e)
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun showNotification(
        reminderId: Long,
        medName: String,
        title: String,
        message: String
    ) {
        try {
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Create notification channel (Android 8.0+) with MAX importance
            val channel = NotificationChannel(
                CHANNEL_ID,
                "用药提醒",
                NotificationManager.IMPORTANCE_HIGH  // 使用HIGH确保显示弹窗
            ).apply {
                description = "提醒您按时用药"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)  // 震动模式
                setShowBadge(true)
                enableLights(true)  // 启用指示灯
                setSound(
                    android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
            }
            notificationManager.createNotificationChannel(channel)

            // Create intent to open app
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("reminder_id", reminderId)
                putExtra("med_name", medName)
            }
            val pendingIntent = PendingIntent.getActivity(
                applicationContext,
                reminderId.toInt(),
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // Build notification with full-screen intent for heads-up display
            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.strawberry)  // Hello Kitty草莓图标
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)  // 高优先级
                .setCategory(NotificationCompat.CATEGORY_ALARM)  // 使用ALARM类别确保弹窗
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)  // 启用所有默认效果
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)  // 锁屏可见
                .setOngoing(false)  // 可以滑动清除
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .build()

            android.util.Log.d("MedicationReminder", "Showing notification: id=$reminderId, title=$title, message=$message")
            notificationManager.notify(reminderId.toInt(), notification)
            android.util.Log.d("MedicationReminder", "Notification posted successfully")
        } catch (e: Exception) {
            android.util.Log.e("MedicationReminder", "Failed to show notification", e)
            e.printStackTrace()
        }
    }

    companion object {
        const val KEY_REMINDER_ID = "reminder_id"
        private const val CHANNEL_ID = "medication_reminder_channel"
    }
}

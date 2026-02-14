package com.heldairy.feature.medication.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.heldairy.HElDairyApplication
import com.heldairy.MainActivity
import com.heldairy.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 用药提醒广播接收器
 * 
 * 由 AlarmManager 精确闹钟触发。
 * 使用 WakeLock + goAsync() 确保在应用关闭状态下也能完整处理。
 */
class MedicationReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_MEDICATION_ALARM) return

        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        Log.i(TAG, "💊 用药提醒闹钟触发: reminderId=$reminderId")

        // 获取 WakeLock
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "HElDairy:MedicationReminder:$reminderId"
        )
        wakeLock.acquire(30_000L) // 最多持有 30 秒（需要数据库查询）

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? HElDairyApplication ?: return@launch
                val repository = app.appContainer.medicationRepository

                val reminder = repository.getReminderById(reminderId)
                if (reminder == null || !reminder.enabled) {
                    Log.d(TAG, "提醒 $reminderId 不存在或已禁用")
                    return@launch
                }

                val med = repository.getMedById(reminder.medId)
                if (med == null) {
                    Log.d(TAG, "药物不存在 for reminder $reminderId")
                    return@launch
                }

                // 发送通知
                showNotification(
                    context = context,
                    reminderId = reminder.id,
                    medName = med.name,
                    title = reminder.title ?: "💊 用药提醒",
                    message = reminder.message ?: "该吃${med.name}了，请按时服用"
                )

                // 重新调度下次提醒
                ReminderScheduler.scheduleReminder(context, reminder)
                Log.i(TAG, "✅ 用药提醒处理完成: ${med.name}")

            } catch (e: Exception) {
                Log.e(TAG, "❌ 用药提醒处理失败", e)
            } finally {
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(
        context: Context,
        reminderId: Long,
        medName: String,
        title: String,
        message: String
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "用药提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "提醒您按时用药"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 200, 500)
            setShowBadge(true)
            enableLights(true)
            setSound(
                Settings.System.DEFAULT_NOTIFICATION_URI,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build()
            )
        }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("reminder_id", reminderId)
            putExtra("med_name", medName)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.strawberry)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .build()

        notificationManager.notify(reminderId.toInt(), notification)
    }

    companion object {
        const val TAG = "MedicationReminder"
        const val ACTION_MEDICATION_ALARM = "com.heldairy.action.MEDICATION_REMINDER"
        const val EXTRA_REMINDER_ID = "reminder_id"
        private const val CHANNEL_ID = "medication_reminder_channel"
    }
}

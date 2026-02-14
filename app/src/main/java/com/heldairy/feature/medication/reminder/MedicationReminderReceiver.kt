package com.heldairy.feature.medication.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.heldairy.HElDairyApplication
import com.heldairy.MainActivity
import com.heldairy.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver for medication reminders triggered by AlarmManager.
 * Shows notification and re-schedules next alarm.
 */
class MedicationReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_MEDICATION_ALARM) return

        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        android.util.Log.d(TAG, "Medication alarm received for reminder: $reminderId")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? HElDairyApplication ?: return@launch
                val repository = app.appContainer.medicationRepository

                val reminder = repository.getReminderById(reminderId)
                if (reminder == null || !reminder.enabled) {
                    android.util.Log.d(TAG, "Reminder $reminderId not found or disabled")
                    return@launch
                }

                val med = repository.getMedById(reminder.medId)
                if (med == null) {
                    android.util.Log.d(TAG, "Medication not found for reminder $reminderId")
                    return@launch
                }

                // Show notification
                showNotification(
                    context = context,
                    reminderId = reminder.id,
                    medName = med.name,
                    title = reminder.title ?: "💊 用药提醒",
                    message = reminder.message ?: "该吃${med.name}了，请按时服用"
                )

                // Re-schedule next occurrence via AlarmManager
                ReminderScheduler.scheduleReminder(context, reminder)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Error handling medication alarm", e)
            } finally {
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

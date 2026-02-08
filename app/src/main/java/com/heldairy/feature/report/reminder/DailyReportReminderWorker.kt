package com.heldairy.feature.report.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.heldairy.MainActivity
import com.heldairy.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

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
            // Pick a random Hello Kitty style message
            val message = HELLO_KITTY_MESSAGES.random()
            
            // Show notification
            showNotification(message)
            
            // PeriodicWorkRequest 会自动调度下次执行，无需手动调度
            android.util.Log.d(TAG, "Daily report reminder sent successfully")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to send daily report reminder", e)
            Result.failure()
        }
    }

    private fun showNotification(message: HelloKittyMessage) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel with sound
        val channel = NotificationChannel(
            CHANNEL_ID,
            "日报提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "每日20:00提醒填写健康日报"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 150, 300, 150, 300)
            setShowBadge(true)
            enableLights(true)
            lightColor = 0xFFFFB7C5.toInt() // Hello Kitty pink
            setSound(
                Settings.System.DEFAULT_NOTIFICATION_URI,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }
        notificationManager.createNotificationChannel(channel)

        // Create intent to open app
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_REPORT, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification with Hello Kitty style
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.strawberry)  // Hello Kitty草莓图标
            .setContentTitle(message.title)
            .setContentText(message.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setVibrate(longArrayOf(0, 300, 150, 300, 150, 300))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val TAG = "DailyReportReminder"
        const val CHANNEL_ID = "daily_report_reminder"
        const val NOTIFICATION_ID = 8888
        const val REQUEST_CODE = 8889
        const val EXTRA_OPEN_REPORT = "open_report"
        
        /**
         * 10 Hello Kitty style reminder messages in Chinese
         */
        val HELLO_KITTY_MESSAGES = listOf(
            HelloKittyMessage(
                title = "🎀 Kitty小管家来啦~",
                body = "亲爱的主人，今天过得怎么样呀？快来填写日报，让Kitty记录下你美好的一天吧！💕"
            ),
            HelloKittyMessage(
                title = "🌸 晚上好，主人~",
                body = "Kitty在等你哦！来聊聊今天的身体状况吧，好好照顾自己才是最重要的呢~ ✨"
            ),
            HelloKittyMessage(
                title = "💖 嘿嘿，是日报时间啦！",
                body = "主人主人，Kitty想知道你今天元气满满吗？快来告诉我吧，我会帮你好好记住的喵~"
            ),
            HelloKittyMessage(
                title = "🎀 叮咚~ Kitty来敲门啦",
                body = "辛苦了一天的主人，现在是属于我们的温馨时光哦！来填写日报，让Kitty陪你回顾这一天吧~ 💫"
            ),
            HelloKittyMessage(
                title = "🌙 晚安前的小任务~",
                body = "主人，睡前别忘了填日报哦！Kitty会把你的健康点滴都温柔地守护起来的~ 🌟"
            ),
            HelloKittyMessage(
                title = "💕 最爱的主人在吗？",
                body = "Kitty等你好久啦！今天有没有好好吃饭、好好休息呀？快来告诉Kitty吧~ 🍓"
            ),
            HelloKittyMessage(
                title = "✨ 日报小闹钟响啦~",
                body = "亲爱的主人，Kitty的小铃铛在提醒你啦！记录今天的健康状况，明天会更棒哦！🎀"
            ),
            HelloKittyMessage(
                title = "🎀 Kitty想你啦~",
                body = "主人今天累不累呀？快来和Kitty聊聊天，填写日报让我更了解你的状态吧！💗"
            ),
            HelloKittyMessage(
                title = "🌸 温柔提醒时间~",
                body = "Hi~是Kitty哦！今天的身体感觉如何呢？来记录一下吧，健康的你才是最可爱的！🌈"
            ),
            HelloKittyMessage(
                title = "💫 主人，日报时间到！",
                body = "Kitty带着小星星来找你啦！一起来填写今日日报，让每一天都闪闪发光吧~ ⭐"
            )
        )
    }
}

/**
 * Data class for Hello Kitty style message
 */
data class HelloKittyMessage(
    val title: String,
    val body: String
)

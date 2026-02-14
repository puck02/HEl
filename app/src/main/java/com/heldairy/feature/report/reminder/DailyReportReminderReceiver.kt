package com.heldairy.feature.report.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log

/**
 * 日报提醒广播接收器
 * 
 * 由 AlarmManager 精确闹钟触发，即使应用已关闭也能接收。
 * 使用 WakeLock 确保在 CPU 休眠状态下也能完成通知发送。
 */
class DailyReportReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_DAILY_REPORT_ALARM) return

        Log.i(TAG, "📱 日报提醒闹钟触发")

        // 获取 WakeLock 保证 CPU 唤醒完成通知
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "HElDairy:DailyReportReminder"
        )
        wakeLock.acquire(10_000L) // 最多持有 10 秒

        try {
            // 1. 发送通知
            DailyReportReminderNotificationHelper.showRandomReminderNotification(context)
            Log.i(TAG, "✅ 日报提醒通知已发送")

            // 2. 调度明天的闹钟（AlarmManager 单次闹钟需要每次重新调度）
            DailyReportReminderScheduler.scheduleReminder(context)
            Log.i(TAG, "✅ 明日日报提醒已调度")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 日报提醒处理失败", e)
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    companion object {
        private const val TAG = "DailyReportReminder"
        const val ACTION_DAILY_REPORT_ALARM = "com.heldairy.action.DAILY_REPORT_REMINDER"
    }
}

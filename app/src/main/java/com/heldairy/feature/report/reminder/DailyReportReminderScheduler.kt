package com.heldairy.feature.report.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * 系统级日报提醒调度器
 * 
 * 使用 AlarmManager 精确闹钟实现，即使应用被杀也能准时触发。
 * AlarmManager.setAlarmClock() 是最高优先级闹钟，不受 Doze 模式影响。
 */
object DailyReportReminderScheduler {

    private const val TAG = "DailyReportReminder"
    private const val ALARM_REQUEST_CODE = 8890

    // 默认提醒时间 20:00
    private val DEFAULT_REMINDER_TIME = LocalTime.of(20, 0)

    /**
     * 调度下一次日报提醒
     * 
     * 使用 setAlarmClock() —— 系统级闹钟，最高优先级：
     * - 不受 Doze 模式限制
     * - 不受电池优化限制
     * - 应用被杀后仍然准时触发
     * - 在状态栏显示闹钟图标（用户可见）
     */
    fun scheduleReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAtMillis = calculateNextReminderEpochMillis()
        val pendingIntent = createAlarmPendingIntent(context)

        // 使用 setAlarmClock —— 这是 Android 最高优先级闹钟
        // 它会：1) 在状态栏显示闹钟图标  2) 无视 Doze  3) 无视电池优化
        val showIntent = createShowPendingIntent(context)
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent)

        try {
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.i(TAG, "✅ 日报提醒已调度 (AlarmClock): ${java.time.Instant.ofEpochMilli(triggerAtMillis)}")
        } catch (e: SecurityException) {
            Log.w(TAG, "AlarmClock 调度失败，降级到 setExactAndAllowWhileIdle", e)
            // 降级方案
            scheduleWithExactAlarm(alarmManager, triggerAtMillis, pendingIntent)
        }
    }

    /**
     * 降级方案：使用 setExactAndAllowWhileIdle
     */
    private fun scheduleWithExactAlarm(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                    Log.i(TAG, "✅ 日报提醒已调度 (ExactAlarm)")
                } else {
                    // 无精确闹钟权限，使用非精确闹钟
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                    Log.w(TAG, "⚠️ 无精确闹钟权限，使用非精确闹钟")
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                Log.i(TAG, "✅ 日报提醒已调度 (ExactAlarm)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "精确闹钟调度失败", e)
        }
    }

    /**
     * 取消日报提醒
     */
    fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createAlarmPendingIntent(context)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.i(TAG, "日报提醒已取消")
    }

    /**
     * 检查提醒是否已调度
     */
    fun isReminderScheduled(context: Context): Boolean {
        val intent = Intent(context, DailyReportReminderReceiver::class.java).apply {
            action = DailyReportReminderReceiver.ACTION_DAILY_REPORT_ALARM
        }
        val existing = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        return existing != null
    }

    /**
     * 计算下一次 20:00 的时间戳
     */
    private fun calculateNextReminderEpochMillis(): Long {
        val now = LocalDateTime.now()
        var nextReminder = now.toLocalDate().atTime(DEFAULT_REMINDER_TIME)

        if (now >= nextReminder) {
            // 今天的20:00已过，调度到明天
            nextReminder = nextReminder.plusDays(1)
        }

        return nextReminder.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /**
     * 创建触发通知的 PendingIntent
     */
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
     * 创建点击闹钟图标时打开应用的 PendingIntent
     */
    private fun createShowPendingIntent(context: Context): PendingIntent {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent()
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

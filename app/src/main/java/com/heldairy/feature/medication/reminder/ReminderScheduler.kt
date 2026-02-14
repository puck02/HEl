package com.heldairy.feature.medication.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.heldairy.feature.medication.MedicationReminder
import com.heldairy.feature.medication.RepeatType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * 用药提醒调度器
 * 
 * 使用 AlarmManager.setAlarmClock() 实现系统级精确提醒。
 * 即使应用被杀、设备 Doze 模式下也能准时触发。
 */
object ReminderScheduler {

    private const val TAG = "ReminderScheduler"
    private const val ALARM_REQUEST_CODE_BASE = 20000

    /**
     * 调度一个提醒
     */
    fun scheduleReminder(context: Context, reminder: MedicationReminder) {
        if (!reminder.enabled) {
            Log.d(TAG, "提醒已禁用，取消调度: ${reminder.id}")
            cancelReminder(context, reminder.id)
            return
        }

        val nextTrigger = calculateNextTrigger(reminder) ?: run {
            Log.d(TAG, "无下次触发时间: ${reminder.id}")
            return
        }

        val triggerMillis = nextTrigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createPendingIntent(context, reminder.id)

        // 使用 AlarmClock —— 最高优先级，不受 Doze 限制
        try {
            val showIntent = createShowPendingIntent(context)
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.i(TAG, "✅ 用药提醒已调度 (AlarmClock): id=${reminder.id}, trigger=$nextTrigger")
        } catch (e: SecurityException) {
            Log.w(TAG, "AlarmClock 失败，降级到 setExactAndAllowWhileIdle", e)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } catch (e2: Exception) {
                Log.e(TAG, "所有闹钟调度方式均失败", e2)
            }
        }
    }

    /**
     * 调度下一次提醒（在 Receiver 触发后调用）
     */
    fun scheduleNextReminder(context: Context, reminder: MedicationReminder) {
        scheduleReminder(context, reminder)
    }

    /**
     * 取消一个提醒
     */
    fun cancelReminder(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createPendingIntent(context, reminderId)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.i(TAG, "用药提醒已取消: $reminderId")
    }

    /**
     * 重新调度所有已启用的提醒（开机后/应用更新后调用）
     */
    fun rescheduleAllReminders(context: Context, reminders: List<MedicationReminder>) {
        Log.i(TAG, "重新调度 ${reminders.size} 个提醒")
        reminders.forEach { reminder ->
            if (reminder.enabled) {
                scheduleReminder(context, reminder)
            }
        }
    }

    /**
     * 计算下次触发时间
     */
    private fun calculateNextTrigger(reminder: MedicationReminder): LocalDateTime? {
        val now = LocalDateTime.now()
        val reminderTime = LocalTime.of(reminder.hour, reminder.minute)

        return when (reminder.repeatType) {
            RepeatType.DAILY -> {
                val todayTrigger = now.toLocalDate().atTime(reminderTime)
                if (now < todayTrigger) todayTrigger else todayTrigger.plusDays(1)
            }

            RepeatType.WEEKLY -> {
                val weekDays = reminder.weekDays
                if (weekDays.isNullOrEmpty()) return null

                // 找到最近的下一个触发日
                for (daysAhead in 0..7) {
                    val candidate = now.toLocalDate().plusDays(daysAhead.toLong())
                    val dayOfWeek = candidate.dayOfWeek.value // 1=Monday ... 7=Sunday
                    if (dayOfWeek in weekDays) {
                        val candidateTime = candidate.atTime(reminderTime)
                        if (candidateTime > now) return candidateTime
                    }
                }
                null
            }

            RepeatType.DATE_RANGE -> {
                val startDate = reminder.startDate ?: return null
                val endDate = reminder.endDate
                val today = now.toLocalDate()

                if (today < startDate) {
                    return startDate.atTime(reminderTime)
                }
                if (endDate != null && today > endDate) {
                    return null // 已过期
                }

                val todayTrigger = today.atTime(reminderTime)
                if (now < todayTrigger) {
                    todayTrigger
                } else {
                    val tomorrow = today.plusDays(1)
                    if (endDate != null && tomorrow > endDate) null
                    else tomorrow.atTime(reminderTime)
                }
            }
        }
    }

    private fun createPendingIntent(context: Context, reminderId: Long): PendingIntent {
        val intent = Intent(context, MedicationReminderReceiver::class.java).apply {
            action = MedicationReminderReceiver.ACTION_MEDICATION_ALARM
            putExtra(MedicationReminderReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            (ALARM_REQUEST_CODE_BASE + reminderId).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

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

package com.heldairy.feature.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.heldairy.HElDairyApplication
import com.heldairy.feature.medication.reminder.ReminderScheduler
import com.heldairy.feature.report.reminder.DailyReportReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 开机/应用更新后自动恢复所有提醒调度。
 * 
 * 监听：
 * - BOOT_COMPLETED: 设备重启
 * - LOCKED_BOOT_COMPLETED: 直接解锁启动
 * - MY_PACKAGE_REPLACED: 应用更新后
 */
class ReminderBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        Log.i(TAG, "系统事件: $action → 重新调度所有提醒")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? HElDairyApplication
                val container = app?.appContainer

                if (container != null) {
                    // 重新调度用药提醒
                    val reminders = container.medicationRepository.getAllEnabledReminders().first()
                    ReminderScheduler.rescheduleAllReminders(context, reminders)
                    Log.i(TAG, "✅ 已恢复 ${reminders.size} 个用药提醒")

                    // 重新调度日报提醒
                    val reminderEnabled = container.dailyReportPreferencesStore.reminderEnabledFlow.first()
                    if (reminderEnabled) {
                        DailyReportReminderScheduler.scheduleReminder(context)
                        Log.i(TAG, "✅ 已恢复日报提醒")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "恢复提醒失败", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "ReminderBootReceiver"
    }
}

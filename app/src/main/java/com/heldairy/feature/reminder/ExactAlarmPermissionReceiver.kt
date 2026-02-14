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
 * 监听精确闹钟权限变更。
 * 
 * Android 12+ 用户可以在系统设置中开关精确闹钟权限。
 * 当权限恢复时，重新调度所有提醒。
 */
class ExactAlarmPermissionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED") return

        Log.i(TAG, "精确闹钟权限状态变更，重新调度所有提醒")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? HElDairyApplication ?: return@launch
                val container = app.appContainer

                // 重新调度用药提醒
                val reminders = container.medicationRepository.getAllEnabledReminders().first()
                ReminderScheduler.rescheduleAllReminders(context, reminders)

                // 重新调度日报提醒
                val reminderEnabled = container.dailyReportPreferencesStore.reminderEnabledFlow.first()
                if (reminderEnabled) {
                    DailyReportReminderScheduler.scheduleReminder(context)
                }
            } catch (e: Exception) {
                Log.e(TAG, "重新调度失败", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "ExactAlarmPermission"
    }
}

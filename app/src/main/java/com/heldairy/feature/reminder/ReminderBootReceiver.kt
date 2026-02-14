package com.heldairy.feature.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.heldairy.HElDairyApplication
import com.heldairy.feature.medication.reminder.ReminderScheduler
import com.heldairy.feature.report.reminder.DailyReportReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? HElDairyApplication
                val container = app?.appContainer

                if (container != null) {
                    val reminders = container.medicationRepository.getAllEnabledReminders().first()
                    ReminderScheduler.rescheduleAllReminders(context, reminders)

                    val reminderEnabled = container.dailyReportPreferencesStore.reminderEnabledFlow.first()
                    if (reminderEnabled) {
                        DailyReportReminderScheduler.scheduleReminder(context)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

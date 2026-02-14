package com.heldairy.feature.report.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DailyReportReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_DAILY_REPORT_ALARM) return

        android.util.Log.d(DailyReportReminderWorker.TAG, "Daily report alarm received")
        DailyReportReminderNotificationHelper.showRandomReminderNotification(context)
        DailyReportReminderScheduler.scheduleReminder(context)
    }

    companion object {
        const val ACTION_DAILY_REPORT_ALARM = "com.heldairy.action.DAILY_REPORT_REMINDER"
    }
}

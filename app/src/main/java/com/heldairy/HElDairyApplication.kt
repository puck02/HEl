package com.heldairy

import android.app.Application
import com.heldairy.core.di.AppContainer
import com.heldairy.core.di.AppContainerImpl
import com.heldairy.feature.medication.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HElDairyApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainerImpl(this) }
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        
        // Reschedule all enabled reminders on app startup
        applicationScope.launch {
            try {
                val reminders = appContainer.medicationRepository.getAllEnabledReminders().first()
                ReminderScheduler.rescheduleAllReminders(applicationContext, reminders)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

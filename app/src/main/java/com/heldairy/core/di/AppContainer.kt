package com.heldairy.core.di

import android.content.Context
import com.heldairy.core.data.DailyReportRepository
import com.heldairy.core.database.DailyReportDatabase

interface AppContainer {
    val dailyReportRepository: DailyReportRepository
}

class AppContainerImpl(context: Context) : AppContainer {
    private val database = DailyReportDatabase.build(context)

    override val dailyReportRepository: DailyReportRepository =
        DailyReportRepository(database.dailyReportDao())
}

package com.heldairy.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.heldairy.core.database.entity.DailyAdviceEntity
import com.heldairy.core.database.entity.DailyEntryEntity
import com.heldairy.core.database.entity.DailySummaryEntity
import com.heldairy.core.database.entity.QuestionResponseEntity

@Database(
    entities = [
        DailyEntryEntity::class,
        QuestionResponseEntity::class,
        DailyAdviceEntity::class,
        DailySummaryEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class DailyReportDatabase : RoomDatabase() {
    abstract fun dailyReportDao(): DailyReportDao

    companion object {
        fun build(context: Context): DailyReportDatabase = Room.databaseBuilder(
            context.applicationContext,
            DailyReportDatabase::class.java,
            "heldairy-daily-report.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
}

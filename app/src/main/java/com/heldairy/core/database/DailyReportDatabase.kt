package com.heldairy.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.heldairy.core.database.entity.DailyAdviceEntity
import com.heldairy.core.database.entity.DailyEntryEntity
import com.heldairy.core.database.entity.DailySummaryEntity
import com.heldairy.core.database.entity.QuestionResponseEntity
import com.heldairy.core.database.entity.InsightReportEntity
import com.heldairy.core.database.entity.MedEntity
import com.heldairy.core.database.entity.MedCourseEntity
import com.heldairy.core.database.entity.MedEventEntity

@Database(
    entities = [
        DailyEntryEntity::class,
        QuestionResponseEntity::class,
        DailyAdviceEntity::class,
        DailySummaryEntity::class,
        InsightReportEntity::class,
        MedEntity::class,
        MedCourseEntity::class,
        MedEventEntity::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class DailyReportDatabase : RoomDatabase() {
    abstract fun dailyReportDao(): DailyReportDao
    abstract fun medicationDao(): MedicationDao

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

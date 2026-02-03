package com.heldairy.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.heldairy.core.database.entity.AdviceTrackingEntity
import com.heldairy.core.database.entity.DailyAdviceEntity
import com.heldairy.core.database.entity.DailyEntryEntity
import com.heldairy.core.database.entity.DailySummaryEntity
import com.heldairy.core.database.entity.QuestionResponseEntity
import com.heldairy.core.database.entity.InsightReportEntity
import com.heldairy.core.database.entity.MedEntity
import com.heldairy.core.database.entity.MedCourseEntity
import com.heldairy.core.database.entity.MedEventEntity
import com.heldairy.core.database.entity.MedicationReminderEntity

@Database(
    entities = [
        DailyEntryEntity::class,
        QuestionResponseEntity::class,
        DailyAdviceEntity::class,
        DailySummaryEntity::class,
        InsightReportEntity::class,
        MedEntity::class,
        MedCourseEntity::class,
        MedEventEntity::class,
        AdviceTrackingEntity::class,
        MedicationReminderEntity::class  // 用药提醒表
    ],
    version = 9,  // 版本升级：添加 med_course.startDate 索引
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class DailyReportDatabase : RoomDatabase() {
    abstract fun dailyReportDao(): DailyReportDao
    abstract fun medicationDao(): MedicationDao

    companion object {
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create medication_reminder table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS medication_reminder (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        medId INTEGER NOT NULL,
                        hour INTEGER NOT NULL,
                        minute INTEGER NOT NULL,
                        repeatType TEXT NOT NULL,
                        weekDays TEXT,
                        startDate TEXT,
                        endDate TEXT,
                        enabled INTEGER NOT NULL DEFAULT 1,
                        title TEXT,
                        message TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY(medId) REFERENCES med(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // Create index for performance
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_medication_reminder_medId 
                    ON medication_reminder(medId)
                """.trimIndent())
                
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_medication_reminder_enabled 
                    ON medication_reminder(enabled)
                """.trimIndent())
            }
        }
        
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 添加 med_course.startDate 索引以优化日期范围查询
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_med_course_startDate 
                    ON med_course(startDate)
                """.trimIndent())
            }
        }

        fun build(context: Context): DailyReportDatabase = Room.databaseBuilder(
            context.applicationContext,
            DailyReportDatabase::class.java,
            "heldairy-daily-report.db"
        )
            .addMigrations(MIGRATION_7_8, MIGRATION_8_9)
            .fallbackToDestructiveMigration()
            .build()
    }
}

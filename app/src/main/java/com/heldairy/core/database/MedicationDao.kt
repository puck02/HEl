package com.heldairy.core.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.heldairy.core.database.entity.MedCourseEntity
import com.heldairy.core.database.entity.MedEntity
import com.heldairy.core.database.entity.MedEventEntity
import com.heldairy.core.database.entity.MedWithCourses
import com.heldairy.core.database.entity.MedicationReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMed(med: MedEntity): Long

    @Update
    suspend fun updateMed(med: MedEntity)

    @Delete
    suspend fun deleteMed(med: MedEntity)

    @Query("SELECT * FROM med ORDER BY updatedAt DESC")
    fun getAllMeds(): Flow<List<MedEntity>>

    @Query("SELECT * FROM med WHERE id = :medId")
    suspend fun getMedById(medId: Long): MedEntity?

    @Transaction
    @Query("SELECT * FROM med ORDER BY updatedAt DESC")
    fun getAllMedsWithCourses(): Flow<List<MedWithCourses>>

    @Transaction
    @Query("SELECT * FROM med WHERE id = :medId")
    suspend fun getMedWithCoursesById(medId: Long): MedWithCourses?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: MedCourseEntity): Long

    @Update
    suspend fun updateCourse(course: MedCourseEntity)

    @Delete
    suspend fun deleteCourse(course: MedCourseEntity)

    @Query("SELECT * FROM med_course WHERE medId = :medId ORDER BY startDate DESC")
    fun getCoursesByMedId(medId: Long): Flow<List<MedCourseEntity>>

    @Query("SELECT * FROM med_course WHERE id = :courseId")
    suspend fun getCourseById(courseId: Long): MedCourseEntity?

    @Query("SELECT * FROM med_course WHERE status = :status ORDER BY startDate DESC")
    fun getCoursesByStatus(status: String): Flow<List<MedCourseEntity>>

    @Query("""
        SELECT DISTINCT m.* FROM med m
        INNER JOIN med_course mc ON m.id = mc.medId
        WHERE mc.status = 'active'
        ORDER BY m.name ASC
    """)
    fun getActiveMeds(): Flow<List<MedEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: MedEventEntity): Long

    @Query("SELECT * FROM med_event ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentEvents(limit: Int = 50): Flow<List<MedEventEntity>>

    @Query("SELECT * FROM med_event WHERE id = :eventId")
    suspend fun getEventById(eventId: Long): MedEventEntity?
    
    // Reminder operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: MedicationReminderEntity): Long
    
    @Update
    suspend fun updateReminder(reminder: MedicationReminderEntity)
    
    @Query("DELETE FROM medication_reminder WHERE id = :reminderId")
    suspend fun deleteReminder(reminderId: Long)
    
    @Query("SELECT * FROM medication_reminder WHERE medId = :medId ORDER BY hour, minute")
    fun getRemindersByMedId(medId: Long): Flow<List<MedicationReminderEntity>>
    
    @Query("SELECT * FROM medication_reminder WHERE id = :reminderId")
    suspend fun getReminderById(reminderId: Long): MedicationReminderEntity?
    
    @Query("SELECT * FROM medication_reminder WHERE enabled = 1")
    fun getAllEnabledReminders(): Flow<List<MedicationReminderEntity>>
    
    @Query("UPDATE medication_reminder SET enabled = :enabled, updatedAt = :updatedAt WHERE id = :reminderId")
    suspend fun updateReminderEnabled(reminderId: Long, enabled: Boolean, updatedAt: Long)
    
    // Clear operations for data management
    @Query("DELETE FROM medication_reminder")
    suspend fun clearAllReminders()
    
    @Query("DELETE FROM med_event")
    suspend fun clearAllEvents()
    
    @Query("DELETE FROM med_course")
    suspend fun clearAllCourses()
    
    @Query("DELETE FROM med")
    suspend fun clearAllMeds()
}

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
}

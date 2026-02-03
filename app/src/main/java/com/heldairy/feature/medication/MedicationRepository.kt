package com.heldairy.feature.medication

import com.heldairy.core.database.MedicationDao
import com.heldairy.core.database.entity.MedCourseEntity
import com.heldairy.core.database.entity.MedEntity
import com.heldairy.core.database.entity.MedEventEntity
import com.heldairy.core.database.entity.MedicationReminderEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class MedicationRepository(private val dao: MedicationDao) {

    fun getAllMeds(): Flow<List<Med>> = dao.getAllMedsWithCourses().map { list ->
        list.map { medWithCourses ->
            medWithCourses.toMed()
        }
    }

    fun getActiveMeds(): Flow<List<Med>> = dao.getActiveMeds().map { list ->
        list.map { it.toMed() }
    }

    suspend fun getMedById(medId: Long): Med? {
        val medWithCourses = dao.getMedWithCoursesById(medId) ?: return null
        return medWithCourses.toMed()
    }

    suspend fun getMedByName(name: String): Med? {
        val allMeds = dao.getAllMedsWithCourses().first()
        return allMeds.map { it.toMed() }.firstOrNull { 
            it.name.equals(name, ignoreCase = true) 
        }
    }

    suspend fun addMed(
        name: String,
        aliases: String? = null,
        note: String? = null,
        infoSummary: String? = null
    ): Long {
        val now = System.currentTimeMillis()
        val entity = MedEntity(
            name = name,
            aliases = aliases,
            note = note,
            infoSummary = infoSummary,
            createdAt = now,
            updatedAt = now
        )
        return dao.insertMed(entity)
    }

    suspend fun updateMed(
        id: Long,
        name: String,
        aliases: String? = null,
        note: String? = null,
        infoSummary: String? = null
    ) {
        val existing = dao.getMedById(id) ?: return
        dao.updateMed(
            existing.copy(
                name = name,
                aliases = aliases,
                note = note,
                infoSummary = infoSummary,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteMed(medId: Long) {
        val med = dao.getMedById(medId) ?: return
        dao.deleteMed(med)
    }

    fun getCoursesByMedId(medId: Long): Flow<List<MedCourse>> =
        dao.getCoursesByMedId(medId).map { list ->
            list.map { it.toMedCourse() }
        }

    suspend fun addCourse(
        medId: Long,
        startDate: LocalDate,
        endDate: LocalDate? = null,
        status: CourseStatus,
        frequencyText: String,
        doseText: String? = null,
        timeHints: String? = null
    ): Long {
        val now = System.currentTimeMillis()
        val entity = MedCourseEntity(
            medId = medId,
            startDate = startDate,
            endDate = endDate,
            status = status.toDbString(),
            frequencyText = frequencyText,
            doseText = doseText,
            timeHints = timeHints,
            createdAt = now,
            updatedAt = now
        )
        return dao.insertCourse(entity)
    }

    suspend fun updateCourseStatus(courseId: Long, status: CourseStatus, endDate: LocalDate? = null) {
        val course = dao.getCourseById(courseId) ?: return
        dao.updateCourse(
            course.copy(
                status = status.toDbString(),
                endDate = endDate,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteCourse(courseId: Long) {
        val course = dao.getCourseById(courseId) ?: return
        dao.deleteCourse(course)
    }

    suspend fun logEvent(
        rawText: String,
        detectedMedNamesJson: String? = null,
        proposedActionsJson: String? = null,
        confirmedActionsJson: String? = null,
        applyResult: String? = null
    ): Long {
        val entity = MedEventEntity(
            createdAt = System.currentTimeMillis(),
            rawText = rawText,
            detectedMedNamesJson = detectedMedNamesJson,
            proposedActionsJson = proposedActionsJson,
            confirmedActionsJson = confirmedActionsJson,
            applyResult = applyResult
        )
        return dao.insertEvent(entity)
    }

    fun getRecentEvents(limit: Int = 50): Flow<List<MedEventEntity>> =
        dao.getRecentEvents(limit)

    // ========== Reminder Operations ==========
    
    suspend fun addReminder(
        medId: Long,
        hour: Int,
        minute: Int,
        repeatType: com.heldairy.feature.medication.RepeatType,
        weekDays: List<Int>? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        title: String? = null,
        message: String? = null
    ): Long {
        val entity = MedicationReminderEntity(
            medId = medId,
            hour = hour,
            minute = minute,
            repeatType = repeatType.name,
            weekDays = weekDays?.sorted()?.joinToString(","),
            startDate = startDate?.toString(),
            endDate = endDate?.toString(),
            enabled = true,
            title = title,
            message = message,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return dao.insertReminder(entity)
    }

    suspend fun updateReminder(reminder: MedicationReminder) {
        val entity = MedicationReminderEntity(
            id = reminder.id,
            medId = reminder.medId,
            hour = reminder.hour,
            minute = reminder.minute,
            repeatType = reminder.repeatType.name,
            weekDays = reminder.weekDays?.sorted()?.joinToString(","),
            startDate = reminder.startDate?.toString(),
            endDate = reminder.endDate?.toString(),
            enabled = reminder.enabled,
            title = reminder.title,
            message = reminder.message,
            createdAt = reminder.createdAt,
            updatedAt = System.currentTimeMillis()
        )
        dao.updateReminder(entity)
    }

    suspend fun deleteReminder(reminderId: Long) {
        dao.deleteReminder(reminderId)
    }

    suspend fun toggleReminderEnabled(reminderId: Long, enabled: Boolean) {
        dao.updateReminderEnabled(reminderId, enabled, System.currentTimeMillis())
    }

    fun getRemindersByMedId(medId: Long): Flow<List<MedicationReminder>> =
        dao.getRemindersByMedId(medId).map { entities ->
            entities.map { it.toReminder() }
        }

    suspend fun getReminderById(reminderId: Long): MedicationReminder? =
        dao.getReminderById(reminderId)?.toReminder()

    fun getAllEnabledReminders(): Flow<List<MedicationReminder>> =
        dao.getAllEnabledReminders().map { entities ->
            entities.map { it.toReminder() }
        }
    
    suspend fun clearAll() {
        // Clear in order: reminders/events → courses → meds (respects foreign key constraints)
        dao.clearAllReminders()
        dao.clearAllEvents()
        dao.clearAllCourses()
        dao.clearAllMeds()
    }
}

private fun MedEntity.toMed() = Med(
    id = id,
    name = name,
    aliases = aliases,
    note = note,
    infoSummary = infoSummary,
    imageUri = imageUri,
    hasActiveCourse = false
)

private fun com.heldairy.core.database.entity.MedWithCourses.toMed(): Med {
    val activeCourse = courses.firstOrNull { it.status == MedCourseEntity.STATUS_ACTIVE }
    val pausedCourse = courses.firstOrNull { it.status == MedCourseEntity.STATUS_PAUSED }
    val latestCourse = activeCourse ?: pausedCourse ?: courses.maxByOrNull { it.id }
    
    return Med(
        id = med.id,
        name = med.name,
        aliases = med.aliases,
        note = med.note,
        infoSummary = med.infoSummary,
        imageUri = med.imageUri,
        hasActiveCourse = activeCourse != null,
        currentCourse = latestCourse?.toMedCourse(),
        createdAt = java.time.Instant.ofEpochMilli(med.createdAt)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate(),
        updatedAt = java.time.Instant.ofEpochMilli(med.updatedAt)
            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    )
}

private fun MedCourseEntity.toMedCourse() = MedCourse(
    id = id,
    medId = medId,
    startDate = startDate,
    endDate = endDate,
    status = CourseStatus.fromDbString(status),
    frequencyText = frequencyText,
    doseText = doseText,
    timeHints = timeHints
)

private fun MedicationReminderEntity.toReminder() = MedicationReminder(
    id = id,
    medId = medId,
    hour = hour,
    minute = minute,
    repeatType = com.heldairy.feature.medication.RepeatType.valueOf(repeatType),
    weekDays = weekDays?.split(",")?.mapNotNull { it.toIntOrNull() },
    startDate = startDate?.let { LocalDate.parse(it) },
    endDate = endDate?.let { LocalDate.parse(it) },
    enabled = enabled,
    title = title,
    message = message,
    createdAt = createdAt,
    updatedAt = updatedAt
)

package com.heldairy.feature.medication

import com.heldairy.core.database.MedicationDao
import com.heldairy.core.database.entity.MedCourseEntity
import com.heldairy.core.database.entity.MedEntity
import com.heldairy.core.database.entity.MedEventEntity
import kotlinx.coroutines.flow.Flow
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
        val courses = dao.getCoursesByMedId(0).map { it.find { c -> c.id == courseId } }
        // This is a simplified version; in practice we'd need to query by course ID
        // For now, we'll add a direct query method
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

private fun com.heldairy.core.database.entity.MedWithCourses.toMed() = Med(
    id = med.id,
    name = med.name,
    aliases = med.aliases,
    note = med.note,
    infoSummary = med.infoSummary,
    imageUri = med.imageUri,
    hasActiveCourse = courses.any { it.status == MedCourseEntity.STATUS_ACTIVE }
)

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

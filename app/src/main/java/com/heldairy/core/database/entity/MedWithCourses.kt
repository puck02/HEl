package com.heldairy.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class MedWithCourses(
    @Embedded val med: MedEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "medId"
    )
    val courses: List<MedCourseEntity>
)

package com.heldairy.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "med_event")
data class MedEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val createdAt: Long,
    val rawText: String,
    val detectedMedNamesJson: String? = null,
    val proposedActionsJson: String? = null,
    val confirmedActionsJson: String? = null,
    val applyResult: String? = null
)

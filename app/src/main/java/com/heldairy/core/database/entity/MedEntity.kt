package com.heldairy.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "med")
data class MedEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val aliases: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val note: String? = null,
    val infoSummary: String? = null,
    val imageUri: String? = null
)

package com.heldairy.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "question_responses",
    foreignKeys = [
        ForeignKey(
            entity = DailyEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entry_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["entry_id"]),
        Index(value = ["question_id"])
    ]
)
data class QuestionResponseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "entry_id") val entryId: Long,
    @ColumnInfo(name = "question_id") val questionId: String,
    @ColumnInfo(name = "step_index") val stepIndex: Int,
    @ColumnInfo(name = "question_order") val questionOrder: Int,
    @ColumnInfo(name = "answer_type") val answerType: String,
    @ColumnInfo(name = "answer_value") val answerValue: String,
    @ColumnInfo(name = "answer_label") val answerLabel: String,
    @ColumnInfo(name = "metadata_json") val metadataJson: String?,
    @ColumnInfo(name = "answered_at") val answeredAt: Long
)

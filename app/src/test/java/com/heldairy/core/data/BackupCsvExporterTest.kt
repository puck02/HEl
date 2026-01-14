package com.heldairy.core.data

import com.heldairy.core.database.entity.DailyEntryEntity
import com.heldairy.core.database.entity.DailyEntrySnapshot
import com.heldairy.core.database.entity.QuestionResponseEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupCsvExporterTest {
    @Test
    fun buildsCsvWithHeaderAndRows() {
        val snapshot = DailyEntrySnapshot(
            entry = DailyEntryEntity(id = 1, entryDate = "2026-01-14", createdAt = 100L, timezoneId = "UTC"),
            responses = listOf(
                QuestionResponseEntity(
                    id = 1,
                    entryId = 1,
                    questionId = "q1",
                    stepIndex = 0,
                    questionOrder = 0,
                    answerType = "single_choice",
                    answerValue = "ok",
                    answerLabel = "fine",
                    metadataJson = null,
                    answeredAt = 100L
                ),
                QuestionResponseEntity(
                    id = 2,
                    entryId = 1,
                    questionId = "q2",
                    stepIndex = 1,
                    questionOrder = 1,
                    answerType = "text",
                    answerValue = "note,with,comma",
                    answerLabel = "note,with,comma",
                    metadataJson = null,
                    answeredAt = 101L
                )
            ),
            advice = null,
            summary = null
        )

        val csv = CsvExporter.buildCsv(listOf(snapshot)).lines()

        assertEquals("entry_date,question_id,answer_label,answer_value,step_index,answer_type,answered_at", csv[0])
        assertEquals("2026-01-14,q1,fine,ok,0,single_choice,100", csv[1])
        assertEquals("2026-01-14,q2,\"note,with,comma\",\"note,with,comma\",1,text,101", csv[2])
    }
}

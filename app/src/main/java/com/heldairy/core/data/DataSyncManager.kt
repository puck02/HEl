package com.heldairy.core.data

import android.util.Log
import com.heldairy.core.database.DailyReportDatabase
import com.heldairy.core.database.entity.DailyAdviceEntity
import com.heldairy.core.database.entity.DailyEntryEntity
import com.heldairy.core.database.entity.DailySummaryEntity
import com.heldairy.core.database.entity.MedCourseEntity
import com.heldairy.core.database.entity.MedEntity
import com.heldairy.core.database.entity.QuestionResponseEntity
import com.heldairy.core.network.agent.AgentClient
import com.heldairy.core.network.agent.SyncEntityEnvelope
import com.heldairy.core.network.agent.HealthEntrySync
import com.heldairy.core.network.agent.MedicationCourseSync
import com.heldairy.core.network.agent.MedicationSync
import com.heldairy.core.network.agent.QuestionResponseSync
import com.heldairy.core.network.agent.SyncTombstone
import com.heldairy.core.network.agent.SyncUploadRequest
import com.heldairy.core.preferences.AgentPreferencesStore
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.time.LocalDate

/**
 * 增量数据同步管理器
 *
 * 策略：客户端优先（client-first conflict resolution）
 * - 同步仅上传：Android Room → hel-agent PostgreSQL
 * - 每次同步只发送上次同步后新增/修改的数据
 * - 服务端以 android_id 做幂等去重
 */
class DataSyncManager(
    private val database: DailyReportDatabase,
    private val agentClient: AgentClient,
    private val agentPrefs: AgentPreferencesStore,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    companion object {
        private const val TAG = "DataSyncManager"
    }

    /**
     * 执行增量同步
     *
     * @return true 表示同步成功
     */
    suspend fun syncNow(): Boolean {
        val settings = agentPrefs.currentSettings()
        if (!settings.isReady) {
            Log.w(TAG, "Agent not ready, skip sync")
            return false
        }

        val lastSync = settings.lastSyncTimestamp
        Log.d(TAG, "Starting sync, lastSyncTimestamp=$lastSync")

        return try {
            // 1. 收集需要同步的数据
            val entries = collectEntries(lastSync)
            val medications = collectMedications(lastSync)
            val courses = collectCourses(lastSync)

            var nextSyncTs = lastSync
            val hasLocalChanges = entries.isNotEmpty() || medications.isNotEmpty() || courses.isNotEmpty()

            if (hasLocalChanges) {
                Log.d(TAG, "Syncing local changes: ${entries.size} entries, ${medications.size} meds, ${courses.size} courses")

                val changes = buildSyncChanges(entries, medications, courses)
                val pushResponse = runCatching {
                    agentClient.syncPush(
                        com.heldairy.core.network.agent.SyncPushRequest(
                            clientChangeId = "android-${System.currentTimeMillis()}",
                            baseServerVersion = lastSync,
                            changes = changes
                        )
                    )
                }

                if (pushResponse.isSuccess) {
                    val response = pushResponse.getOrThrow()
                    nextSyncTs = maxOf(nextSyncTs, response.serverCursor, response.serverTimestamp)
                    Log.d(TAG, "Sync v2 push done: applied=${response.applied}, conflicts=${response.conflicts}")
                } else {
                    // fallback: v1 upload
                    val request = SyncUploadRequest(
                        lastSyncTimestamp = lastSync,
                        entries = entries,
                        medications = medications,
                        medicationCourses = courses
                    )
                    val response = agentClient.syncUpload(request)
                    nextSyncTs = maxOf(nextSyncTs, response.serverTimestamp)
                    Log.d(TAG, "Sync v1 upload done: entriesSynced=${response.entriesSynced}, medsSynced=${response.medicationsSynced}")
                }
            }

            // 2. pull server changes and apply to Room
            runCatching {
                val pull = agentClient.syncPull(since = lastSync, limit = 300)
                if (pull.changes.isNotEmpty()) {
                    applyPulledChanges(pull.changes)
                }
                if (pull.tombstones.isNotEmpty()) {
                    applyPulledTombstones(pull.tombstones)
                }
                nextSyncTs = maxOf(nextSyncTs, pull.nextCursor, pull.serverTime)
            }.onFailure {
                Log.w(TAG, "Sync pull skipped: ${it.message}")
            }

            // 3. update cursor
            if (nextSyncTs > lastSync) {
                agentPrefs.updateLastSyncTimestamp(nextSyncTs)
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e.message}", e)
            false
        }
    }

    // ── 收集增量数据 ──────────────────────────────────────

    private suspend fun collectEntries(sinceTimestamp: Long): List<HealthEntrySync> {
        // 获取所有条目（Room 没有按 createdAt 过滤的现成方法，
        // 这里加载最近条目后在内存中过滤）
        val allEntries = database.dailyReportDao().loadRecentEntries(limit = 200)
        return allEntries
            .filter { it.entry.createdAt > sinceTimestamp }
            .map { entryWithResponses ->
                val entry = entryWithResponses.entry
                val responses = entryWithResponses.responses.map { r ->
                    QuestionResponseSync(
                        questionId = r.questionId,
                        stepIndex = r.stepIndex,
                        answerType = r.answerType,
                        answerValue = r.answerValue,
                        answerLabel = r.answerLabel,
                        metadataJson = null  // 简化：metadataJson 是 String，需要解析为 Map
                    )
                }
                HealthEntrySync(
                    androidId = entry.id,
                    entryDate = entry.entryDate,
                    timezoneId = entry.timezoneId,
                    createdAt = entry.createdAt,
                    questionResponses = responses
                )
            }
    }

    private suspend fun collectMedications(sinceTimestamp: Long): List<MedicationSync> {
        val allMeds = database.medicationDao().loadAllMedsSuspend()
        return allMeds
            .filter { it.createdAt > sinceTimestamp || it.updatedAt > sinceTimestamp }
            .map { med ->
                MedicationSync(
                    androidId = med.id,
                    name = med.name,
                    aliases = med.aliases,
                    note = med.note,
                    infoSummary = med.infoSummary
                )
            }
    }

    private suspend fun collectCourses(sinceTimestamp: Long): List<MedicationCourseSync> {
        val allCourses = database.medicationDao().loadAllCoursesSuspend()
        return allCourses
            .filter { it.createdAt > sinceTimestamp || it.updatedAt > sinceTimestamp }
            .map { course ->
                MedicationCourseSync(
                    medAndroidId = course.medId,
                    startDate = course.startDate.toString(),
                    endDate = course.endDate?.toString(),
                    status = course.status,
                    frequencyText = course.frequencyText,
                    doseText = course.doseText,
                    timeHints = course.timeHints
                )
            }
    }

    private fun buildSyncChanges(
        entries: List<HealthEntrySync>,
        medications: List<MedicationSync>,
        courses: List<MedicationCourseSync>
    ): List<com.heldairy.core.network.agent.SyncChange> {
        val entryChanges = entries.map { entry ->
            com.heldairy.core.network.agent.SyncChange(
                entity = "health_entry",
                op = "upsert",
                payload = buildEntryPayload(entry)
            )
        }

        val medChanges = medications.map { med ->
            com.heldairy.core.network.agent.SyncChange(
                entity = "medication",
                op = "upsert",
                payload = buildMedicationPayload(med)
            )
        }

        val courseChanges = courses.map { course ->
            com.heldairy.core.network.agent.SyncChange(
                entity = "medication_course",
                op = "upsert",
                payload = buildCoursePayload(course)
            )
        }

        return entryChanges + medChanges + courseChanges
    }

    private fun buildEntryPayload(entry: HealthEntrySync): Map<String, kotlinx.serialization.json.JsonElement> {
        val questionResponses = buildJsonArray {
            entry.questionResponses.forEach { qr ->
                add(
                    buildJsonObject {
                        put("question_id", JsonPrimitive(qr.questionId))
                        put("step_index", JsonPrimitive(qr.stepIndex))
                        put("answer_type", JsonPrimitive(qr.answerType))
                        qr.answerValue?.let { put("answer_value", JsonPrimitive(it)) }
                        qr.answerLabel?.let { put("answer_label", JsonPrimitive(it)) }
                    }
                )
            }
        }

        val payload = buildJsonObject {
            put("android_id", JsonPrimitive(entry.androidId))
            put("entry_date", JsonPrimitive(entry.entryDate))
            entry.timezoneId?.let { put("timezone_id", JsonPrimitive(it)) }
            put("created_at", JsonPrimitive(entry.createdAt))
            put("question_responses", questionResponses)
        }
        return payload
    }

    private fun buildMedicationPayload(med: MedicationSync): Map<String, kotlinx.serialization.json.JsonElement> {
        val payload = buildJsonObject {
            put("android_id", JsonPrimitive(med.androidId))
            put("name", JsonPrimitive(med.name))
            med.aliases?.let { put("aliases", JsonPrimitive(it)) }
            med.note?.let { put("note", JsonPrimitive(it)) }
            med.infoSummary?.let { put("info_summary", JsonPrimitive(it)) }
        }
        return payload
    }

    private fun buildCoursePayload(course: MedicationCourseSync): Map<String, kotlinx.serialization.json.JsonElement> {
        val payload = buildJsonObject {
            put("med_android_id", JsonPrimitive(course.medAndroidId))
            put("start_date", JsonPrimitive(course.startDate))
            course.endDate?.let { put("end_date", JsonPrimitive(it)) }
            put("status", JsonPrimitive(course.status))
            course.frequencyText?.let { put("frequency_text", JsonPrimitive(it)) }
            course.doseText?.let { put("dose_text", JsonPrimitive(it)) }
            course.timeHints?.let { put("time_hints", JsonPrimitive(it)) }
        }
        return payload
    }

    private suspend fun applyPulledChanges(changes: List<SyncEntityEnvelope>) {
        val sorted = changes.sortedBy { it.serverVersion }
        val medIdMap = mutableMapOf<Long, Long>()

        sorted.forEach { envelope ->
            when (envelope.entity) {
                "health_entry" -> applyHealthEntry(envelope.payload)
                "medication" -> {
                    val remoteId = envelope.payload.long("android_id")
                    val localId = applyMedication(envelope.payload)
                    if (remoteId != null && localId != null) medIdMap[remoteId] = localId
                }
                "medication_course" -> applyMedicationCourse(envelope.payload, medIdMap)
            }
        }
    }

    private suspend fun applyPulledTombstones(tombstones: List<SyncTombstone>) {
        tombstones.sortedBy { it.deletedAt }.forEach { tombstone ->
            when (tombstone.entity) {
                "health_entry" -> deleteHealthEntry(tombstone)
                "medication" -> deleteMedication(tombstone)
                "medication_course" -> deleteMedicationCourse(tombstone)
            }
        }
    }

    private suspend fun deleteHealthEntry(tombstone: SyncTombstone) {
        val dao = database.dailyReportDao()
        val payload = tombstone.payload
        val entryDate = payload.string("entry_date")
        val entryId = when {
            !entryDate.isNullOrBlank() -> dao.findEntryIdByDate(entryDate)
            else -> tombstone.recordId.takeIf { it > 0 }
        }
        if (entryId != null) {
            dao.deleteEntryById(entryId)
        }
    }

    private suspend fun deleteMedication(tombstone: SyncTombstone) {
        val medDao = database.medicationDao()
        val payload = tombstone.payload
        val androidId = payload.long("android_id")
        val name = payload.string("name")
        val all = medDao.loadAllMedsSuspend()

        val target = all.firstOrNull {
            (androidId != null && it.id == androidId) ||
                (!name.isNullOrBlank() && it.name.equals(name, ignoreCase = true)) ||
                it.id == tombstone.recordId
        }

        if (target != null) {
            medDao.deleteMed(target)
        }
    }

    private suspend fun deleteMedicationCourse(tombstone: SyncTombstone) {
        val medDao = database.medicationDao()
        val payload = tombstone.payload
        val medAndroidId = payload.long("med_android_id")
        val startDate = payload.string("start_date")?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
        val status = payload.string("status")

        val all = medDao.loadAllCoursesSuspend()
        val target = all.firstOrNull {
            it.id == tombstone.recordId ||
                ((medAndroidId == null || it.medId == medAndroidId) &&
                    (startDate == null || it.startDate == startDate) &&
                    (status.isNullOrBlank() || it.status == status))
        }

        if (target != null) {
            medDao.deleteCourse(target)
        }
    }

    private suspend fun applyHealthEntry(payload: Map<String, JsonElement>) {
        val entryDate = payload.string("entry_date") ?: return
        val timezoneId = payload.string("timezone_id") ?: "Asia/Shanghai"
        val createdAt = payload.long("created_at") ?: System.currentTimeMillis()

        val responses = payload.array("question_responses").mapIndexed { idx, item ->
            val obj = item.asObject()
            QuestionResponseEntity(
                entryId = 0L,
                questionId = obj.string("question_id") ?: "q_$idx",
                stepIndex = obj.int("step_index") ?: 0,
                questionOrder = idx,
                answerType = obj.string("answer_type") ?: "choice",
                answerValue = obj.string("answer_value") ?: "",
                answerLabel = obj.string("answer_label") ?: "",
                metadataJson = obj.get("metadata_json")?.toString(),
                answeredAt = createdAt
            )
        }

        val dao = database.dailyReportDao()
        val existingId = dao.findEntryIdByDate(entryDate)
        val entry = DailyEntryEntity(
            id = existingId ?: 0L,
            entryDate = entryDate,
            createdAt = createdAt,
            timezoneId = timezoneId
        )
        val entryId = if (existingId != null) {
            dao.replaceEntryWithResponses(entry, responses)
        } else {
            dao.insertEntryWithResponses(entry, responses)
        }

        payload.obj("daily_advice")?.let { adviceObj ->
            dao.upsertAdvice(
                DailyAdviceEntity(
                    entryId = entryId,
                    entryDate = entryDate,
                    model = adviceObj.string("model") ?: "agent",
                    adviceJson = adviceObj.obj("advice_json")?.toString() ?: "{}",
                    promptHash = adviceObj.string("prompt_hash") ?: "",
                    generatedAt = adviceObj.long("generated_at") ?: createdAt
                )
            )
        }

        payload.obj("daily_summary")?.let { summaryObj ->
            dao.upsertSummary(
                DailySummaryEntity(
                    entryId = entryId,
                    entryDate = entryDate,
                    window7Json = summaryObj.obj("window_7d_json")?.toString(),
                    window30Json = summaryObj.obj("window_30d_json")?.toString(),
                    computedAt = summaryObj.long("computed_at") ?: createdAt
                )
            )
        }
    }

    private suspend fun applyMedication(payload: Map<String, JsonElement>): Long? {
        val name = payload.string("name") ?: return null
        val medDao = database.medicationDao()
        val all = medDao.loadAllMedsSuspend()
        val existing = all.firstOrNull { it.name.equals(name, ignoreCase = true) }
        val now = System.currentTimeMillis()

        return if (existing != null) {
            medDao.updateMed(
                existing.copy(
                    aliases = payload.string("aliases"),
                    note = payload.string("note"),
                    infoSummary = payload.string("info_summary"),
                    updatedAt = now
                )
            )
            existing.id
        } else {
            medDao.insertMed(
                MedEntity(
                    name = name,
                    aliases = payload.string("aliases"),
                    note = payload.string("note"),
                    infoSummary = payload.string("info_summary"),
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    private suspend fun applyMedicationCourse(
        payload: Map<String, JsonElement>,
        medIdMap: Map<Long, Long>
    ) {
        val remoteMedId = payload.long("med_android_id") ?: return
        val medDao = database.medicationDao()
        val localMedId = medIdMap[remoteMedId] ?: medDao.getMedById(remoteMedId)?.id ?: return

        val startDate = payload.string("start_date")?.let { LocalDate.parse(it) } ?: return
        val endDate = payload.string("end_date")?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) }
        val status = payload.string("status") ?: "active"
        val frequencyText = payload.string("frequency_text") ?: ""
        val doseText = payload.string("dose_text")
        val timeHints = payload.string("time_hints")
        val now = System.currentTimeMillis()

        val existing = medDao.loadAllCoursesSuspend().firstOrNull {
            it.medId == localMedId && it.startDate == startDate && it.status == status
        }

        if (existing != null) {
            medDao.updateCourse(
                existing.copy(
                    endDate = endDate,
                    frequencyText = frequencyText,
                    doseText = doseText,
                    timeHints = timeHints,
                    updatedAt = now
                )
            )
        } else {
            medDao.insertCourse(
                MedCourseEntity(
                    medId = localMedId,
                    startDate = startDate,
                    endDate = endDate,
                    status = status,
                    frequencyText = frequencyText,
                    doseText = doseText,
                    timeHints = timeHints,
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    private fun Map<String, JsonElement>.string(key: String): String? =
        get(key)?.jsonPrimitive?.contentOrNull

    private fun Map<String, JsonElement>.long(key: String): Long? =
        get(key)?.jsonPrimitive?.longOrNull

    private fun Map<String, JsonElement>.int(key: String): Int? =
        get(key)?.jsonPrimitive?.intOrNull

    private fun Map<String, JsonElement>.array(key: String): List<JsonElement> =
        get(key)?.asArray() ?: emptyList()

    private fun Map<String, JsonElement>.obj(key: String): Map<String, JsonElement>? =
        get(key)?.asObject()

    private fun JsonElement.asObject(): Map<String, JsonElement> =
        runCatching { this.jsonObject.toMap() }.getOrDefault(emptyMap())

    private fun JsonElement.asArray(): List<JsonElement> =
        runCatching { this.jsonArray.toList() }.getOrDefault(emptyList())
}

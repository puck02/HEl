package com.heldairy.core.data

import android.util.Log
import com.heldairy.core.database.DailyReportDatabase
import com.heldairy.core.network.agent.AgentClient
import com.heldairy.core.network.agent.HealthEntrySync
import com.heldairy.core.network.agent.MedicationCourseSync
import com.heldairy.core.network.agent.MedicationSync
import com.heldairy.core.network.agent.QuestionResponseSync
import com.heldairy.core.network.agent.DailyAdviceSync
import com.heldairy.core.network.agent.DailySummarySync
import com.heldairy.core.network.agent.SyncUploadRequest
import com.heldairy.core.preferences.AgentPreferencesStore
import kotlinx.serialization.json.Json

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

            if (entries.isEmpty() && medications.isEmpty() && courses.isEmpty()) {
                Log.d(TAG, "Nothing to sync")
                return true
            }

            Log.d(TAG, "Syncing: ${entries.size} entries, ${medications.size} meds, ${courses.size} courses")

            // 2. 上传
            val request = SyncUploadRequest(
                lastSyncTimestamp = lastSync,
                entries = entries,
                medications = medications,
                medicationCourses = courses
            )
            val response = agentClient.syncUpload(request)

            // 3. 更新同步时间戳
            agentPrefs.updateLastSyncTimestamp(response.serverTimestamp)
            Log.d(TAG, "Sync done: entriesSynced=${response.entriesSynced}, medsSynced=${response.medicationsSynced}")

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
}

package com.heldairy.core.testing

import com.heldairy.core.database.DailyReportDao
import com.heldairy.core.database.entity.DailyEntryEntity
import com.heldairy.core.database.entity.QuestionResponseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * 测试数据生成器
 * 用于生成一年量的假数据以测试报表功能
 */
class TestDataGenerator(
    private val dao: DailyReportDao
) {

    /**
     * 生成一年量的测试数据
     * @param startDate 开始日期（默认一年前）
     * @param endDate 结束日期（默认今天）
     * @param completionRate 数据完整度（0.0-1.0，默认0.85）
     */
    suspend fun generateYearOfData(
        startDate: LocalDate = LocalDate.now().minusYears(1),
        endDate: LocalDate = LocalDate.now(),
        completionRate: Double = 0.85
    ) = withContext(Dispatchers.IO) {
        
        val totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
        val daysToGenerate = (totalDays * completionRate).toInt()
        
        // 随机选择要生成数据的日期
        val allDates = (0 until totalDays).map { startDate.plusDays(it.toLong()) }
        val selectedDates = allDates.shuffled().take(daysToGenerate).sorted()
        
        selectedDates.forEach { date ->
            generateDayData(date)
        }
    }

    /**
     * 生成单日测试数据
     */
    private suspend fun generateDayData(date: LocalDate) {
        val entryId = dao.insertEntry(
            DailyEntryEntity(
                entryDate = date.toString(),
                createdAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                timezoneId = ZoneId.systemDefault().id
            )
        )

        // Step 0: 整体感觉
        insertResponse(entryId, "overall_feeling", 0, 0, "choice", 
            listOf("很好", "还行", "有点不舒服", "很难受").random())

        // Step 0: 关注重点（多选）
        val focusOptions = listOf("头痛", "颈肩腰", "胃", "鼻咽", "膝盖", "情绪", "睡眠", "经期", "没有特别")
        val selectedFocus = focusOptions.shuffled().take(Random.nextInt(1, 3))
        insertResponse(entryId, "focus_priority", 0, 1, "multi_choice", 
            selectedFocus.joinToString(","))

        // Step 1: 基线指标
        // 睡眠时长
        insertResponse(entryId, "sleep_duration", 1, 0, "choice",
            listOf("lt6", "6_7", "7_8", "gt8").random(), 
            getSleepLabel(listOf("lt6", "6_7", "7_8", "gt8").random()))

        // 午睡时长
        insertResponse(entryId, "nap_duration", 1, 1, "choice",
            listOf("none", "lt30", "30_60", "gt60").random(),
            getNapLabel(listOf("none", "lt30", "30_60", "gt60").random()))

        // 日均步数
        insertResponse(entryId, "daily_steps", 1, 2, "choice",
            listOf("lt3k", "3_6k", "6_10k", "gt10k").random(),
            getStepsLabel(listOf("lt3k", "3_6k", "6_10k", "gt10k").random()))

        // 症状强度（0-10）
        val headache = Random.nextDouble(0.0, 8.0)
        insertResponse(entryId, "headache_intensity", 1, 3, "slider", 
            headache.toString(), String.format("%.1f", headache))

        val neckBack = Random.nextDouble(0.0, 8.0)
        insertResponse(entryId, "neck_back_intensity", 1, 4, "slider",
            neckBack.toString(), String.format("%.1f", neckBack))

        val stomach = Random.nextDouble(0.0, 6.0)
        insertResponse(entryId, "stomach_intensity", 1, 5, "slider",
            stomach.toString(), String.format("%.1f", stomach))

        val nasal = Random.nextDouble(0.0, 5.0)
        insertResponse(entryId, "nasal_intensity", 1, 6, "slider",
            nasal.toString(), String.format("%.1f", nasal))

        val knee = Random.nextDouble(0.0, 6.0)
        insertResponse(entryId, "knee_intensity", 1, 7, "slider",
            knee.toString(), String.format("%.1f", knee))

        val mood = Random.nextDouble(0.0, 7.0)
        insertResponse(entryId, "mood_irritability", 1, 8, "slider",
            mood.toString(), String.format("%.1f", mood))

        // 受凉情况
        insertResponse(entryId, "chill_exposure", 1, 9, "choice",
            if (Random.nextDouble() < 0.2) "yes" else "no",
            if (Random.nextDouble() < 0.2) "是" else "否")

        // 用药依从性
        insertResponse(entryId, "medication_adherence", 1, 10, "choice",
            listOf("on_time", "missed", "na").random(),
            getMedicationLabel(listOf("on_time", "missed", "na").random()))

        // 经期状态
        insertResponse(entryId, "menstrual_status", 1, 11, "choice",
            listOf("period", "non_period", "irregular").random(),
            getMenstrualLabel(listOf("period", "non_period", "irregular").random()))

        // 每日备注（随机添加）
        if (Random.nextDouble() < 0.3) {
            val notes = listOf(
                "今天感觉不错",
                "工作有点累",
                "睡眠质量很好",
                "有点头疼",
                "天气冷了",
                "运动后感觉舒服多了",
                "吃了辣的东西胃不舒服"
            )
            insertResponse(entryId, "daily_notes", 1, 12, "text",
                notes.random(), notes.random())
        }
    }

    private suspend fun insertResponse(
        entryId: Long,
        questionId: String,
        stepIndex: Int,
        questionOrder: Int,
        answerType: String,
        answerValue: String,
        answerLabel: String = answerValue
    ) {
        dao.upsertResponses(
            listOf(
                QuestionResponseEntity(
                    entryId = entryId,
                    questionId = questionId,
                    stepIndex = stepIndex,
                    questionOrder = questionOrder,
                    answerType = answerType,
                    answerValue = answerValue,
                    answerLabel = answerLabel,
                    metadataJson = null,
                    answeredAt = System.currentTimeMillis()
                )
            )
        )
    }

    private fun getSleepLabel(value: String) = when (value) {
        "lt6" -> "少于6小时"
        "6_7" -> "6-7小时"
        "7_8" -> "7-8小时"
        "gt8" -> "多于8小时"
        else -> value
    }

    private fun getNapLabel(value: String) = when (value) {
        "none" -> "无午睡"
        "lt30" -> "少于30分钟"
        "30_60" -> "30-60分钟"
        "gt60" -> "多于60分钟"
        else -> value
    }

    private fun getStepsLabel(value: String) = when (value) {
        "lt3k" -> "少于3000步"
        "3_6k" -> "3000-6000步"
        "6_10k" -> "6000-10000步"
        "gt10k" -> "多于10000步"
        else -> value
    }

    private fun getMedicationLabel(value: String) = when (value) {
        "on_time" -> "按时服用"
        "missed" -> "有遗漏"
        "na" -> "无需/未用"
        else -> value
    }

    private fun getMenstrualLabel(value: String) = when (value) {
        "period" -> "经期"
        "non_period" -> "非经期"
        "irregular" -> "有异常"
        else -> value
    }

    /**
     * 清除所有数据
     */
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        dao.clearEntries()
        dao.clearResponses()
    }
}

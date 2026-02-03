package com.heldairy.core.data

import com.heldairy.core.database.DailyReportDao
import com.heldairy.core.database.entity.AdviceCategory
import com.heldairy.core.database.entity.AdviceTrackingEntity
import com.heldairy.core.database.entity.UserFeedback
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 阶段2：建议追踪Repository
 * 管理AI建议的保存、反馈收集和效果追踪
 */
class AdviceTrackingRepository(
    private val dao: DailyReportDao,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    
    /**
     * 保存AI生成的建议为可追踪项
     * 将AdvicePayload拆分为独立的追踪条目
     */
    suspend fun saveAdviceAsTrackable(
        entryId: Long,
        entryDate: LocalDate,
        payload: AdvicePayload
    ) = withContext(ioDispatcher) {
        val trackings = mutableListOf<AdviceTrackingEntity>()
        
        // observations
        payload.observations.forEach { observation ->
            trackings.add(
                AdviceTrackingEntity(
                    entryId = entryId,
                    adviceText = observation,
                    generatedDate = entryDate.toString(),
                    category = AdviceCategory.inferFromText(observation),
                    sourceField = "observation"
                )
            )
        }
        
        // actions
        payload.actions.forEach { action ->
            trackings.add(
                AdviceTrackingEntity(
                    entryId = entryId,
                    adviceText = action,
                    generatedDate = entryDate.toString(),
                    category = AdviceCategory.inferFromText(action),
                    sourceField = "action"
                )
            )
        }
        
        // tomorrowFocus (重要建议)
        payload.tomorrowFocus.forEach { focus ->
            trackings.add(
                AdviceTrackingEntity(
                    entryId = entryId,
                    adviceText = focus,
                    generatedDate = entryDate.toString(),
                    category = AdviceCategory.inferFromText(focus),
                    sourceField = "tomorrowFocus"
                )
            )
        }
        
        if (trackings.isNotEmpty()) {
            dao.insertAdviceTrackings(trackings)
        }
    }
    
    /**
     * 获取指定 entry 的所有 tracking 记录（阶段3 UI需要）
     */
    suspend fun getTrackingsForEntry(entryId: Long): List<AdviceTrackingEntity> = withContext(ioDispatcher) {
        dao.getTrackingsForEntry(entryId)
    }
    
    /**
     * 用户标记建议为"有帮助"
     */
    suspend fun markAsHelpful(trackingId: Long) = withContext(ioDispatcher) {
        val tracking = dao.getTrackingsForEntry(trackingId).firstOrNull { it.id == trackingId }
        tracking?.let {
            dao.updateAdviceTracking(
                it.copy(
                    userFeedback = UserFeedback.HELPFUL,
                    feedbackAt = clock.instant().toEpochMilli()
                )
            )
        }
    }
    
    /**
     * 用户标记建议为"无帮助"
     */
    suspend fun markAsNotHelpful(trackingId: Long) = withContext(ioDispatcher) {
        val tracking = dao.getTrackingsForEntry(trackingId).firstOrNull { it.id == trackingId }
        tracking?.let {
            dao.updateAdviceTracking(
                it.copy(
                    userFeedback = UserFeedback.NOT_HELPFUL,
                    feedbackAt = clock.instant().toEpochMilli()
                )
            )
        }
    }
    
    /**
     * 用户标记建议为"已执行"并可选评分
     */
    suspend fun markAsExecuted(
        trackingId: Long,
        effectivenessScore: Int? = null,
        executionNote: String? = null
    ) = withContext(ioDispatcher) {
        val tracking = dao.getTrackingsForEntry(trackingId).firstOrNull { it.id == trackingId }
        tracking?.let {
            dao.updateAdviceTracking(
                it.copy(
                    userFeedback = UserFeedback.EXECUTED,
                    feedbackAt = clock.instant().toEpochMilli(),
                    effectivenessScore = effectivenessScore?.coerceIn(1, 5),
                    executionNote = executionNote
                )
            )
        }
    }
    
    /**
     * 获取最近一周的所有建议
     */
    suspend fun getRecentWeekTrackings(): List<AdviceTrackingEntity> = 
        withContext(ioDispatcher) {
            val today = LocalDate.now(clock)
            val weekAgo = today.minusDays(7)
            dao.getTrackingsInDateRange(weekAgo.toString(), today.toString())
        }
    
    /**
     * 获取用户已执行且有效果评分的建议
     * 用于AI生成新建议时参考哪些建议真正有效
     */
    suspend fun getExecutedWithFeedback(limit: Int = 10): List<AdviceTrackingEntity> =
        withContext(ioDispatcher) {
            dao.getExecutedWithScore(limit)
        }
    
    /**
     * 生成建议有效性摘要
     * 用于在AI Prompt中展示历史建议的执行情况
     */
    suspend fun generateEffectivenessSummary(): String = withContext(ioDispatcher) {
        val executed = dao.getTrackingsByFeedback(UserFeedback.EXECUTED, limit = 20)
        
        if (executed.isEmpty()) {
            return@withContext "暂无执行记录"
        }
        
        val withScore = executed.filter { it.effectivenessScore != null }
        val avgScore = withScore.map { it.effectivenessScore!! }.average()
        
        val byCategory = executed.groupBy { it.category }
        val categoryStats = byCategory.mapValues { (_, items) ->
            val scored = items.filter { it.effectivenessScore != null }
            if (scored.isEmpty()) "无评分"
            else "平均${scored.map { it.effectivenessScore!! }.average().format(1)}分"
        }
        
        buildString {
            appendLine("## 历史建议执行情况")
            appendLine("- 已执行建议：${executed.size}条")
            if (withScore.isNotEmpty()) {
                appendLine("- 平均效果评分：${avgScore.format(1)}/5")
            }
            appendLine("- 分类效果：")
            categoryStats.forEach { (category, stat) ->
                appendLine("  * ${translateCategory(category)}: $stat")
            }
            appendLine()
            appendLine("高评分建议示例：")
            withScore.sortedByDescending { it.effectivenessScore }
                .take(3)
                .forEach { tracking ->
                    appendLine("- ${tracking.adviceText}（${tracking.effectivenessScore}分）")
                }
        }
    }
    
    /**
     * 删除某条日报的所有追踪
     */
    suspend fun deleteTrackingsForEntry(entryId: Long) = withContext(ioDispatcher) {
        dao.deleteTrackingsForEntry(entryId)
    }
    
    /**
     * 清空所有追踪数据
     */
    suspend fun clearAll() = withContext(ioDispatcher) {
        dao.clearAllTrackings()
    }
    
    // ========== 辅助函数 ==========
    
    private fun Double.format(decimals: Int): String = "%.${decimals}f".format(this)
    
    private fun translateCategory(category: String): String = when (category) {
        AdviceCategory.SLEEP -> "睡眠"
        AdviceCategory.EXERCISE -> "运动"
        AdviceCategory.DIET -> "饮食"
        AdviceCategory.EMOTION -> "情绪"
        AdviceCategory.SYMPTOM -> "症状"
        else -> "其他"
    }
}

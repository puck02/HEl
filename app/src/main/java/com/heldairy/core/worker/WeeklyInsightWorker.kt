package com.heldairy.core.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.heldairy.HElDairyApplication
import com.heldairy.core.data.InsightRepository
import com.heldairy.core.data.WeeklyInsightCoordinator
import com.heldairy.core.util.Constants
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 每周 Insights 自动生成后台任务
 * 
 * 在每周日午夜后执行，自动为上一周生成健康趋势分析报告。
 * 使用 WorkManager 确保任务可靠执行（即使应用未运行）。
 * 
 * 执行条件：
 * - 网络连接可用
 * - AI 功能已启用
 * - 上周 Insights 尚未生成
 */
class WeeklyInsightWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val app = context.applicationContext as HElDairyApplication
    private val coordinator: WeeklyInsightCoordinator = app.appContainer.weeklyInsightCoordinator
    private val repository: InsightRepository = app.appContainer.insightRepository

    override suspend fun doWork(): Result {
        Log.i(TAG, "WeeklyInsightWorker started")

        return try {
            // 计算本周的周一日期（如果今天是周日，计算本周周一；否则计算上周周一）
            val today = LocalDate.now()
            val currentMonday = if (today.dayOfWeek == DayOfWeek.SUNDAY) {
                // 今天是周日，计算本周周一（6天前）
                today.minusDays(6)
            } else {
                // 其他日期，计算当前周的周一
                today.with(DayOfWeek.MONDAY)
            }

            Log.i(TAG, "Calculating insight for week starting: $currentMonday (today: $today, day: ${today.dayOfWeek})")

            // 检查是否已生成
            val existing = repository.getInsightForWeek(currentMonday.toString())
            if (existing != null) {
                Log.i(TAG, "Insight for week $currentMonday already exists, skipping")
                return Result.success()
            }

            // 触发生成（协调器内部会检查 AI 是否启用）
            val result = coordinator.getWeeklyInsight(force = true)

            if (result.status == com.heldairy.core.data.WeeklyInsightStatus.Success) {
                Log.i(TAG, "Successfully generated weekly insight for $currentMonday")
                Result.success()
            } else {
                Log.w(TAG, "Failed to generate weekly insight: ${result.message}")
                // 失败时重试（最多3次）
                if (runAttemptCount < Constants.Worker.MAX_RETRY_ATTEMPTS) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "WeeklyInsightWorker failed", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val TAG = "WeeklyInsightWorker"
        const val WORK_NAME = "weekly_insight_generation"
    }
}

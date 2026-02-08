package com.heldairy.core.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.heldairy.HElDairyApplication
import com.heldairy.core.data.InsightRepository
import java.time.LocalDate

/**
 * 旧数据清理后台任务
 * 
 * 定期删除 90 天以前的 Insight 报告，释放存储空间。
 * 不影响日报原始数据（daily_entries、question_responses）。
 * 
 * 执行频率：每月 1 号执行
 * 保留策略：只保留最近 90 天的 Insights
 */
class DataCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val app = context.applicationContext as HElDairyApplication
    private val insightRepository: InsightRepository = app.appContainer.insightRepository

    override suspend fun doWork(): Result {
        Log.i(TAG, "DataCleanupWorker started")

        return try {
            // 计算 90 天前的日期
            val cutoffDate = LocalDate.now().minusDays(RETENTION_DAYS)
            val cutoffString = cutoffDate.toString()

            // 删除旧的 Insights（数据量通常不大，直接执行）
            val deletedCount = insightRepository.deleteInsightsBefore(cutoffString)

            Log.i(TAG, "Cleaned up $deletedCount old insight reports (before $cutoffString)")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "DataCleanupWorker failed", e)
            // 清理任务失败不致命，下次再试
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "DataCleanupWorker"
        private const val RETENTION_DAYS = 90L
        const val WORK_NAME = "data_cleanup"
    }
}

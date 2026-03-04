package com.heldairy.core.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.heldairy.HElDairyApplication

/**
 * 后台数据同步 Worker
 *
 * 定期将 Room 数据增量上传到 hel-agent 服务器。
 * 执行条件：网络可用 + Agent 已登录 + 同步开关开。
 */
class DataSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val app = context.applicationContext as HElDairyApplication

    override suspend fun doWork(): Result {
        Log.i(TAG, "DataSyncWorker started")

        val syncManager = app.appContainer.dataSyncManager
            ?: run {
                Log.w(TAG, "DataSyncManager not available")
                return Result.success() // 非关键任务，不重试
            }

        return try {
            val success = syncManager.syncNow()
            if (success) {
                Log.i(TAG, "Sync completed successfully")
                Result.success()
            } else {
                Log.w(TAG, "Sync returned false, scheduling retry")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed with exception: ${e.message}", e)
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Log.e(TAG, "Max retries reached, giving up")
                Result.failure()
            }
        }
    }

    companion object {
        const val TAG = "DataSyncWorker"
        const val WORK_NAME = "data_sync_periodic"
        const val ONE_TIME_WORK_NAME = "data_sync_once"
        private const val MAX_RETRY_ATTEMPTS = 3
    }
}

package com.heldairy.core.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.heldairy.core.util.Constants
import java.time.Duration
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

/**
 * WorkManager 任务调度管理器
 * 
 * 负责注册和管理应用的所有后台周期性任务：
 * 1. 每周 Insights 自动生成（周日 01:00）
 * 2. 旧数据自动清理（每月 1 号 02:00）
 * 
 * 初始化时机：Application.onCreate()
 */
object WorkScheduler {

    /**
     * 初始化所有后台任务调度
     * 
     * 应在 Application.onCreate() 中调用一次。
     * WorkManager 会自动处理应用重启、设备重启后的任务恢复。
     */
    fun initialize(context: Context) {
        scheduleWeeklyInsightGeneration(context)
        scheduleDataCleanup(context)
    }

    /**
     * 调度每周 Insights 自动生成任务
     * 
     * 执行时间：每周日 01:00（避开用户高峰使用时段）
     * 执行条件：需要网络连接
     */
    private fun scheduleWeeklyInsightGeneration(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // 需要联网（调用 AI API）
            .setRequiresBatteryNotLow(true) // 电池电量充足
            .build()

        // 计算初始延迟：距离下一个周日 01:00 的时间
        val initialDelay = calculateDelayUntilNextSunday()

        val workRequest = PeriodicWorkRequestBuilder<WeeklyInsightWorker>(
            repeatInterval = Constants.Worker.WEEKLY_INTERVAL_DAYS, // 每 7 天执行一次
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelay.toMillis(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WeeklyInsightWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // 如果已存在，保留现有任务
            workRequest
        )
    }

    /**
     * 调度数据清理任务
     * 
     * 执行时间：每月 1 号 02:00
     * 执行条件：无网络要求（本地操作）
     */
    private fun scheduleDataCleanup(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        // 计算初始延迟：距离下个月 1 号 02:00 的时间
        val initialDelay = calculateDelayUntilNextFirstOfMonth()

        val workRequest = PeriodicWorkRequestBuilder<DataCleanupWorker>(
            repeatInterval = Constants.Worker.CLEANUP_INTERVAL_DAYS, // 每 30 天执行一次（近似月周期）
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelay.toMillis(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DataCleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * 取消所有后台任务
     * 
     * 用于用户关闭相关功能或应用卸载时清理。
     */
    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).apply {
            cancelUniqueWork(WeeklyInsightWorker.WORK_NAME)
            cancelUniqueWork(DataCleanupWorker.WORK_NAME)
        }
    }

    /**
     * 计算到下一个周日 01:00 的延迟时间
     * 如果今天是周日，则设置短延迟立即执行
     */
    private fun calculateDelayUntilNextSunday(): Duration {
        val now = LocalDateTime.now()
        
        // 如果今天是周日，立即执行（延迟5分钟以确保应用完成初始化）
        if (now.dayOfWeek == DayOfWeek.SUNDAY) {
            return Duration.ofMinutes(5)
        }
        
        // 计算下一个周日 01:00
        val nextSunday = now.with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
            .with(LocalTime.of(Constants.Worker.WEEKLY_INSIGHT_HOUR, 0, 0))

        return Duration.between(now, nextSunday)
    }

    /**
     * 计算到下个月 1 号 02:00 的延迟时间
     */
    private fun calculateDelayUntilNextFirstOfMonth(): Duration {
        val now = LocalDateTime.now()
        val firstOfNextMonth = now.plusMonths(1)
            .withDayOfMonth(1)
            .with(LocalTime.of(Constants.Worker.DATA_CLEANUP_HOUR, 0, 0)) // 凌晨 2 点

        return Duration.between(now, firstOfNextMonth)
    }
}

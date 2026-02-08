package com.heldairy.core.util

/**
 * 应用全局常量集中管理
 *
 * 将散布在各模块中的魔法数字统一定义在此，便于维护与调整。
 * 按职责划分为 Network / Worker / AI 三个子对象。
 */
object Constants {

    /* ── 网络超时与重试 ─────────────────────────────────── */
    object Network {
        /** OkHttp 连接超时 (秒) */
        const val CONNECT_TIMEOUT_SECONDS = 30L
        /** OkHttp 读超时 (秒) */
        const val READ_TIMEOUT_SECONDS = 60L
        /** OkHttp 写超时 (秒) */
        const val WRITE_TIMEOUT_SECONDS = 60L
        /** OkHttp 整体调用超时 (秒) */
        const val CALL_TIMEOUT_SECONDS = 90L
        /** 默认最大重试次数 */
        const val RETRY_MAX_ATTEMPTS = 3
        /** 首次重试基础延迟 (毫秒) */
        const val RETRY_BASE_DELAY_MS = 1000L
        /** 重试最大延迟上限 (毫秒) */
        const val RETRY_MAX_DELAY_MS = 8000L
        /** DeepSeek API Base URL */
        const val DEEPSEEK_BASE_URL = "https://api.deepseek.com/"
    }

    /* ── WorkManager 调度 ────────────────────────────────── */
    object Worker {
        /** 每周 Insight 生成间隔 (天) */
        const val WEEKLY_INTERVAL_DAYS = 7L
        /** 数据清理间隔 (天) */
        const val CLEANUP_INTERVAL_DAYS = 30L
        /** 每周 Insight 生成触发时间 (时) */
        const val WEEKLY_INSIGHT_HOUR = 1
        /** 数据清理触发时间 (时) */
        const val DATA_CLEANUP_HOUR = 2
        /** Worker 最大重试次数 */
        const val MAX_RETRY_ATTEMPTS = 3
        /** Insights 数据保留天数 */
        const val INSIGHT_RETENTION_DAYS = 90L
    }

    /* ── AI 模型与查询 ───────────────────────────────────── */
    object Ai {
        /** 默认模型名称 */
        const val DEFAULT_MODEL = "deepseek-chat"
        /** 补充追问最大题数 */
        const val MAX_FOLLOW_UP_QUESTIONS = 2
        /** Advice payload 红旗条目上限 */
        const val MAX_RED_FLAGS = 3
        /** Advice payload 观察条目上限 */
        const val MAX_OBSERVATIONS = 3
        /** Advice payload 行动条目上限 */
        const val MAX_ACTIONS = 3
        /** Advice payload 明日聚焦上限 */
        const val MAX_TOMORROW_FOCUS = 2
    }

    /* ── 数据查询窗口 ────────────────────────────────────── */
    object Data {
        /** 增强分析回溯天数 */
        const val ENHANCED_ANALYSIS_LOOKBACK = 14
        /** 摘要计算回溯条目数 */
        const val SUMMARY_LOOKBACK_ENTRIES = 30
        /** Insight 计算回溯条目数 */
        const val INSIGHT_LOOKBACK_ENTRIES = 31
        /** 执行跟踪回溯天数 */
        const val TRACKING_LOOKBACK_DAYS = 7L
        /** 反馈查询条数 */
        const val FEEDBACK_QUERY_LIMIT = 10
        /** 有效性查询条数 */
        const val EFFECTIVENESS_QUERY_LIMIT = 20
    }
}

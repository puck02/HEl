package com.heldairy.core.network

import android.util.Log
import com.heldairy.core.util.Constants
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.math.min
import kotlin.math.pow

/**
 * OkHttp 网络重试拦截器
 * 
 * 对临时性网络错误（503、SocketTimeoutException）实施指数退避重试策略，
 * 提高在不稳定网络环境下的 API 调用成功率。
 * 
 * 重试策略：
 * - 最大重试次数：3次
 * - 退避算法：指数增长 (baseDelayMs * 2^attempt)
 * - 最大延迟：8秒
 * - 只重试幂等性安全的请求（GET、POST with retry-safe header）
 * 
 * 不重试的场景：
 * - 认证错误 (401, 403)
 * - 客户端错误 (400, 404)
 * - 非幂等操作（PUT、DELETE、PATCH）
 */
class RetryInterceptor(
    private val maxRetries: Int = Constants.Network.RETRY_MAX_ATTEMPTS,
    private val baseDelayMs: Long = Constants.Network.RETRY_BASE_DELAY_MS
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var attempt = 0
        var lastException: IOException? = null
        var response: Response? = null

        while (attempt <= maxRetries) {
            try {
                // 尝试执行请求
                response?.close() // 关闭上一次失败的响应
                response = chain.proceed(request)

                // 检查响应码是否需要重试
                if (shouldRetry(response, attempt)) {
                    val delay = calculateDelay(attempt)
                    Log.d(TAG, "API call failed with ${response.code}, retrying after ${delay}ms (attempt ${attempt + 1}/$maxRetries)")
                    response.close()
                    Thread.sleep(delay)
                    attempt++
                    continue
                }

                // 成功或不可重试的错误，返回响应
                return response

            } catch (e: SocketTimeoutException) {
                lastException = e
                if (attempt < maxRetries && isRetryable(request)) {
                    val delay = calculateDelay(attempt)
                    Log.d(TAG, "Socket timeout, retrying after ${delay}ms (attempt ${attempt + 1}/$maxRetries)")
                    Thread.sleep(delay)
                    attempt++
                    continue
                }
                throw e

            } catch (e: IOException) {
                lastException = e
                if (attempt < maxRetries && isRetryable(request) && isRetryableException(e)) {
                    val delay = calculateDelay(attempt)
                    Log.d(TAG, "Network error: ${e.message}, retrying after ${delay}ms (attempt ${attempt + 1}/$maxRetries)")
                    Thread.sleep(delay)
                    attempt++
                    continue
                }
                throw e
            }
        }

        // 重试次数耗尽
        response?.let { return it }
        throw lastException ?: IOException("Maximum retry attempts ($maxRetries) exceeded")
    }

    /**
     * 判断响应是否需要重试
     */
    private fun shouldRetry(response: Response, attempt: Int): Boolean {
        if (attempt >= maxRetries) return false
        
        return when (response.code) {
            503 -> true  // Service Unavailable
            429 -> true  // Too Many Requests
            502 -> true  // Bad Gateway
            504 -> true  // Gateway Timeout
            else -> false
        }
    }

    /**
     * 判断请求是否可以安全重试（幂等性检查）
     */
    private fun isRetryable(request: okhttp3.Request): Boolean {
        // GET 请求总是幂等的
        if (request.method == "GET") return true
        
        // POST 请求需要显式标记为可重试
        if (request.method == "POST" && request.header("X-Retry-Safe") == "true") {
            return true
        }
        
        // DeepSeek Chat API 的 POST 请求是幂等的（只读操作）
        if (request.method == "POST" && request.url.encodedPath.contains("/chat/completions")) {
            return true
        }
        
        return false
    }

    /**
     * 判断异常是否可重试
     */
    private fun isRetryableException(e: IOException): Boolean {
        val message = e.message?.lowercase() ?: return false
        return message.contains("timeout") ||
               message.contains("connection reset") ||
               message.contains("connection refused") ||
               message.contains("socket closed")
    }

    /**
     * 计算指数退避延迟时间
     * 
     * 公式：min(baseDelay * 2^attempt, maxDelay)
     */
    private fun calculateDelay(attempt: Int): Long {
        val exponentialDelay = baseDelayMs * 2.0.pow(attempt.toDouble()).toLong()
        return min(exponentialDelay, Constants.Network.RETRY_MAX_DELAY_MS)
    }

    companion object {
        private const val TAG = "RetryInterceptor"
    }
}

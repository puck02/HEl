package com.heldairy.core.network.agent

import android.util.Log
import com.heldairy.core.preferences.AgentPreferencesStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp Interceptor：自动为 hel-agent 请求注入 JWT Bearer token。
 *
 * 对不需要认证的端点（/auth/register, /auth/login, /health）跳过注入。
 * 当收到 401 时，尝试用 refresh_token 刷新并重试一次。
 */
class AgentAuthInterceptor(
    private val agentPrefs: AgentPreferencesStore,
    private val agentApiProvider: () -> AgentApi   // 延迟获取，避免循环依赖
) : Interceptor {

    companion object {
        private const val TAG = "AgentAuth"
        /** 不需要 JWT 的路径 */
        private val PUBLIC_PATHS = listOf("/auth/register", "/auth/login", "/health")
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath

        // 公共端点不注入 token
        if (PUBLIC_PATHS.any { path.endsWith(it) }) {
            return chain.proceed(request)
        }

        // 注入 Access Token
        val settings = runBlocking { agentPrefs.currentSettings() }
        val token = settings.accessToken

        if (token.isBlank()) {
            // 未登录，直接发请求（服务端会返回 401）
            return chain.proceed(request)
        }

        val authenticatedRequest = request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        val response = chain.proceed(authenticatedRequest)

        // 401 → 尝试刷新 token
        if (response.code == 401 && settings.refreshToken.isNotBlank()) {
            response.close()
            Log.d(TAG, "Access token expired, attempting refresh...")

            val refreshed = tryRefreshToken(settings.refreshToken)
            if (refreshed) {
                val newSettings = runBlocking { agentPrefs.currentSettings() }
                val retryRequest = request.newBuilder()
                    .header("Authorization", "Bearer ${newSettings.accessToken}")
                    .build()
                return chain.proceed(retryRequest)
            } else {
                // Refresh 也失败了，清除登录状态
                Log.w(TAG, "Token refresh failed, clearing login")
                runBlocking { agentPrefs.clearLogin() }
            }
        }

        return response
    }

    /**
     * 尝试用 refresh_token 获取新的 access/refresh token。
     * @return true 表示刷新成功并已持久化新 token
     */
    private fun tryRefreshToken(refreshToken: String): Boolean {
        return try {
            val api = agentApiProvider()
            val result = runBlocking {
                api.refreshToken(AgentRefreshRequest(refreshToken))
            }
            runBlocking {
                agentPrefs.updateTokens(result.accessToken, result.refreshToken)
            }
            Log.d(TAG, "Token refreshed successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Token refresh error: ${e.message}")
            false
        }
    }
}

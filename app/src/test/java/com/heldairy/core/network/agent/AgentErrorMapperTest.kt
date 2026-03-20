package com.heldairy.core.network.agent

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class AgentErrorMapperTest {
    @Test
    fun mapChatHttpError_extractsDetailFrom500Body() {
        val error = AgentClientErrorMapper.mapChatHttpError(500, "{\"detail\":\"数据库暂时不可用，请稍后重试\"}")

        assertEquals("数据库暂时不可用，请稍后重试", error.message)
    }

    @Test
    fun toUserMessage_returnsFallbackWhenThrowableMessageBlank() {
        val noMessageThrowable = Throwable()

        val msg = AgentClientErrorMapper.toUserMessage(noMessageThrowable, "聊天请求失败，请稍后重试")

        assertEquals("聊天请求失败，请稍后重试", msg)
    }

    @Test
    fun toUserMessage_returnsFallbackWhenThrowableMessageUnknown() {
        val unknownMessageThrowable = Throwable("未知错误")

        val msg = AgentClientErrorMapper.toUserMessage(unknownMessageThrowable, "聊天请求失败，请稍后重试")

        assertEquals("聊天请求失败，请稍后重试", msg)
    }

    @Test
    fun mapAuthThrowable_mapsUsernameConflictToFriendlyMessage() {
        val httpException = httpException(
            code = 409,
            body = "{\"detail\":\"Username already exists\"}"
        )

        val mapped = AgentClientErrorMapper.mapAuthThrowable(httpException)

        assertTrue(mapped is IllegalStateException)
        assertEquals("用户名已存在，请直接登录", mapped.message)
    }

    private fun httpException(code: Int, body: String): HttpException {
        val responseBody = body.toResponseBody("application/json".toMediaType())
        val response = Response.error<Any>(code, responseBody)
        return HttpException(response)
    }
}

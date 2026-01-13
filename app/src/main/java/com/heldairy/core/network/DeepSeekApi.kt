package com.heldairy.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface DeepSeekApi {
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authHeader: String,
        @Body request: DeepSeekRequest
    ): DeepSeekResponse
}

@Serializable
data class DeepSeekRequest(
    val model: String,
    val messages: List<DeepSeekMessage>,
    @SerialName("response_format") val responseFormat: ResponseFormat = ResponseFormat()
)

@Serializable
data class DeepSeekMessage(
    val role: String,
    val content: String
)

@Serializable
data class ResponseFormat(
    val type: String = "json_object"
)

@Serializable
data class DeepSeekResponse(
    val choices: List<DeepSeekChoice>
)

@Serializable
data class DeepSeekChoice(
    val message: DeepSeekMessage
)

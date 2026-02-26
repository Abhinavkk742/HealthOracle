package com.healthoracle.data.remote

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class AiRequest(
    val model: String = "gpt-3.5-turbo",
    val messages: List<AiMessage>,
    val max_tokens: Int = 1000
)

data class AiMessage(
    val role: String,
    val content: String
)

data class AiResponse(
    val choices: List<AiChoice>
)

data class AiChoice(
    val message: AiMessage
)

interface AiApiService {
    @POST("v1/chat/completions")
    suspend fun getHealthSuggestions(
        @Header("Authorization") apiKey: String,
        @Body request: AiRequest
    ): AiResponse
}

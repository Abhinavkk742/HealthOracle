package com.healthoracle.data.remote

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

// Gemini Request Models
data class GeminiRequest(
    val contents: List<GeminiContent>
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String
)

// Gemini Response Models
data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContent?
)

interface AiApiService {
    // Using @Url bypasses Base URL issues and stops Retrofit from breaking the colon (:)
    @POST
    suspend fun getHealthSuggestions(
        @Url fullUrl: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}
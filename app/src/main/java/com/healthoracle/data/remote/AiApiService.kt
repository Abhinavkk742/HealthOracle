package com.healthoracle.data.remote

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

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
    // Using Gemini 1.5 Flash for fast, efficient text generation
    @POST("v1beta/models/gemini-1.5-flash:generateContent")
    suspend fun getHealthSuggestions(
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}
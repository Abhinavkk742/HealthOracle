package com.healthoracle.domain.usecase

import com.healthoracle.BuildConfig
import com.healthoracle.data.remote.AiApiService
import com.healthoracle.data.remote.GeminiContent
import com.healthoracle.data.remote.GeminiPart
import com.healthoracle.data.remote.GeminiRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetAiSuggestionsUseCase @Inject constructor(
    private val aiApiService: AiApiService
) {
    operator fun invoke(conditionName: String, conditionSource: String): Flow<Result<String>> = flow {
        try {
            // Combining system instructions and user prompt for Gemini
            val prompt = """
                You are a helpful medical AI assistant. Always include a disclaimer that you are an AI and the user should consult a real doctor for medical advice.
                
                The user has been indicated with the following health condition: $conditionName (Source: $conditionSource). 
                Please create a personalized daily health timetable for them. Include dietary recommendations, recommended activities or lifestyle changes, and any specific precautions they should take. 
                Format the response clearly with headings and bullet points.
            """.trimIndent()

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = prompt))
                    )
                )
            )

            // Injecting the secure API key from BuildConfig
            val response = aiApiService.getHealthSuggestions(
                apiKey = BuildConfig.AI_API_KEY,
                request = request
            )

            // Parse the Gemini response structure safely
            val advice = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "No suggestions available at the moment. Please try again later."

            emit(Result.success(advice))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
package com.healthoracle.domain.usecase

import com.google.ai.client.generativeai.GenerativeModel
import com.healthoracle.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetAiSuggestionsUseCase @Inject constructor() {
    operator fun invoke(conditionName: String, conditionSource: String): Flow<Result<String>> = flow {
        try {
            val prompt = """
                You are a helpful medical AI assistant. Always include a disclaimer that you are an AI and the user should consult a real doctor for medical advice.
                
                The user has been indicated with the following health condition: $conditionName (Source: $conditionSource). 
                Please create a personalized daily health timetable for them. Include dietary recommendations, recommended activities or lifestyle changes, and any specific precautions they should take. 
                Format the response clearly with headings and bullet points.
            """.trimIndent()

            // ✅ Updated to the current, active model: gemini-2.5-flash
            val generativeModel = GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = BuildConfig.AI_API_KEY
            )

            // Direct call using the official SDK
            val response = generativeModel.generateContent(prompt)

            val advice = response.text ?: "No suggestions available at the moment. Please try again later."

            emit(Result.success(advice))
        } catch (e: Exception) {
            e.printStackTrace()
            emit(Result.failure(e))
        }
    }
}
package com.healthoracle.domain.usecase

import com.google.ai.client.generativeai.GenerativeModel
import com.healthoracle.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetAiSuggestionsUseCase @Inject constructor() {
    operator fun invoke(conditionName: String, conditionSource: String): Flow<Result<String>> = flow {
        try {
            // Highly structured prompt to prevent messy markdown and enforce a beautiful layout
            val prompt = """
                You are an empathetic and professional medical AI assistant.
                The user has received the following health indication: $conditionName (Source: $conditionSource). 
                
                Please create a highly organized, easy-to-read personalized daily health timetable and advice guide.
                
                CRITICAL FORMATTING RULES:
                1. DO NOT use markdown characters like ** or *. 
                2. Use ALL CAPS for section headers.
                3. Use appropriate emojis for headers and bullet points.
                4. Use standard dashes (-) for list items.
                5. Add a blank line between every single section for readability.
                
                Please structure your response exactly with these sections:
                🌅 MORNING ROUTINE
                ☀️ AFTERNOON ROUTINE
                🌙 EVENING ROUTINE
                🥗 DIETARY RECOMMENDATIONS
                ⚠️ PRECAUTIONS & LIFESTYLE CHANGES
                
                Always end with a disclaimer that you are an AI and the user must consult a real doctor for medical advice.
            """.trimIndent()

            val generativeModel = GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = BuildConfig.AI_API_KEY
            )

            val response = generativeModel.generateContent(prompt)

            val advice = response.text ?: "No suggestions available at the moment. Please try again later."

            emit(Result.success(advice))
        } catch (e: Exception) {
            e.printStackTrace()
            emit(Result.failure(e))
        }
    }
}
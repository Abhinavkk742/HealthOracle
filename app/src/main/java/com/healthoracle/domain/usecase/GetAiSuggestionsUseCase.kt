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
                You are an empathetic and professional medical AI assistant.
                The user has received the following health indication: $conditionName (Source: $conditionSource). 
                
                Please create a highly organized, easy-to-read personalized daily health timetable and advice guide.
                
                CRITICAL FORMATTING RULES:
                1. DO NOT use markdown characters like ** or *. 
                2. Use ALL CAPS for section headers.
                3. Use appropriate emojis for headers only.
                4. For EVERY SINGLE ITEM in the MORNING, AFTERNOON, and EVENING routines, you MUST start the line with a standard dash, followed exactly by the time in HH:MM AM/PM format, then a colon, then the task. 
                   Example: - 07:00 AM: Go for a 30-minute run.
                   Example: - 08:30 AM: Eat a healthy breakfast (Oatmeal).
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
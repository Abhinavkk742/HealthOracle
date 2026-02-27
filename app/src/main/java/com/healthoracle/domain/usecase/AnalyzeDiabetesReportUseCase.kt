package com.healthoracle.domain.usecase

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.healthoracle.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class AnalyzeDiabetesReportUseCase @Inject constructor() {
    operator fun invoke(bitmap: Bitmap): Flow<Result<String>> = flow {
        try {
            val generativeModel = GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = BuildConfig.AI_API_KEY
            )

            val prompt = """
                You are an expert medical AI. Analyze this uploaded medical report/blood test.
                Please carefully extract any key indicators related to diabetes (such as Fasting Blood Sugar, HbA1c, Glucose levels, etc.).
                
                Format your response cleanly:
                🔍 KEY FINDINGS (List the exact extracted metrics)
                📊 RISK ASSESSMENT (State if the numbers indicate Low, Pre-diabetic, or High risk)
                💡 RECOMMENDATIONS (Brief advice based on these specific numbers)
                
                CRITICAL RULES:
                1. DO NOT use markdown characters like **. 
                2. Use ALL CAPS for headers.
                3. If the image is not a medical report, politely ask the user to upload a valid report.
                4. Always include a disclaimer that you are an AI and the user should consult a real doctor.
            """.trimIndent()

            // Passing BOTH the image and the text prompt to Gemini!
            val response = generativeModel.generateContent(
                content {
                    image(bitmap)
                    text(prompt)
                }
            )

            val advice = response.text ?: "Could not extract data from the report. Please try again with a clearer image."
            emit(Result.success(advice))
        } catch (e: Exception) {
            e.printStackTrace()
            emit(Result.failure(e))
        }
    }
}
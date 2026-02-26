package com.healthoracle.data.local

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

data class DiabetesResult(
    val isDiabetic: Boolean,
    val confidence: Float,
    val riskLevel: String
)

@Singleton
class DiabetesClassifier @Inject constructor(
    private val context: Context
) {
    private var interpreter: Interpreter? = null
    private var scalerMean: FloatArray = FloatArray(21)
    private var scalerScale: FloatArray = FloatArray(21)

    fun initialize() {
        try {
            val model = FileUtil.loadMappedFile(context, "diabetes_model.tflite")
            val options = Interpreter.Options().apply {
                numThreads = 4
                useNNAPI = false
                useXNNPACK = true
            }
            interpreter = Interpreter(model, options)
            scalerMean  = loadFloatArray("scaler_mean.txt")
            scalerScale = loadFloatArray("scaler_scale.txt")
        } catch (e: Exception) {
            throw IllegalStateException("Failed to initialize DiabetesClassifier: ${e.message}", e)
        }
    }

    private fun loadFloatArray(fileName: String): FloatArray {
        val reader = BufferedReader(InputStreamReader(context.assets.open(fileName)))
        val values = mutableListOf<Float>()
        var line = reader.readLine()
        while (line != null) {
            line.trim().toFloatOrNull()?.let { values.add(it) }
            line = reader.readLine()
        }
        reader.close()
        return values.toFloatArray()
    }

    fun predict(input: FloatArray): DiabetesResult {
        val interp = interpreter
            ?: throw IllegalStateException("Classifier not initialized. Call initialize() first.")

        // Normalize using saved scaler
        val scaled = FloatArray(21) { i ->
            (input[i] - scalerMean[i]) / scalerScale[i]
        }

        // Run inference
        val inputBuffer  = Array(1) { scaled }
        val outputBuffer = Array(1) { FloatArray(1) }
        interp.run(inputBuffer, outputBuffer)

        val score = outputBuffer[0][0]
        val isDiabetic = score >= 0.5f
        val confidence = if (isDiabetic) score else 1f - score
        val riskLevel = when {
            score < 0.3f -> "Low Risk"
            score < 0.5f -> "Moderate Risk"
            score < 0.7f -> "High Risk"
            else         -> "Very High Risk"
        }

        return DiabetesResult(isDiabetic, confidence, riskLevel)
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}

package com.healthoracle.data.local

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SkinDiseaseClassifier @Inject constructor(
    private val context: Context
) {
    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private val inputImageSize = 224

    fun initialize() {
        try {
            val model = FileUtil.loadMappedFile(context, "skin_disease_model.tflite")
            val options = Interpreter.Options().apply {
                numThreads = 4
                useNNAPI = false  // Disable NNAPI to avoid version conflicts
                useXNNPACK = true
            }
            interpreter = Interpreter(model, options)
            labels = loadLabels()
        } catch (e: Exception) {
            throw IllegalStateException("Failed to initialize classifier: ${e.message}", e)
        }
    }

    private fun loadLabels(): List<String> {
        return try {
            val reader = BufferedReader(
                InputStreamReader(context.assets.open("skin_disease_labels.txt"))
            )
            val result = mutableListOf<String>()
            var line = reader.readLine()
            while (line != null) {
                if (line.trim().isNotEmpty()) result.add(line.trim())
                line = reader.readLine()
            }
            reader.close()
            result
        } catch (e: Exception) {
            listOf(
                "Acne", "Atopic Dermatitis", "Basal Cell Carcinoma",
                "Eczema", "Melanoma", "Melanocytic Nevi",
                "Nail Fungus", "Normal Skin", "Pigmented Benign Keratosis",
                "Psoriasis", "Ringworm", "Rosacea",
                "Seborrheic Keratosis", "Squamous Cell Carcinoma", "Vascular Lesion"
            )
        }
    }

    fun classify(bitmap: Bitmap): List<Pair<String, Float>> {
        val interp = interpreter
            ?: throw IllegalStateException("Classifier not initialized. Call initialize() first.")

        // Get input tensor shape from model
        val inputShape = interp.getInputTensor(0).shape()
        val inputH = inputShape[1]
        val inputW = inputShape[2]

        // Preprocess image
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputH, inputW, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(127.5f, 127.5f)) // MobileNetV2 preprocessing
            .build()

        var tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        // Prepare output buffer
        val outputShape = interp.getOutputTensor(0).shape()
        val outputBuffer = TensorBuffer.createFixedSize(outputShape, DataType.FLOAT32)

        // Run inference
        interp.run(tensorImage.buffer, outputBuffer.buffer.rewind())

        // Map to labels
        val scores = outputBuffer.floatArray
        return labels.mapIndexed { index, label ->
            if (index < scores.size) Pair(label, scores[index])
            else Pair(label, 0f)
        }.sortedByDescending { it.second }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}

package com.healthoracle.data.repository

import android.graphics.Bitmap
import com.healthoracle.core.util.Resource
import com.healthoracle.data.local.SkinDiseaseClassifier
import com.healthoracle.data.model.PredictionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SkinDiseaseRepository @Inject constructor(
    private val classifier: SkinDiseaseClassifier
) {
    suspend fun classifyImage(bitmap: Bitmap): Resource<PredictionResult> {
        return withContext(Dispatchers.IO) {
            try {
                classifier.initialize()
                val results = classifier.classify(bitmap)
                if (results.isEmpty()) {
                    Resource.Error("No results returned from model")
                } else {
                    val topResult = results.first()
                    Resource.Success(
                        PredictionResult(
                            label = topResult.first,
                            confidence = topResult.second,
                            allResults = results.take(5) // top 5 results
                        )
                    )
                }
            } catch (e: Exception) {
                Resource.Error(
                    message = e.message ?: "Classification failed",
                    throwable = e
                )
            }
        }
    }
}

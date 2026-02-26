package com.healthoracle.data.repository

import com.healthoracle.data.local.DiabetesClassifier
import com.healthoracle.data.local.DiabetesResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiabetesRepository @Inject constructor(
    private val classifier: DiabetesClassifier
) {
    fun initialize() = classifier.initialize()

    fun predict(input: FloatArray): DiabetesResult = classifier.predict(input)

    fun close() = classifier.close()
}

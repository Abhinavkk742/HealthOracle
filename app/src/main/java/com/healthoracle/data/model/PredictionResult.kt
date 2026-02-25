package com.healthoracle.data.model

data class PredictionResult(
    val label: String,
    val confidence: Float,
    val allResults: List<Pair<String, Float>> = emptyList()
)

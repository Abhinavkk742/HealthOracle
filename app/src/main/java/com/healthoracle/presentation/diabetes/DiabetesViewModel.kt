package com.healthoracle.presentation.diabetes

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthoracle.data.local.DiabetesResult
import com.healthoracle.data.repository.DiabetesRepository
import com.healthoracle.domain.usecase.AnalyzeDiabetesReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiabetesUiState(
    // Original states for the manual slider
    val isLoading: Boolean = false,
    val result: DiabetesResult? = null,
    val error: String? = null,

    // New states for the AI report scanner
    val isReportLoading: Boolean = false,
    val reportResult: String? = null,
    val reportError: String? = null
)

@HiltViewModel
class DiabetesViewModel @Inject constructor(
    private val repository: DiabetesRepository, // Your original working repository
    private val analyzeDiabetesReportUseCase: AnalyzeDiabetesReportUseCase // The new AI use case
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiabetesUiState())
    val uiState: StateFlow<DiabetesUiState> = _uiState.asStateFlow()

    init {
        // Initialize your local TFLite model just like before
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.initialize()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Model init failed: ${e.message}")
            }
        }
    }

    // Manual Prediction using your original working logic
    fun predict(
        highBP: Float, highChol: Float, cholCheck: Float, bmi: Float,
        smoker: Float, stroke: Float, heartDisease: Float, physActivity: Float,
        fruits: Float, veggies: Float, hvyAlcohol: Float, anyHealthcare: Float,
        noDocCost: Float, genHlth: Float, mentHlth: Float, physHlth: Float,
        diffWalk: Float, sex: Float, age: Float, education: Float, income: Float
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val input = floatArrayOf(
                    highBP, highChol, cholCheck, bmi, smoker,
                    stroke, heartDisease, physActivity, fruits,
                    veggies, hvyAlcohol, anyHealthcare, noDocCost,
                    genHlth, mentHlth, physHlth, diffWalk, sex,
                    age, education, income
                )
                val result = repository.predict(input)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    result = result
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    // NEW: AI Report Analysis
    fun analyzeReport(bitmap: Bitmap) {
        _uiState.value = _uiState.value.copy(
            isReportLoading = true,
            reportResult = null,
            reportError = null
        )

        viewModelScope.launch {
            analyzeDiabetesReportUseCase(bitmap).collect { result ->
                result.fold(
                    onSuccess = { analysisText ->
                        _uiState.value = _uiState.value.copy(
                            isReportLoading = false,
                            reportResult = analysisText
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isReportLoading = false,
                            reportError = error.message ?: "Failed to analyze report."
                        )
                    }
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.close()
    }
}
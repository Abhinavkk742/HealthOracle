package com.healthoracle.presentation.diabetes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthoracle.data.local.DiabetesResult
import com.healthoracle.data.repository.DiabetesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DiabetesUiState(
    val isLoading: Boolean = false,
    val result: DiabetesResult? = null,
    val error: String? = null
)

@HiltViewModel
class DiabetesViewModel @Inject constructor(
    private val repository: DiabetesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiabetesUiState())
    val uiState: StateFlow<DiabetesUiState> = _uiState

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.initialize()
            } catch (e: Exception) {
                _uiState.value = DiabetesUiState(error = "Model init failed: ${e.message}")
            }
        }
    }

    fun predict(
        highBP: Float, highChol: Float, cholCheck: Float, bmi: Float,
        smoker: Float, stroke: Float, heartDisease: Float, physActivity: Float,
        fruits: Float, veggies: Float, hvyAlcohol: Float, anyHealthcare: Float,
        noDocCost: Float, genHlth: Float, mentHlth: Float, physHlth: Float,
        diffWalk: Float, sex: Float, age: Float, education: Float, income: Float
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = DiabetesUiState(isLoading = true)
            try {
                val input = floatArrayOf(
                    highBP, highChol, cholCheck, bmi, smoker,
                    stroke, heartDisease, physActivity, fruits,
                    veggies, hvyAlcohol, anyHealthcare, noDocCost,
                    genHlth, mentHlth, physHlth, diffWalk, sex,
                    age, education, income
                )
                val result = repository.predict(input)
                _uiState.value = DiabetesUiState(result = result)
            } catch (e: Exception) {
                _uiState.value = DiabetesUiState(error = e.message)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.close()
    }
}

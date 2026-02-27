package com.healthoracle.presentation.diabetes

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.healthoracle.data.local.DiabetesResult
import com.healthoracle.data.repository.DiabetesRepository
import com.healthoracle.domain.model.UserProfile
import com.healthoracle.domain.usecase.AnalyzeDiabetesReportUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class DiabetesUiState(
    val isLoading: Boolean = false,
    val result: DiabetesResult? = null,
    val error: String? = null,

    val isReportLoading: Boolean = false,
    val reportResult: String? = null,
    val reportError: String? = null,

    // NEW: Holds the loaded profile data
    val userProfile: UserProfile? = null
)

@HiltViewModel
class DiabetesViewModel @Inject constructor(
    private val repository: DiabetesRepository,
    private val analyzeDiabetesReportUseCase: AnalyzeDiabetesReportUseCase,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiabetesUiState())
    val uiState: StateFlow<DiabetesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.initialize()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Model init failed: ${e.message}")
            }
        }
        // Fetch the user's profile as soon as they open the Diabetes screen
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            try {
                val document = firestore.collection("users").document(userId).get().await()
                if (document.exists()) {
                    val profile = document.toObject(UserProfile::class.java)
                    _uiState.value = _uiState.value.copy(userProfile = profile)
                }
            } catch (e: Exception) {
                // If it fails, we just don't pre-fill. No need to crash the app.
                e.printStackTrace()
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
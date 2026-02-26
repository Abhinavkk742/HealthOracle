package com.healthoracle.presentation.skin

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthoracle.core.util.Resource
import com.healthoracle.data.model.PredictionResult
import com.healthoracle.domain.usecase.ClassifySkinImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SkinDiseaseUiState(
    val selectedBitmap: Bitmap? = null,
    val predictionResult: PredictionResult? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasResult: Boolean = false
)

@HiltViewModel
class SkinDiseaseViewModel @Inject constructor(
    private val classifySkinImageUseCase: ClassifySkinImageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SkinDiseaseUiState())
    val uiState: StateFlow<SkinDiseaseUiState> = _uiState.asStateFlow()

    fun onImageSelected(bitmap: Bitmap) {
        _uiState.update {
            it.copy(
                selectedBitmap = bitmap,
                hasResult = false,
                errorMessage = null,
                predictionResult = null
            )
        }
        classifyImage(bitmap)
    }

    private fun classifyImage(bitmap: Bitmap) {
        classifySkinImageUseCase(bitmap)
            .onEach { result ->
                when (result) {
                    is Resource.Loading -> {
                        _uiState.update {
                            it.copy(isLoading = true, errorMessage = null)
                        }
                    }
                    is Resource.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                predictionResult = result.data,
                                hasResult = true,
                                errorMessage = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.message
                            )
                        }
                    }
                    is Resource.Idle -> Unit
                }
            }
            .launchIn(viewModelScope)
    }

    fun clearResult() {
        _uiState.update {
            SkinDiseaseUiState()
        }
    }
}

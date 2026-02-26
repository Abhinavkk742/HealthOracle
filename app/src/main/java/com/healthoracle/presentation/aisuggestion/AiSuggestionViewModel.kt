package com.healthoracle.presentation.aisuggestion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthoracle.domain.usecase.GetAiSuggestionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiSuggestionViewModel @Inject constructor(
    private val getAiSuggestionsUseCase: GetAiSuggestionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AiSuggestionUiState>(AiSuggestionUiState.Initial)
    val uiState: StateFlow<AiSuggestionUiState> = _uiState.asStateFlow()

    fun fetchHealthTimetable(conditionName: String, conditionSource: String) {
        _uiState.value = AiSuggestionUiState.Loading

        viewModelScope.launch {
            getAiSuggestionsUseCase(conditionName, conditionSource).collect { result ->
                result.fold(
                    onSuccess = { advice ->
                        _uiState.value = AiSuggestionUiState.Success(advice)
                    },
                    onFailure = { error ->
                        _uiState.value = AiSuggestionUiState.Error(
                            error.message ?: "An unexpected error occurred while fetching your timetable."
                        )
                    }
                )
            }
        }
    }
}

// Represents the different states of our screen
sealed class AiSuggestionUiState {
    object Initial : AiSuggestionUiState()
    object Loading : AiSuggestionUiState()
    data class Success(val advice: String) : AiSuggestionUiState()
    data class Error(val message: String) : AiSuggestionUiState()
}
package com.healthoracle.presentation.aisuggestion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.healthoracle.data.model.HealthRecord
import com.healthoracle.domain.usecase.GetAiSuggestionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiSuggestionViewModel @Inject constructor(
    private val getAiSuggestionsUseCase: GetAiSuggestionsUseCase,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
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
                        // Save the plan in the background
                        saveToHistory(conditionName, advice)
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

    private fun saveToHistory(conditionName: String, advice: String) {
        val userId = auth.currentUser?.uid ?: return

        val record = HealthRecord(
            title = "Plan for: $conditionName",
            description = advice,
            type = "AI Plan"
        )

        firestore.collection("users").document(userId)
            .collection("history")
            .add(record)
    }
}

sealed class AiSuggestionUiState {
    data object Initial : AiSuggestionUiState()
    data object Loading : AiSuggestionUiState()
    data class Success(val advice: String) : AiSuggestionUiState()
    data class Error(val message: String) : AiSuggestionUiState()
}
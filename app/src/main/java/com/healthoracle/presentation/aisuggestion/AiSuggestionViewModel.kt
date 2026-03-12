package com.healthoracle.presentation.aisuggestion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.healthoracle.data.local.dao.AppointmentDao
import com.healthoracle.data.local.entity.AppointmentEntity
import com.healthoracle.data.model.HealthRecord
import com.healthoracle.domain.usecase.GetAiSuggestionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class AiSuggestionViewModel @Inject constructor(
    private val getAiSuggestionsUseCase: GetAiSuggestionsUseCase,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val appointmentDao: AppointmentDao
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

    fun addPlanToLocalCalendar(
        conditionName: String,
        morningAdvice: String,
        afternoonAdvice: String,
        eveningAdvice: String,
        onScheduleAlarm: (Int, String, String, LocalDate) -> Unit,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val startDate = LocalDate.now().plusDays(1)

            for (i in 0 until 30) {
                val date = startDate.plusDays(i.toLong())

                if (morningAdvice.isNotBlank()) {
                    processRoutineBlock(morningAdvice, date, conditionName, "8:00 AM", onScheduleAlarm)
                }

                if (afternoonAdvice.isNotBlank()) {
                    processRoutineBlock(afternoonAdvice, date, conditionName, "1:00 PM", onScheduleAlarm)
                }

                if (eveningAdvice.isNotBlank()) {
                    processRoutineBlock(eveningAdvice, date, conditionName, "7:00 PM", onScheduleAlarm)
                }
            }
            onComplete()
        }
    }

    private suspend fun processRoutineBlock(
        block: String,
        date: LocalDate,
        conditionName: String,
        defaultTime: String,
        onScheduleAlarm: (Int, String, String, LocalDate) -> Unit
    ) {
        val lines = block.split("\n")
            .map { it.trim() }
            .filter { it.startsWith("-") }

        if (lines.isEmpty()) {
            val title = "$conditionName Routine"
            val cleanDesc = block.substringAfter("\n").trim()
            if (cleanDesc.isNotBlank()) {
                appointmentDao.insertAppointment(
                    AppointmentEntity(title = title, time = defaultTime, date = date.toString(), category = "Routine", description = cleanDesc)
                )
                val alarmId = (title + defaultTime + date.toString()).hashCode()
                onScheduleAlarm(alarmId, title, defaultTime, date)
            }
            return
        }

        val timeRegex = Regex("""\b(1[0-2]|0?[1-9]):([0-5][0-9])\s*([AaPp][Mm])\b""", RegexOption.IGNORE_CASE)

        lines.forEach { line ->
            val match = timeRegex.find(line)
            val extractedTime = match?.value?.uppercase() ?: defaultTime

            var cleanText = line.removePrefix("-").trim()
            if (match != null) {
                cleanText = cleanText.replaceFirst(match.value, "").trim()
            }
            cleanText = cleanText.removePrefix(":").removePrefix("-").trim()

            val title = if (cleanText.length > 30) cleanText.take(27) + "..." else cleanText
            val description = line.removePrefix("-").trim()

            appointmentDao.insertAppointment(
                AppointmentEntity(
                    title = title,
                    time = extractedTime,
                    date = date.toString(),
                    category = "Routine", // Formally tags AI generated tasks as routines
                    description = description
                )
            )

            val alarmId = (title + extractedTime + date.toString()).hashCode()
            onScheduleAlarm(alarmId, title, extractedTime, date)
        }
    }
}

sealed class AiSuggestionUiState {
    data object Initial : AiSuggestionUiState()
    data object Loading : AiSuggestionUiState()
    data class Success(val advice: String) : AiSuggestionUiState()
    data class Error(val message: String) : AiSuggestionUiState()
}
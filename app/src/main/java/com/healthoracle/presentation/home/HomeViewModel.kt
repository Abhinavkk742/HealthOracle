package com.healthoracle.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.healthoracle.core.dashboard.DailyHealthData
import com.healthoracle.core.dashboard.HealthScore
import com.healthoracle.core.dashboard.HealthScoreEngine
import com.healthoracle.core.dashboard.InsightGenerator
import com.healthoracle.core.dashboard.SmartInsight
import com.healthoracle.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// UI STATE
// ─────────────────────────────────────────────────────────────────────────────

data class HomeUiState(
    val isLoading: Boolean         = true,
    val profile: UserProfile       = UserProfile(),
    val healthData: DailyHealthData = DailyHealthData(),
    val yesterdayData: DailyHealthData? = null,
    val healthScore: HealthScore?  = null,
    val insights: List<SmartInsight> = emptyList(),
    val streakDays: Int            = 0,
    val error: String?             = null
)

// ─────────────────────────────────────────────────────────────────────────────
// VIEW MODEL
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    private fun loadAll() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            try {
                // Load profile
                val doc = firestore.collection("users").document(userId).get().await()
                val profile = doc.toObject(UserProfile::class.java)?.copy(uid = userId)
                    ?: UserProfile(uid = userId)

                // Load today's health log
                val today     = LocalDate.now()
                val yesterday = today.minusDays(1)
                val fmt       = DateTimeFormatter.ofPattern("yyyy-MM-dd")

                val todayData  = loadHealthLog(userId, today.format(fmt))
                val yesterData = loadHealthLog(userId, yesterday.format(fmt))
                val streak     = loadStreak(userId)

                val score    = HealthScoreEngine.calculate(todayData, yesterData)
                val insights = InsightGenerator.generate(todayData, yesterData)

                _uiState.value = HomeUiState(
                    isLoading     = false,
                    profile       = profile,
                    healthData    = todayData,
                    yesterdayData = yesterData,
                    healthScore   = score,
                    insights      = insights,
                    streakDays    = streak
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }

    private suspend fun loadHealthLog(userId: String, dateKey: String): DailyHealthData {
        return try {
            val doc = firestore
                .collection("users").document(userId)
                .collection("health_logs").document(dateKey)
                .get().await()
            if (doc.exists()) {
                DailyHealthData(
                    steps        = (doc.getLong("steps") ?: 0).toInt(),
                    stepGoal     = (doc.getLong("stepGoal") ?: 10_000).toInt(),
                    waterGlasses = (doc.getLong("waterGlasses") ?: 0).toInt(),
                    waterGoal    = (doc.getLong("waterGoal") ?: 8).toInt(),
                    sleepHours   = (doc.getDouble("sleepHours") ?: 0.0).toFloat(),
                    sleepGoal    = (doc.getDouble("sleepGoal") ?: 8.0).toFloat(),
                    moodScore    = (doc.getLong("moodScore") ?: 0).toInt(),
                    date         = dateKey
                )
            } else {
                DailyHealthData(date = dateKey)
            }
        } catch (e: Exception) {
            DailyHealthData(date = dateKey)
        }
    }

    private suspend fun loadStreak(userId: String): Int {
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            (doc.getLong("streakDays") ?: 0).toInt()
        } catch (e: Exception) {
            0
        }
    }

    /** Called when user logs mood from the Dashboard */
    fun logMood(score: Int) {
        viewModelScope.launch {
            val userId  = auth.currentUser?.uid ?: return@launch
            val dateKey = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            try {
                firestore.collection("users").document(userId)
                    .collection("health_logs").document(dateKey)
                    .set(mapOf("moodScore" to score), com.google.firebase.firestore.SetOptions.merge())
                    .await()
                val updated = _uiState.value.healthData.copy(moodScore = score)
                val score2  = HealthScoreEngine.calculate(updated, _uiState.value.yesterdayData)
                val insights = InsightGenerator.generate(updated, _uiState.value.yesterdayData)
                _uiState.value = _uiState.value.copy(
                    healthData  = updated,
                    healthScore = score2,
                    insights    = insights
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.localizedMessage)
            }
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        loadAll()
    }
}

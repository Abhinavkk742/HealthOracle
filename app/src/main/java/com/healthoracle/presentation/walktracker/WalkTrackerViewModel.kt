package com.healthoracle.presentation.walktracker

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.gson.Gson
import com.healthoracle.data.local.entity.WalkSession
import com.healthoracle.data.model.RoutePoint
import com.healthoracle.data.repository.WalkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WalkTrackerUiState(
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val distanceMeters: Float = 0f,
    val durationSeconds: Long = 0L,
    val currentSpeed: Float = 0f,
    val routePoints: List<RoutePoint> = emptyList(),
    val lastLocation: Location? = null,
    val sessionSaved: Boolean = false
)

@HiltViewModel
class WalkTrackerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val walkRepository: WalkRepository
) : ViewModel() {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _uiState = MutableStateFlow(WalkTrackerUiState())
    val uiState: StateFlow<WalkTrackerUiState> = _uiState.asStateFlow()

    val walkHistory = walkRepository.getAllSessions()

    private var startTimeMs: Long = 0L
    private var timerJob: Job? = null
    private var locationCallback: LocationCallback? = null
    private val gson = Gson()

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY, 3000L
    ).setMinUpdateDistanceMeters(5f).build()

    init {
        // Fetch any missing walks from Firebase when the ViewModel starts
        viewModelScope.launch {
            walkRepository.syncWalksFromFirebase()
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        startTimeMs = System.currentTimeMillis()
        _uiState.update {
            it.copy(
                isTracking = true,
                isPaused = false,
                distanceMeters = 0f,
                durationSeconds = 0L,
                routePoints = emptyList(),
                lastLocation = null,
                sessionSaved = false
            )
        }

        startTimer()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val state = _uiState.value

                val newPoint = RoutePoint(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = System.currentTimeMillis()
                )
                val updatedPoints = state.routePoints + newPoint

                val addedDistance = state.lastLocation?.distanceTo(location) ?: 0f

                _uiState.update {
                    it.copy(
                        distanceMeters = it.distanceMeters + addedDistance,
                        currentSpeed = location.speed,
                        routePoints = updatedPoints,
                        lastLocation = location
                    )
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            context.mainLooper
        )
    }

    fun pauseTracking() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        timerJob?.cancel()
        _uiState.update { it.copy(isPaused = true) }
    }

    @SuppressLint("MissingPermission")
    fun resumeTracking() {
        _uiState.update { it.copy(isPaused = false) }
        startTimer()
        locationCallback?.let {
            fusedLocationClient.requestLocationUpdates(locationRequest, it, context.mainLooper)
        }
    }

    fun stopAndSave() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        timerJob?.cancel()

        val state = _uiState.value
        if (state.routePoints.isNotEmpty()) {
            viewModelScope.launch {
                val session = WalkSession(
                    startTime = startTimeMs,
                    endTime = System.currentTimeMillis(),
                    distanceMeters = state.distanceMeters,
                    steps = estimateSteps(state.distanceMeters),
                    durationSeconds = state.durationSeconds,
                    routePointsJson = gson.toJson(state.routePoints)
                )
                walkRepository.saveSession(session)
                _uiState.update { it.copy(isTracking = false, isPaused = false, sessionSaved = true) }
            }
        } else {
            _uiState.update { it.copy(isTracking = false, isPaused = false) }
        }
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                _uiState.update { it.copy(durationSeconds = it.durationSeconds + 1) }
            }
        }
    }

    private fun estimateSteps(distanceMeters: Float): Int {
        return (distanceMeters / 0.762f).toInt()
    }

    fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%02d:%02d:%02d".format(h, m, s)
        else "%02d:%02d".format(m, s)
    }

    fun formatDistance(meters: Float): String {
        return if (meters >= 1000) "%.2f km".format(meters / 1000)
        else "%.0f m".format(meters)
    }
    fun syncFromFirebase() {
        viewModelScope.launch {
            walkRepository.syncWalksFromFirebase()
        }
    }
    override fun onCleared() {
        super.onCleared()
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        timerJob?.cancel()
    }
}
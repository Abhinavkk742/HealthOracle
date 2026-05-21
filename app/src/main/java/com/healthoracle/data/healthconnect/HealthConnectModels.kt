package com.healthoracle.data.healthconnect

/**
 * Represents the overall Health Connect availability state.
 */
enum class HealthConnectAvailability {
    AVAILABLE,
    NOT_INSTALLED,
    NOT_SUPPORTED
}

/**
 * Aggregated daily health snapshot fetched from Health Connect.
 */
data class DailyHealthData(
    val steps: Long = 0L,
    val stepsGoal: Long = 10_000L,
    val heartRateBpm: Double? = null,        // average BPM for today
    val heartRateMin: Double? = null,
    val heartRateMax: Double? = null,
    val sleepHours: Double? = null,          // total sleep in hours
    val sleepQuality: SleepQuality = SleepQuality.UNKNOWN,
    val caloriesBurned: Double? = null,
    val activeMinutes: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasPermissions: Boolean = false
)

enum class SleepQuality { UNKNOWN, POOR, FAIR, GOOD, EXCELLENT }

/**
 * Weekly step data point for the Insights chart.
 */
data class WeeklyStepPoint(
    val dayLabel: String,   // "Mon", "Tue", …
    val steps: Long
)

/**
 * Sealed result for any Health Connect operation.
 */
sealed class HealthConnectResult<out T> {
    data class Success<T>(val data: T) : HealthConnectResult<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : HealthConnectResult<Nothing>()
    object Loading : HealthConnectResult<Nothing>()
}

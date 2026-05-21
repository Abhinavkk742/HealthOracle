package com.healthoracle.data.healthconnect

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HealthConnectManager
 *
 * Central repository for all Health Connect interactions.
 * Wraps availability checks, permission contracts, and data fetching
 * behind a clean, coroutine-based API that the ViewModels consume.
 */
@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // ── Permissions we request ────────────────────────────────────────────────

    val requiredPermissions: Set<String> = buildSet {
        add(HealthPermission.getReadPermission(StepsRecord::class))
        add(HealthPermission.getReadPermission(HeartRateRecord::class))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(HealthPermission.getReadPermission(SleepSessionRecord::class))
        }
    }

    // ── Availability ──────────────────────────────────────────────────────────

    fun checkAvailability(): HealthConnectAvailability {
        return when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE          -> HealthConnectAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthConnectAvailability.NOT_INSTALLED
            else                                       -> HealthConnectAvailability.NOT_SUPPORTED
        }
    }

    fun getInstallIntent(): Intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("market://details?id=com.google.android.apps.healthdata")
        setPackage("com.android.vending")
    }

    // ── Client accessor (lazy, only if available) ─────────────────────────────

    private val healthConnectClient: HealthConnectClient? by lazy {
        runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
    }

    // ── Permission helpers ────────────────────────────────────────────────────

    /**
     * Returns the ActivityResultContract for the permission request launcher.
     * Register this in your Activity / Composable via rememberLauncherForActivityResult.
     */
    fun requestPermissionsActivityContract(): ActivityResultContract<Set<String>, Set<String>> =
        PermissionController.createRequestPermissionResultContract()

    suspend fun hasAllPermissions(): Boolean = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext false
        runCatching {
            client.permissionController.getGrantedPermissions()
                .containsAll(requiredPermissions)
        }.getOrDefault(false)
    }

    suspend fun getGrantedPermissions(): Set<String> = withContext(Dispatchers.IO) {
        val client = healthConnectClient ?: return@withContext emptySet()
        runCatching { client.permissionController.getGrantedPermissions() }.getOrDefault(emptySet())
    }

    // ── Steps ─────────────────────────────────────────────────────────────────

    suspend fun readTodaySteps(): HealthConnectResult<Long> = withContext(Dispatchers.IO) {
        val client = healthConnectClient
            ?: return@withContext HealthConnectResult.Error("Health Connect not available")
        runCatching {
            val today = LocalDate.now()
            val start = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val end   = Instant.now()
            val response = client.aggregate(
                AggregateRequest(
                    metrics       = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            val steps = response[StepsRecord.COUNT_TOTAL] ?: 0L
            HealthConnectResult.Success(steps)
        }.getOrElse { HealthConnectResult.Error(it.message ?: "Unknown error", it) }
    }

    suspend fun readWeeklySteps(): HealthConnectResult<List<WeeklyStepPoint>> =
        withContext(Dispatchers.IO) {
            val client = healthConnectClient
                ?: return@withContext HealthConnectResult.Error("Health Connect not available")
            runCatching {
                val today     = LocalDate.now()
                val weekStart = today.minusDays(6)
                val start     = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant()
                val end       = Instant.now()
                val formatter = DateTimeFormatter.ofPattern("EEE")

                val response = client.aggregateGroupByDuration(
                    AggregateGroupByDurationRequest(
                        metrics         = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(start, end),
                        timeRangeSlicer = Duration.ofDays(1)
                    )
                )
                val points = response.map { bucket ->
                    val day   = LocalDateTime.ofInstant(bucket.startTime, ZoneId.systemDefault())
                    val label = day.format(formatter)
                    val steps = bucket.result[StepsRecord.COUNT_TOTAL] ?: 0L
                    WeeklyStepPoint(dayLabel = label, steps = steps)
                }
                HealthConnectResult.Success(points)
            }.getOrElse { HealthConnectResult.Error(it.message ?: "Unknown error", it) }
        }

    // ── Heart Rate ────────────────────────────────────────────────────────────

    suspend fun readTodayHeartRate(): HealthConnectResult<Triple<Double, Double, Double>> =
        withContext(Dispatchers.IO) {
            val client = healthConnectClient
                ?: return@withContext HealthConnectResult.Error("Health Connect not available")
            runCatching {
                val today = LocalDate.now()
                val start = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
                val end   = Instant.now()

                val records = client.readRecords(
                    ReadRecordsRequest(
                        recordType      = HeartRateRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(start, end)
                    )
                ).records

                val allSamples = records.flatMap { it.samples }
                if (allSamples.isEmpty()) {
                    return@withContext HealthConnectResult.Error("No heart rate data")
                }
                val bpms = allSamples.map { it.beatsPerMinute.toDouble() }
                val avg  = bpms.average()
                val min  = bpms.min()
                val max  = bpms.max()
                HealthConnectResult.Success(Triple(avg, min, max))
            }.getOrElse { HealthConnectResult.Error(it.message ?: "Unknown error", it) }
        }

    // ── Sleep ─────────────────────────────────────────────────────────────────

    suspend fun readLastNightSleep(): HealthConnectResult<Double> = withContext(Dispatchers.IO) {
        val client = healthConnectClient
            ?: return@withContext HealthConnectResult.Error("Health Connect not available")
        runCatching {
            val now       = Instant.now()
            val yesterday = now.minus(24, ChronoUnit.HOURS)

            val records = client.readRecords(
                ReadRecordsRequest(
                    recordType      = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(yesterday, now)
                )
            ).records

            if (records.isEmpty()) {
                return@withContext HealthConnectResult.Error("No sleep data")
            }
            val totalMillis = records.sumOf {
                Duration.between(it.startTime, it.endTime).toMillis()
            }
            val hours = totalMillis / 3_600_000.0
            HealthConnectResult.Success(hours)
        }.getOrElse { HealthConnectResult.Error(it.message ?: "Unknown error", it) }
    }

    // ── Aggregate daily snapshot ──────────────────────────────────────────────

    suspend fun readDailyHealthData(): DailyHealthData {
        val hasPerms = hasAllPermissions()
        if (!hasPerms) return DailyHealthData(hasPermissions = false)

        val steps = when (val r = readTodaySteps()) {
            is HealthConnectResult.Success -> r.data
            else                           -> 0L
        }
        val (avg, min, max) = when (val r = readTodayHeartRate()) {
            is HealthConnectResult.Success -> r.data
            else                           -> Triple(null, null, null)
        }
        val sleep = when (val r = readLastNightSleep()) {
            is HealthConnectResult.Success -> r.data
            else                           -> null
        }
        val quality = when {
            sleep == null          -> SleepQuality.UNKNOWN
            sleep < 4.0            -> SleepQuality.POOR
            sleep < 6.0            -> SleepQuality.FAIR
            sleep < 7.5            -> SleepQuality.GOOD
            else                   -> SleepQuality.EXCELLENT
        }
        return DailyHealthData(
            steps         = steps,
            heartRateBpm  = avg,
            heartRateMin  = min,
            heartRateMax  = max,
            sleepHours    = sleep,
            sleepQuality  = quality,
            hasPermissions = true
        )
    }

    // ── Sample / fallback data (shown when HC not available) ──────────────────

    fun getSampleData(): DailyHealthData = DailyHealthData(
        steps         = 7_248L,
        stepsGoal     = 10_000L,
        heartRateBpm  = 72.4,
        heartRateMin  = 58.0,
        heartRateMax  = 94.0,
        sleepHours    = 7.2,
        sleepQuality  = SleepQuality.GOOD,
        caloriesBurned = 456.0,
        activeMinutes  = 38L,
        hasPermissions = false
    )

    fun getSampleWeeklySteps(): List<WeeklyStepPoint> = listOf(
        WeeklyStepPoint("Mon", 8_521),
        WeeklyStepPoint("Tue", 6_234),
        WeeklyStepPoint("Wed", 9_875),
        WeeklyStepPoint("Thu", 5_102),
        WeeklyStepPoint("Fri", 11_045),
        WeeklyStepPoint("Sat", 7_680),
        WeeklyStepPoint("Sun", 7_248)
    )
}

package com.db2k.wearstretch.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.ZonedDateTime

data class WorkoutDetails(
    val avgHeartRate: Int,
    val maxHeartRate: Int,
    val totalSteps: Int
)

class HealthConnectManager(private val context: Context) {
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    val permissions = setOf(
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    /**
     * Lets the cool-down check read openGym's workouts while the app is closed.
     * Requested separately from [permissions] because older Health Connect
     * versions don't offer it.
     */
    fun backgroundReadAvailable(): Boolean = try {
        healthConnectClient.features.getFeatureStatus(
            HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND
        ) == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
    } catch (e: Exception) {
        false
    }

    suspend fun hasBackgroundRead(): Boolean =
        healthConnectClient.permissionController.getGrantedPermissions()
            .contains(HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND)

    suspend fun hasAllPermissions(): Boolean {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }

    suspend fun hasReadPermission(): Boolean {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        return granted.contains(HealthPermission.getReadPermission(ExerciseSessionRecord::class))
    }

    suspend fun writeWorkout(
        id: String,
        startTime: ZonedDateTime,
        endTime: ZonedDateTime,
        title: String,
        heartRateSamples: List<Int> = emptyList(),
        stepCount: Int = 0,
        location: String? = null
    ) {
        val records = mutableListOf<Record>()

        // Attach the captured location as a single-point ExerciseRoute so it is
        // visible as a location in other apps (Health Connect only surfaces
        // location via routes, not the notes field). Best-effort: if the route
        // is unparseable we simply omit it.
        val route = parseRoute(location, startTime.toInstant())

        fun session(withRoute: ExerciseRoute?) = ExerciseSessionRecord(
            startTime = startTime.toInstant(),
            startZoneOffset = startTime.offset,
            endTime = endTime.toInstant(),
            endZoneOffset = endTime.offset,
            metadata = Metadata.manualEntry(clientRecordId = id),
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING,
            title = title,
            notes = location,
            exerciseRoute = withRoute
        )
        records.add(session(route))

        if (heartRateSamples.isNotEmpty()) {
            val hrRecord = HeartRateRecord(
                startTime = startTime.toInstant(),
                startZoneOffset = startTime.offset,
                endTime = endTime.toInstant(),
                endZoneOffset = endTime.offset,
                metadata = Metadata.manualEntry(clientRecordId = "${id}_hr"),
                samples = heartRateSamples.mapIndexed { index, bpm ->
                    val sampleTime = startTime.toInstant().plusSeconds(index.toLong())
                    if (sampleTime.isBefore(endTime.toInstant())) {
                        HeartRateRecord.Sample(sampleTime, bpm.toLong())
                    } else {
                        HeartRateRecord.Sample(endTime.toInstant(), bpm.toLong())
                    }
                }
            )
            records.add(hrRecord)
        }

        if (stepCount > 0) {
            val stepsRecord = StepsRecord(
                startTime = startTime.toInstant(),
                startZoneOffset = startTime.offset,
                endTime = endTime.toInstant(),
                endZoneOffset = endTime.offset,
                metadata = Metadata.manualEntry(clientRecordId = "${id}_steps"),
                count = stepCount.toLong()
            )
            records.add(stepsRecord)
        }
        
        try {
            healthConnectClient.insertRecords(records)
        } catch (e: Exception) {
            if (route != null) {
                // The route may have been rejected (validation/permission). Retry
                // without it so the session, heart rate and steps still save.
                android.util.Log.w("HealthConnect", "Insert with route failed; retrying without route", e)
                records[0] = session(null)
                healthConnectClient.insertRecords(records)
            } else {
                android.util.Log.e("HealthConnect", "Error writing to Health Connect", e)
                throw e
            }
        }
    }

    /** Parses a "lat,lng" string into a single-point [ExerciseRoute], or null. */
    private fun parseRoute(location: String?, time: Instant): ExerciseRoute? {
        if (location.isNullOrBlank()) return null
        return try {
            val parts = location.split(",")
            if (parts.size != 2) return null
            val lat = parts[0].trim().toDouble()
            val lng = parts[1].trim().toDouble()
            if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
            ExerciseRoute(listOf(ExerciseRoute.Location(time = time, latitude = lat, longitude = lng)))
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getWorkoutDetails(startTime: Instant, endTime: Instant): WorkoutDetails {
        return try {
            val hrResponse = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            
            val stepsResponse = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )

            val allHeartRates = hrResponse.records.flatMap { it.samples }.map { it.beatsPerMinute }
            val avgHeartRate = if (allHeartRates.isNotEmpty()) allHeartRates.average().toInt() else 0
            val maxHeartRate = if (allHeartRates.isNotEmpty()) allHeartRates.maxOrNull()?.toInt() ?: 0 else 0
            val totalSteps = stepsResponse.records.sumOf { it.count }

            WorkoutDetails(
                avgHeartRate = avgHeartRate,
                maxHeartRate = maxHeartRate,
                totalSteps = totalSteps.toInt()
            )
        } catch (e: Exception) {
            android.util.Log.e("HealthConnect", "Error reading workout details: ${e.message}")
            WorkoutDetails(0, 0, 0)
        }
    }

    suspend fun readRecentStretchingWorkouts(): List<ExerciseSessionRecord> {
        val response = healthConnectClient.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.after(ZonedDateTime.now().minusDays(30).toInstant())
            )
        )
        
        android.util.Log.d("HealthConnect", "Total records found: ${response.records.size}")
        
        // Only stretching sessions this app wrote. Other apps (openGym, SparkyFitness,
        // the watch's own fitness app) also write titled exercise sessions; those are
        // not stretches and must not appear in, or be deletable from, the Me tab.
        val filtered = response.records
            .filter { isOwnStretch(it) }
            .sortedByDescending { it.startTime }

        android.util.Log.d("HealthConnect", "Filtered records count: ${filtered.size}")
        return filtered
    }

    private fun isOwnStretch(record: ExerciseSessionRecord) =
        record.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING &&
            record.metadata.dataOrigin.packageName == context.packageName

    /**
     * Workouts written by OTHER apps (e.g. openGym strength sessions) that ended at
     * or after [since]. Stretching and yoga sessions are skipped: a cool-down is
     * only suggested after "real" training.
     */
    suspend fun readOtherWorkoutsEndedSince(since: Instant): List<ExerciseSessionRecord> {
        val response = healthConnectClient.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                // Sessions can start well before `since` and still end after it.
                timeRangeFilter = TimeRangeFilter.after(since.minusSeconds(6 * 3600))
            )
        )
        return response.records
            .filter { it.metadata.dataOrigin.packageName != context.packageName }
            .filter {
                it.exerciseType != ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING &&
                    it.exerciseType != ExerciseSessionRecord.EXERCISE_TYPE_YOGA
            }
            .filter { !it.endTime.isBefore(since) }
            .sortedByDescending { it.endTime }
    }

    /** True if this app logged a stretch that started at or after [since]. */
    suspend fun hasStretchedSince(since: Instant): Boolean {
        val response = healthConnectClient.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.after(since)
            )
        )
        return response.records.any { isOwnStretch(it) }
    }

    suspend fun deleteAllStretchingWorkouts() {
        // Health Connect only lets an app delete its own records, so this never
        // touches openGym/Sparky data. Heart rate and steps written with each
        // stretch go too, otherwise they'd linger as orphans in other apps.
        val range = TimeRangeFilter.after(ZonedDateTime.now().minusDays(30).toInstant())
        healthConnectClient.deleteRecords(ExerciseSessionRecord::class, timeRangeFilter = range)
        healthConnectClient.deleteRecords(HeartRateRecord::class, timeRangeFilter = range)
        healthConnectClient.deleteRecords(StepsRecord::class, timeRangeFilter = range)
    }

    suspend fun deleteWorkout(workout: ExerciseSessionRecord) {
        healthConnectClient.deleteRecords(
            recordType = ExerciseSessionRecord::class,
            recordIdsList = listOf(workout.metadata.id),
            clientRecordIdsList = emptyList()
        )
        // writeWorkout() stores this stretch's heart rate and steps as
        // "<id>_hr" / "<id>_steps". Without this, deleting a stretch left its
        // steps counted in Sparky and Fitbit.
        val clientId = workout.metadata.clientRecordId ?: return
        runCatching {
            healthConnectClient.deleteRecords(HeartRateRecord::class, emptyList(), listOf("${clientId}_hr"))
        }
        runCatching {
            healthConnectClient.deleteRecords(StepsRecord::class, emptyList(), listOf("${clientId}_steps"))
        }
    }
}


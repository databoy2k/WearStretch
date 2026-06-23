package com.db2k.wearstretch.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
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
        
        val sessionRecord = ExerciseSessionRecord(
            startTime = startTime.toInstant(),
            startZoneOffset = startTime.offset,
            endTime = endTime.toInstant(),
            endZoneOffset = endTime.offset,
            metadata = Metadata.manualEntry(),
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING,
            title = title,
            notes = location
        )
        records.add(sessionRecord)

        if (heartRateSamples.isNotEmpty()) {
            val hrRecord = HeartRateRecord(
                startTime = startTime.toInstant(),
                startZoneOffset = startTime.offset,
                endTime = endTime.toInstant(),
                endZoneOffset = endTime.offset,
                metadata = Metadata.manualEntry(),
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
                metadata = Metadata.manualEntry(),
                count = stepCount.toLong()
            )
            records.add(stepsRecord)
        }
        
        try {
            healthConnectClient.insertRecords(records)
        } catch (e: Exception) {
            android.util.Log.e("HealthConnect", "Error writing to Health Connect", e)
            throw e
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
        
        // Filter: include everything with a title (our workouts) or matching our package
        val filtered = response.records
            .filter { 
                !it.title.isNullOrEmpty() || 
                it.metadata.dataOrigin.packageName == context.packageName
            }
            .sortedByDescending { it.startTime }
            
        android.util.Log.d("HealthConnect", "Filtered records count: ${filtered.size}")
        return filtered
    }

    suspend fun deleteAllStretchingWorkouts() {
        healthConnectClient.deleteRecords(
            ExerciseSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.after(ZonedDateTime.now().minusDays(30).toInstant())
        )
    }

    suspend fun deleteWorkout(workout: ExerciseSessionRecord) {
        healthConnectClient.deleteRecords(
            recordType = ExerciseSessionRecord::class,
            recordIdsList = listOf(workout.metadata.id),
            clientRecordIdsList = emptyList()
        )
    }
}


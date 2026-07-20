package com.db2k.wearstretch.presentation

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.db2k.wearstretch.data.WearToMobileSender
import com.db2k.wearstretch.data.WorkoutRepository
import com.db2k.wearstretch.data.WorkoutSession
import com.db2k.wearstretch.model.Routine
import kotlinx.coroutines.launch

class WorkoutViewModel : ViewModel() {
    private var messageSender: WearToMobileSender? = null
    private var workoutRepository: WorkoutRepository? = null

    // Delegate observable states to WorkoutSession singleton
    val routine get() = WorkoutSession.routine
    val currentStretchIndex get() = WorkoutSession.currentStretchIndex
    val timeLeftSeconds get() = WorkoutSession.timeLeftSeconds
    val isPaused get() = WorkoutSession.isPaused
    val isFinished get() = WorkoutSession.isFinished
    val isBreak get() = WorkoutSession.isBreak
    val isSwitchingSides get() = WorkoutSession.isSwitchingSides
    val isSplitSecondHalf get() = WorkoutSession.isSplitSecondHalf
    val isStarting get() = WorkoutSession.isStarting
    
    val nextStretchName get() = WorkoutSession.nextStretchName
    val nextStretchDescription get() = WorkoutSession.nextStretchDescription
    val currentHeartRate get() = WorkoutSession.currentHeartRate
    val currentSteps get() = WorkoutSession.currentSteps
    val locationSnapshot get() = WorkoutSession.locationSnapshot

    fun startRoutine(context: Context, selectedRoutine: Routine) {
        WorkoutSession.reset()
        WorkoutSession.routine = selectedRoutine
        
        // Start WorkoutService foreground service
        val intent = Intent(context, WorkoutService::class.java)
        context.startForegroundService(intent)
    }

    fun togglePause() {
        WorkoutSession.isPaused = !WorkoutSession.isPaused
    }

    fun pauseWorkout() {
        WorkoutSession.isPaused = true
    }

    fun nextStretch(context: Context) {
        // Send intent to WorkoutService to trigger next stretch
        val intent = Intent(context, WorkoutService::class.java).apply {
            action = "ACTION_NEXT_STRETCH"
        }
        context.startService(intent)
    }

    fun cancelWorkout(context: Context) {
        context.stopService(Intent(context, WorkoutService::class.java))
        WorkoutSession.reset()
    }

    /**
     * Re-publishes any workouts whose DataItem write previously failed. Normally
     * a no-op (records are published the moment a workout finishes), so this is
     * cheap to call on launch. Re-publishing is safe: identical DataItems are
     * ignored by the data layer and the phone dedups on Health Connect insert.
     */
    fun publishUnsynced(context: Context) {
        if (workoutRepository == null) workoutRepository = WorkoutRepository(context.applicationContext)
        if (messageSender == null) messageSender = WearToMobileSender(context.applicationContext)

        viewModelScope.launch {
            val unsynced = workoutRepository?.getUnsyncedWorkouts() ?: emptyList()
            unsynced.forEach { record ->
                val published = messageSender?.publishRecord(record) ?: false
                if (published) {
                    workoutRepository?.updateSyncStatus(record.id, true)
                }
            }
        }
    }
}

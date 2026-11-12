package com.db2k.wearstretch.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.db2k.wearstretch.model.Routine

object WorkoutSession {
    var routine by mutableStateOf<Routine?>(null)
    var currentStretchIndex by mutableIntStateOf(0)
    var timeLeftSeconds by mutableIntStateOf(0)
    var isPaused by mutableStateOf(false)
    var isFinished by mutableStateOf(false)
    var isBreak by mutableStateOf(false)
    var isSwitchingSides by mutableStateOf(false)
    var isSplitSecondHalf by mutableStateOf(false)
    var isStarting by mutableStateOf(false)
    
    var nextStretchName by mutableStateOf<String?>(null)
    var nextStretchDescription by mutableStateOf<String?>(null)
    var currentHeartRate by mutableIntStateOf(0)
    var currentSteps by mutableIntStateOf(0)
    var locationSnapshot by mutableStateOf<String?>(null)

    /** Length of the current phase (stretch side, break, get ready) for the progress ring. */
    var phaseDurationSeconds by mutableIntStateOf(0)
    /** Stretches the user skipped, for the finish summary. */
    var skippedStretches by mutableIntStateOf(0)
    var endTimeMillis = 0L

    val heartRateSamples = mutableListOf<Int>()
    var initialSteps = -1
    var startTimeMillis = 0L

    fun reset() {
        routine = null
        currentStretchIndex = 0
        timeLeftSeconds = 0
        isPaused = false
        isFinished = false
        isBreak = false
        isSwitchingSides = false
        isSplitSecondHalf = false
        isStarting = false
        nextStretchName = null
        nextStretchDescription = null
        currentHeartRate = 0
        currentSteps = 0
        locationSnapshot = null
        phaseDurationSeconds = 0
        skippedStretches = 0
        endTimeMillis = 0L
        heartRateSamples.clear()
        initialSteps = -1
        startTimeMillis = 0L
    }
}

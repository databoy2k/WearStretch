package com.db2k.wearstretch.presentation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.db2k.wearstretch.R
import com.db2k.wearstretch.data.SettingsStore
import com.db2k.wearstretch.data.WearToMobileSender
import com.db2k.wearstretch.data.WorkoutRepository
import com.db2k.wearstretch.data.WorkoutSession
import com.db2k.wearstretch.model.Routine
import com.db2k.wearstretch.model.Stretch
import com.db2k.wearstretch.model.WorkoutRecord
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.*
import java.util.UUID

class WorkoutService : Service(), SensorEventListener {
    private val binder = LocalBinder()
    private val notificationId = 101
    private val channelId = "workout_channel"
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null
    
    private var sensorManager: SensorManager? = null
    private var hrSensor: Sensor? = null
    private var stepSensor: Sensor? = null
    private var stepDetectorSensor: Sensor? = null
    
    private var messageSender: WearToMobileSender? = null
    private var workoutRepository: WorkoutRepository? = null

    class LocalBinder : Binder()

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        messageSender = WearToMobileSender(applicationContext)
        workoutRepository = WorkoutRepository(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "ACTION_NEXT_STRETCH") {
            nextStretch()
            return START_STICKY
        }

        val routineName = WorkoutSession.routine?.name ?: "Stretching"
        startWorkoutNotification(routineName)
        
        // Start tracking
        startWorkoutTracking()
        
        return START_STICKY
    }

    private fun startWorkoutTracking() {
        // Initialize sensors if settings allow and permissions are granted
        val hasHrPermission = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.BODY_SENSORS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasStepPermission = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACTIVITY_RECOGNITION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        if (SettingsStore.trackHeartRate.value || SettingsStore.trackSteps.value) {
            if (hasHrPermission || hasStepPermission) {
                sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
                
                if (SettingsStore.trackHeartRate.value && hasHrPermission) {
                    hrSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_HEART_RATE)
                    if (hrSensor != null) {
                        sensorManager?.registerListener(this, hrSensor, SensorManager.SENSOR_DELAY_NORMAL)
                    }
                }
                
                if (SettingsStore.trackSteps.value && hasStepPermission) {
                    stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
                    stepDetectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
                    
                    if (stepDetectorSensor != null) {
                        sensorManager?.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_FASTEST)
                    }
                    if (stepSensor != null) {
                        sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
                    }
                }
            }
        }
        
        // Coarse location tracking (balanced power accuracy)
        if (SettingsStore.trackLocation.value) {
            val hasLocationPermission = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (hasLocationPermission) {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                        if (WorkoutSession.locationSnapshot == null && loc != null) {
                            WorkoutSession.locationSnapshot = "${loc.latitude},${loc.longitude}"
                            Log.d("WearStretch", "Location initialized from last known: ${WorkoutSession.locationSnapshot}")
                        }
                    }
                    
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token)
                        .addOnSuccessListener { location ->
                            location?.let {
                                WorkoutSession.locationSnapshot = "${it.latitude},${it.longitude}"
                                Log.d("WearStretch", "Location captured (coarse): ${WorkoutSession.locationSnapshot}")
                            }
                        }
                } catch (_: SecurityException) {
                    Log.e("WearStretch", "Location permission missing")
                }
            }
        }

        WorkoutSession.startTimeMillis = System.currentTimeMillis()
        
        if (SettingsStore.startDelaySeconds.value > 0) {
            startPreparing()
        } else {
            vibrate(VibeType.NORMAL)
            val firstStretch = WorkoutSession.routine?.stretches?.getOrNull(0)
            if (firstStretch != null) {
                startStretch(firstStretch, secondHalf = false)
            } else {
                finishWorkout()
            }
        }
    }

    private fun startPreparing() {
        WorkoutSession.isStarting = true
        WorkoutSession.isBreak = false
        WorkoutSession.isSwitchingSides = false
        WorkoutSession.timeLeftSeconds = SettingsStore.startDelaySeconds.value
        WorkoutSession.nextStretchName = WorkoutSession.routine?.stretches?.getOrNull(0)?.name
        WorkoutSession.nextStretchDescription = WorkoutSession.routine?.stretches?.getOrNull(0)?.description
        runTimer()
    }

    private fun startStretch(stretch: Stretch, secondHalf: Boolean) {
        WorkoutSession.isStarting = false
        WorkoutSession.isBreak = false
        WorkoutSession.isSwitchingSides = false
        WorkoutSession.isSplitSecondHalf = secondHalf
        
        WorkoutSession.timeLeftSeconds = if (stretch.isSplit) stretch.durationSeconds / 2 else stretch.durationSeconds
        
        val nextIndex = WorkoutSession.currentStretchIndex + 1
        val nextStretch = WorkoutSession.routine?.stretches?.getOrNull(nextIndex)
        
        if (stretch.isSplit && !secondHalf) {
            WorkoutSession.nextStretchName = "Switch Sides"
            WorkoutSession.nextStretchDescription = stretch.description
        } else {
            WorkoutSession.nextStretchName = nextStretch?.name
            WorkoutSession.nextStretchDescription = nextStretch?.description
        }
        
        runTimer()
    }

    private fun startBreak(stretch: Stretch) {
        WorkoutSession.isBreak = true
        WorkoutSession.isSwitchingSides = false
        WorkoutSession.nextStretchName = stretch.name
        WorkoutSession.nextStretchDescription = stretch.description
        WorkoutSession.timeLeftSeconds = stretch.breakDurationSeconds
        runTimer()
    }

    private fun startSwitchSidesBreak(stretch: Stretch) {
        WorkoutSession.isBreak = false
        WorkoutSession.isSwitchingSides = true
        WorkoutSession.nextStretchName = stretch.name + " (Side 2)"
        WorkoutSession.nextStretchDescription = stretch.description
        WorkoutSession.timeLeftSeconds = stretch.splitBreakDurationSeconds
        runTimer()
    }

    private fun runTimer() {
        timerJob?.cancel()
        val durationAtStart = WorkoutSession.timeLeftSeconds
        val timerStartTime = System.currentTimeMillis()
        
        timerJob = serviceScope.launch {
            while (WorkoutSession.timeLeftSeconds > 0) {
                if (!WorkoutSession.isPaused && !WorkoutSession.isFinished) {
                    val elapsedSeconds = (System.currentTimeMillis() - timerStartTime) / 1000
                    WorkoutSession.timeLeftSeconds = (durationAtStart - elapsedSeconds).toInt().coerceAtLeast(0)
                    delay(500)
                } else {
                    delay(100)
                    if (!WorkoutSession.isPaused && !WorkoutSession.isFinished) {
                        runTimer()
                        return@launch
                    }
                }
            }
            if ((WorkoutSession.timeLeftSeconds == 0) && !WorkoutSession.isFinished) {
                if (WorkoutSession.isStarting) {
                    WorkoutSession.isStarting = false
                    vibrate(VibeType.NORMAL)
                    val firstStretch = WorkoutSession.routine?.stretches?.getOrNull(0)
                    if (firstStretch != null) {
                        startStretch(firstStretch, secondHalf = false)
                    } else {
                        finishWorkout()
                    }
                    return@launch
                }
                
                val currentStretch = WorkoutSession.routine?.stretches?.getOrNull(WorkoutSession.currentStretchIndex)
                if (currentStretch == null) {
                    finishWorkout()
                    return@launch
                }
                
                if (WorkoutSession.isSwitchingSides) {
                    vibrate(VibeType.NORMAL)
                    startStretch(currentStretch, secondHalf = true)
                } else if (WorkoutSession.isBreak) {
                    vibrate(VibeType.NORMAL)
                    nextStretch()
                } else {
                    if (currentStretch.isSplit && !WorkoutSession.isSplitSecondHalf) {
                        vibrate(VibeType.SHORT)
                        startSwitchSidesBreak(currentStretch)
                    } else {
                        val nextIndex = WorkoutSession.currentStretchIndex + 1
                        val nextStretch = WorkoutSession.routine?.stretches?.getOrNull(nextIndex)
                        if (nextStretch != null) {
                            vibrate(VibeType.NORMAL)
                            startBreak(nextStretch)
                        } else {
                            finishWorkout()
                        }
                    }
                }
            }
        }
    }

    fun nextStretch() {
        val nextIndex = WorkoutSession.currentStretchIndex + 1
        val nextStretch = WorkoutSession.routine?.stretches?.getOrNull(nextIndex)
        if (nextStretch != null) {
            WorkoutSession.currentStretchIndex = nextIndex
            startStretch(nextStretch, secondHalf = false)
        } else {
            finishWorkout()
        }
    }

    private fun finishWorkout() {
        WorkoutSession.isFinished = true
        vibrate(VibeType.LONG)
        
        sensorManager?.unregisterListener(this)

        val endTime = System.currentTimeMillis()
        val record = WorkoutRecord(
            id = UUID.randomUUID().toString(),
            routineName = WorkoutSession.routine?.name ?: "Unknown",
            startTimeMillis = WorkoutSession.startTimeMillis,
            endTimeMillis = endTime,
            heartRateSamples = WorkoutSession.heartRateSamples.toList(),
            stepCount = WorkoutSession.currentSteps,
            location = WorkoutSession.locationSnapshot,
            isSynced = false,
        )
        
        Log.d("WearStretch", "Saving workout: steps=${record.stepCount}, location=${record.location}")
        workoutRepository?.saveWorkout(record)
        
        serviceScope.launch {
            val success = messageSender?.syncRecord(record) ?: false
            if (success) {
                workoutRepository?.updateSyncStatus(record.id, true)
            }
            
            // Sync unsynced in background
            val unsynced = workoutRepository?.getUnsyncedWorkouts() ?: emptyList()
            unsynced.forEach { unsyncedRecord ->
                val syncSuccess = messageSender?.syncRecord(unsyncedRecord) ?: false
                if (syncSuccess) {
                    workoutRepository?.updateSyncStatus(unsyncedRecord.id, true)
                }
            }
            
            // Stop service after finishing work
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        serviceScope.cancel()
        sensorManager?.unregisterListener(this)
    }

    // Vibrations helper
    private enum class VibeType { SHORT, NORMAL, LONG }

    private fun vibrate(type: VibeType) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val duration = when (type) {
            VibeType.SHORT -> 200L
            VibeType.NORMAL -> 500L
            VibeType.LONG -> 1000L
        }
        vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    // Sensors listener
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_HEART_RATE) {
            val hr = event.values[0].toInt()
            if (hr > 0) {
                WorkoutSession.currentHeartRate = hr
                WorkoutSession.heartRateSamples.add(hr)
            }
        } else if (event?.sensor?.type == Sensor.TYPE_STEP_DETECTOR) {
            WorkoutSession.currentSteps++
            Log.d("WearStretch", "Step detected (service): ${WorkoutSession.currentSteps}")
        } else if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSteps = event.values[0].toInt()
            if (WorkoutSession.initialSteps == -1) {
                WorkoutSession.initialSteps = totalSteps
                Log.d("WearStretch", "Initial steps set (service): ${WorkoutSession.initialSteps}")
            }
            val counterSteps = totalSteps - WorkoutSession.initialSteps
            if (counterSteps > WorkoutSession.currentSteps) {
                WorkoutSession.currentSteps = counterSteps
                Log.d("WearStretch", "Current steps updated from counter (service): ${WorkoutSession.currentSteps}")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // Notifications setup
    fun startWorkoutNotification(routineName: String) {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Stretching")
            .setContentText("Routine: $routineName")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setOngoing(true)

        val status = Status.Builder()
            .addTemplate("Stretching: #routine#")
            .addPart("routine", Status.TextPart(routineName))
            .build()

        val ongoingActivity = OngoingActivity.Builder(
            applicationContext, notificationId, notificationBuilder
        )
            .setAnimatedIcon(R.drawable.ic_launcher_foreground)
            .setStaticIcon(R.drawable.ic_launcher_foreground)
            .setTouchIntent(pendingIntent)
            .setStatus(status)
            .build()

        ongoingActivity.apply(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                notificationId,
                notificationBuilder.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            )
        } else {
            startForeground(notificationId, notificationBuilder.build())
        }
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            channelId, "Workout Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }
}

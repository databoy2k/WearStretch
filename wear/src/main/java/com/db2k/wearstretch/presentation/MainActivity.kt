package com.db2k.wearstretch.presentation

import android.annotation.SuppressLint
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import com.db2k.wearstretch.data.RoutineStore
import com.db2k.wearstretch.data.SettingsStore
import com.db2k.wearstretch.data.WorkoutRepository
import com.db2k.wearstretch.model.DataLayerPaths
import com.db2k.wearstretch.model.Routine
import com.db2k.wearstretch.presentation.theme.WearStretchTheme
import com.google.android.gms.wearable.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream

@SuppressLint("VisibleForTests")
class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener {
    private val viewModel: WorkoutViewModel by viewModels()
    private val dataClient by lazy { Wearable.getDataClient(this) }
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        refreshRoutines()
        viewModel.syncUnsynced(this)

        setContent {
            WearStretchTheme {
                val context = LocalContext.current
                var currentScreen by remember { mutableStateOf(value = "routines") }
                var pendingRoutine by remember { mutableStateOf<Routine?>(null) }
                var showPermissionError by remember { mutableStateOf(false) }
                
                // Keep screen on while workout is active
                val window = (context as android.app.Activity).window
                DisposableEffect(viewModel.routine, viewModel.isFinished) {
                    if ((viewModel.routine != null) && !viewModel.isFinished) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    onDispose {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                val permissionsToRequest = remember(SettingsStore.trackHeartRate.value, SettingsStore.trackSteps.value, SettingsStore.trackLocation.value) {
                    val list = mutableListOf<String>()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        list.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    if (SettingsStore.trackHeartRate.value) {
                        list.add(Manifest.permission.BODY_SENSORS)
                    }
                    if (SettingsStore.trackSteps.value) {
                        list.add(Manifest.permission.ACTIVITY_RECOGNITION)
                    }
                    if (SettingsStore.trackLocation.value) {
                        list.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                    list
                }

                var activePermissionsToRequest by remember { mutableStateOf(emptyList<String>()) }
                var permissionRequestIndex by remember { mutableStateOf(-1) }

                var prevTrackHr by remember { mutableStateOf(SettingsStore.trackHeartRate.value) }
                var prevTrackSteps by remember { mutableStateOf(SettingsStore.trackSteps.value) }
                var prevTrackLocation by remember { mutableStateOf(SettingsStore.trackLocation.value) }

                LaunchedEffect(SettingsStore.trackHeartRate.value, SettingsStore.trackSteps.value, SettingsStore.trackLocation.value) {
                    val currentHr = SettingsStore.trackHeartRate.value
                    val currentSteps = SettingsStore.trackSteps.value
                    val currentLocation = SettingsStore.trackLocation.value

                    val permissionsToTrigger = mutableListOf<String>()

                    if (currentHr && !prevTrackHr && ContextCompat.checkSelfPermission(context, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToTrigger.add(Manifest.permission.BODY_SENSORS)
                    }
                    if (currentSteps && !prevTrackSteps && ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToTrigger.add(Manifest.permission.ACTIVITY_RECOGNITION)
                    }
                    if (currentLocation && !prevTrackLocation && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToTrigger.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }

                    prevTrackHr = currentHr
                    prevTrackSteps = currentSteps
                    prevTrackLocation = currentLocation

                    if (permissionsToTrigger.isNotEmpty()) {
                        activePermissionsToRequest = permissionsToTrigger
                        permissionRequestIndex = 0
                    }
                }

                val singlePermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { _ ->
                    permissionRequestIndex += 1
                }

                LaunchedEffect(permissionRequestIndex) {
                    if (permissionRequestIndex in activePermissionsToRequest.indices) {
                        val perm = activePermissionsToRequest[permissionRequestIndex]
                        if (ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED) {
                            permissionRequestIndex += 1
                        } else {
                            singlePermissionLauncher.launch(perm)
                        }
                    } else if (permissionRequestIndex >= activePermissionsToRequest.size) {
                        val hrGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED
                        val stepsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
                        val locationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        
                        var newHr = SettingsStore.trackHeartRate.value
                        var newSteps = SettingsStore.trackSteps.value
                        var newLoc = SettingsStore.trackLocation.value

                        if (activePermissionsToRequest.contains(Manifest.permission.BODY_SENSORS)) {
                            newHr = hrGranted
                        }
                        if (activePermissionsToRequest.contains(Manifest.permission.ACTIVITY_RECOGNITION)) {
                            newSteps = stepsGranted
                        }
                        if (activePermissionsToRequest.contains(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                            newLoc = locationGranted
                        }

                        // Avoid double LaunchedEffect trigger
                        prevTrackHr = newHr
                        prevTrackSteps = newSteps
                        prevTrackLocation = newLoc
                        
                        updateAndSyncSetting(newHr, newSteps, newLoc)

                        val hrNeeded = SettingsStore.trackHeartRate.value
                        val stepsNeeded = SettingsStore.trackSteps.value
                        val locationNeeded = SettingsStore.trackLocation.value

                        val hasHr = !hrNeeded || hrGranted
                        val hasSteps = !stepsNeeded || stepsGranted
                        val hasLoc = !locationNeeded || locationGranted

                        if (pendingRoutine != null) {
                            if (hasHr && hasSteps && hasLoc) {
                                showPermissionError = false
                                viewModel.startRoutine(context, pendingRoutine!!)
                                startWorkoutService(pendingRoutine!!.name)
                                pendingRoutine = null
                            } else {
                                showPermissionError = true
                            }
                        }
                        permissionRequestIndex = -1
                        activePermissionsToRequest = emptyList()
                    }
                }

                val startupNotificationLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) {}

                // Only request notifications on startup, others when workout starts
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            startupNotificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner, pendingRoutine) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            val hrGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED
                            val stepsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
                            val locationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            
                            var changed = false
                            var nextHr = SettingsStore.trackHeartRate.value
                            var nextSteps = SettingsStore.trackSteps.value
                            var nextLoc = SettingsStore.trackLocation.value

                            // If not granted, force to false. If granted, ensure it defaults to true
                            if (!hrGranted && nextHr) {
                                nextHr = false
                                changed = true
                            } else if (hrGranted && !nextHr) {
                                nextHr = true
                                changed = true
                            }

                            if (!stepsGranted && nextSteps) {
                                nextSteps = false
                                changed = true
                            } else if (stepsGranted && !nextSteps) {
                                nextSteps = true
                                changed = true
                            }

                            if (!locationGranted && nextLoc) {
                                nextLoc = false
                                changed = true
                            } else if (locationGranted && !nextLoc) {
                                nextLoc = true
                                changed = true
                            }

                            if (changed) {
                                prevTrackHr = nextHr
                                prevTrackSteps = nextSteps
                                prevTrackLocation = nextLoc
                                updateAndSyncSetting(nextHr, nextSteps, nextLoc)
                            }

                            val hrNeeded = SettingsStore.trackHeartRate.value
                            val stepsNeeded = SettingsStore.trackSteps.value
                            val locationNeeded = SettingsStore.trackLocation.value

                            val hasHr = !hrNeeded || hrGranted
                            val hasSteps = !stepsNeeded || stepsGranted
                            val hasLoc = !locationNeeded || locationGranted

                            if (hasHr && hasSteps && hasLoc) {
                                if (showPermissionError) {
                                    showPermissionError = false
                                    if (pendingRoutine != null) {
                                        viewModel.startRoutine(context, pendingRoutine!!)
                                        startWorkoutService(pendingRoutine!!.name)
                                        pendingRoutine = null
                                    }
                                }
                            }
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                AppScaffold {
                    if (showPermissionError) {
                        BackHandler {
                            showPermissionError = false
                            pendingRoutine = null
                        }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Sensor permission is hidden or restricted.",
                                    style = MaterialTheme.typography.titleSmall,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "1. Tap 'Open Settings'\n2. Scroll to the very bottom\n3. Look for 'Restricted Setting' or 3-dots at top\n4. Select 'Allow restricted settings'",
                                    style = MaterialTheme.typography.bodyExtraSmall,
                                    textAlign = TextAlign.Start
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = {
                                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = android.net.Uri.fromParts("package", packageName, null)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    startActivity(intent)
                                }) {
                                    Text("Open Settings")
                                }
                                TextButton(onClick = {
                                    showPermissionError = false
                                    pendingRoutine = null
                                }) {
                                    Text("Exit to Routines")
                                }
                            }
                        }
                    } else if (viewModel.isFinished) {
                        LaunchedEffect(Unit) { stopWorkoutService() }
                        BackHandler {
                            viewModel.cancelWorkout(context)
                        }
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "Workout Finished!", style = MaterialTheme.typography.titleLarge)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { 
                                    viewModel.cancelWorkout(context)
                                    finish() 
                                }) {
                                    Text(text = "Sync and Close App")
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(onClick = {
                                    viewModel.cancelWorkout(context)
                                }) {
                                    Text(text = "Exit to Routines")
                                }
                            }
                        }
                    } else if (viewModel.routine != null) {
                        val routine = viewModel.routine!!
                        val isBreak = viewModel.isBreak
                        val currentStretch = routine.stretches[viewModel.currentStretchIndex]
                        
                        // During a break, show the UPCOMING stretch's info
                        val displayStretch = if (isBreak) {
                            routine.stretches.getOrNull(viewModel.currentStretchIndex + 1) ?: currentStretch
                        } else {
                            currentStretch
                        }
                        
                        WorkoutScreen(
                            stretch = displayStretch,
                            timeLeft = viewModel.timeLeftSeconds,
                            isPaused = viewModel.isPaused,
                            isBreak = isBreak,
                            isSwitchingSides = viewModel.isSwitchingSides,
                            isStarting = viewModel.isStarting,
                            isSplitSecondHalf = viewModel.isSplitSecondHalf,
                            nextStretchName = viewModel.nextStretchName,
                            currentHeartRate = viewModel.currentHeartRate,
                            currentSteps = viewModel.currentSteps,
                            currentLocation = viewModel.locationSnapshot,
                            onPauseToggle = { viewModel.togglePause() },
                            onSkip = { viewModel.nextStretch(context) },
                            onCancel = {
                                viewModel.cancelWorkout(context)
                                stopWorkoutService()
                            }
                        )
                    } else {
                        when (currentScreen) {
                            "history" -> WorkoutHistoryScreen(
                                onBack = { currentScreen = "routines" },
                                onSyncClick = { viewModel.forceSyncAll(context) }
                            )
                            else -> RoutineSelectionScreen(
                                onRoutineSelected = { routine ->
                                    val missingPermissions = permissionsToRequest.filter {
                                        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                                    }
                                    
                                    if (missingPermissions.isNotEmpty()) {
                                        pendingRoutine = routine
                                        activePermissionsToRequest = missingPermissions
                                        permissionRequestIndex = 0
                                    } else {
                                        viewModel.startRoutine(context, routine)
                                        startWorkoutService(routine.name)
                                    }
                                },
                                onHistoryClick = { currentScreen = "history" }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        dataClient.addListener(this)
        viewModel.syncUnsynced(this)
    }

    override fun onPause() {
        super.onPause()
        dataClient.removeListener(this)
    }

    private fun updateAndSyncSetting(
        trackHr: Boolean,
        trackSteps: Boolean,
        trackLocation: Boolean
    ) {
        SettingsStore.trackHeartRate.value = trackHr
        SettingsStore.trackSteps.value = trackSteps
        SettingsStore.trackLocation.value = trackLocation

        lifecycleScope.launch {
            try {
                val putDataMapReq = PutDataMapRequest.create(DataLayerPaths.SETTINGS)
                val dataMap = putDataMapReq.dataMap
                dataMap.putBoolean("track_hr", trackHr)
                dataMap.putBoolean("track_location", trackLocation)
                dataMap.putBoolean("track_steps", trackSteps)
                dataMap.putBoolean("show_animations", SettingsStore.showAnimations.value)
                dataMap.putInt("start_delay", SettingsStore.startDelaySeconds.value)
                dataMap.putString("mobile_version", SettingsStore.mobileVersion.value)
                dataMap.putLong("timestamp", System.currentTimeMillis())
                
                val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
                dataClient.putDataItem(putDataReq).await()
                Log.d("WearStretch", "Synced settings back to phone: hr=$trackHr, steps=$trackSteps, location=$trackLocation")
            } catch (e: Exception) {
                Log.e("WearStretch", "Failed to sync settings back to phone", e)
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        try {
            if (viewModel.routine != null) return

            for (event in dataEvents) {
                if (event.type == DataEvent.TYPE_CHANGED && (event.dataItem.uri.path == DataLayerPaths.ROUTINE_LIST)) {
                    lifecycleScope.launch {
                        processRoutinesDataItem(event.dataItem)
                    }
                }
            }
        } finally {
            dataEvents.release()
        }
    }

    private fun refreshRoutines() {
        lifecycleScope.launch {
            try {
                val dataItems = dataClient.dataItems.await()
                for (item in dataItems) {
                    if (item.uri.path == DataLayerPaths.ROUTINE_LIST) {
                        processRoutinesDataItem(item)
                    } else if (item.uri.path?.startsWith("/routines/") == true) {
                        dataClient.deleteDataItems(item.uri)
                    }
                }
            } catch (_: Exception) {
            Log.e("WearStretch", "Manual refresh failed")
        }
        }
    }

    @SuppressLint("DiscouragedApi")
    private suspend fun processRoutinesDataItem(item: DataItem) {
        val dataMap = DataMapItem.fromDataItem(item).dataMap
        val json = dataMap.getString("routines_json") ?: return
        val type = object : TypeToken<List<Routine>>() {}.type
        val routines: List<Routine> = gson.fromJson(json, type)
        
        val updatedRoutines = routines.map { routine ->
            routine.copy(stretches = routine.stretches.map { stretch ->
                val assetKey = "asset_${stretch.id}"
                if (dataMap.containsKey(assetKey)) {
                    val asset = dataMap.getAsset(assetKey)
                    val localPath = saveAssetToLocalFile(asset, "stretch_${stretch.id}.png")
                    stretch.copy(customImageUri = localPath)
                } else {
                    val imageKey = stretch.imageKey
                    if (imageKey != null) {
                        val resId = resources.getIdentifier(imageKey, "drawable", packageName)
                        if (resId == 0) stretch.copy(imageKey = null) else stretch
                    } else {
                        stretch
                    }
                }
            })
        }
        
        RoutineStore.receivedRoutines.clear()
        RoutineStore.receivedRoutines.addAll(updatedRoutines)
    }

    private suspend fun saveAssetToLocalFile(asset: Asset?, fileName: String): String? {
        if (asset == null) return null
        return try {
            val inputStream = dataClient.getFdForAsset(asset).await().inputStream
            val file = File(filesDir, fileName)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                FileOutputStream(file).use { output ->
                    inputStream.copyTo(output)
                }
            }
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    private fun startWorkoutService(routineName: String) {
        val intent = Intent(this, WorkoutService::class.java).apply {
            putExtra("routine_name", routineName)
        }
        startForegroundService(intent)
    }

    private fun stopWorkoutService() {
        stopService(Intent(this, WorkoutService::class.java))
    }
}


package com.db2k.wearstretch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.db2k.wearstretch.health.HealthConnectManager
import com.db2k.wearstretch.ui.DashboardScreen
import com.db2k.wearstretch.ui.LibraryScreen
import com.db2k.wearstretch.ui.routine.RoutineBuilderScreen
import com.db2k.wearstretch.ui.routine.RoutineListScreen
import com.db2k.wearstretch.ui.routine.MainViewModel
import com.db2k.wearstretch.ui.theme.WearStretchTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val healthConnectManager by lazy { HealthConnectManager(this) }
    private val mainViewModel: MainViewModel by viewModels()

    private val requestHealthPermissionContract = PermissionController.createRequestPermissionResultContract()

    private val requestHealthPermissions = registerForActivityResult(requestHealthPermissionContract) { _ ->
        lifecycleScope.launch {
            if (healthConnectManager.hasAllPermissions()) {
                fetchRecentWorkouts()
            }
        }
    }

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mainViewModel.init(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            var recentWorkouts by remember { mutableStateOf<List<ExerciseSessionRecord>>(value = emptyList()) }
            var currentScreen by remember { mutableStateOf(value = "routines") }
            var isBuildingRoutine by remember { mutableStateOf(value = false) }
            var showExitPrompt by remember { mutableStateOf(value = false) }
            var showHcUpdateDialog by remember { mutableStateOf(value = false) }

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        mainViewModel.reloadSettings(this@MainActivity)
                        lifecycleScope.launch {
                            try {
                                if (HealthConnectClient.getSdkStatus(this@MainActivity) == HealthConnectClient.SDK_AVAILABLE) {
                                    if (healthConnectManager.hasReadPermission()) {
                                        recentWorkouts = healthConnectManager.readRecentStretchingWorkouts()
                                    }
                                }
                            } catch (_: Exception) { }
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            LaunchedEffect(Unit) {
                try {
                    val sdkStatus = HealthConnectClient.getSdkStatus(this@MainActivity)
                    android.util.Log.d("WearStretchHC", "Health Connect SDK status: $sdkStatus")
                    if (sdkStatus == HealthConnectClient.SDK_AVAILABLE) {
                        val hasAll = healthConnectManager.hasAllPermissions()
                        android.util.Log.d("WearStretchHC", "Has all HC permissions: $hasAll")
                        if (!hasAll) {
                            requestHealthPermissions.launch(healthConnectManager.permissions)
                        }
                    } else if (sdkStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
                        showHcUpdateDialog = true
                    }
                } catch (e: Exception) {
                    android.util.Log.e("WearStretchHC", "Failed checking/requesting HC permissions", e)
                }
            }

            WearStretchTheme {
                if (showHcUpdateDialog) {
                    AlertDialog(
                        onDismissRequest = { showHcUpdateDialog = false },
                        title = { Text(text = "Health Connect Update Required") },
                        text = { Text(text = "WearStretch needs the Health Connect app to be updated to sync your workouts and heart rate data. Would you like to update it now?") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showHcUpdateDialog = false
                                    try {
                                        val providerPackageName = "com.google.android.apps.healthdata"
                                        val uriString = "market://details?id=$providerPackageName&url=healthconnect%3A%2F%2Fonboarding"
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setPackage("com.android.vending")
                                            data = Uri.parse(uriString)
                                            putExtra("overlay", true)
                                            putExtra("callerId", packageName)
                                        }
                                        startActivity(intent)
                                    } catch (e: Exception) {
                                        try {
                                            val providerPackageName = "com.google.android.apps.healthdata"
                                            val webUri = Uri.parse("https://play.google.com/store/apps/details?id=$providerPackageName")
                                            startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                        } catch (_: Exception) {}
                                    }
                                }
                            ) {
                                Text(text = "Update")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showHcUpdateDialog = false }) {
                                Text(text = "Cancel")
                            }
                        }
                    )
                }

                if (isBuildingRoutine) {
                    BackHandler {
                        showExitPrompt = true
                    }
                    
                    if (showExitPrompt) {
                        AlertDialog(
                            onDismissRequest = { showExitPrompt = false },
                            title = { Text(text = "Unsaved Changes") },
                            text = { Text(text = "Do you want to save your routine before leaving?") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        mainViewModel.saveAndSyncRoutine {
                                            isBuildingRoutine = false
                                            currentScreen = "routines"
                                            showExitPrompt = false
                                        }
                                    }
                                ) { 
                                    Text(text = "Save") 
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    mainViewModel.cancelEditingRoutine()
                                    isBuildingRoutine = false
                                    showExitPrompt = false
                                }) {
                                    Text(text = "Discard")
                                }
                            }
                        )
                    }

                    RoutineBuilderScreen(mainViewModel) {
                        isBuildingRoutine = false
                    }
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(
                                    icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
                                    label = { Text(text = "Library") },
                                    selected = currentScreen == "library",
                                    onClick = { currentScreen = "library" }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                                    label = { Text(text = "Routines") },
                                    selected = currentScreen == "routines",
                                    onClick = { currentScreen = "routines" }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                                    label = { Text(text = "Me") },
                                    selected = currentScreen == "me",
                                    onClick = { currentScreen = "me" }
                                )
                            }
                        }
                    ) { innerPadding ->
                        Surface(
                            modifier = Modifier.padding(innerPadding),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            when (currentScreen) {
                                "library" -> LibraryScreen(viewModel = mainViewModel)
                                "routines" -> RoutineListScreen(
                                    viewModel = mainViewModel,
                                    onEditRoutine = { routine ->
                                        mainViewModel.startEditingRoutine(routine)
                                        isBuildingRoutine = true
                                    },
                                    onAddRoutine = {
                                        mainViewModel.cancelEditingRoutine()
                                        isBuildingRoutine = true
                                    }
                                )
                                "me" -> DashboardScreen(
                                    viewModel = mainViewModel,
                                    recentWorkouts = recentWorkouts,
                                    onRefresh = {
                                        lifecycleScope.launch {
                                            if (healthConnectManager.hasReadPermission()) {
                                                recentWorkouts = healthConnectManager.readRecentStretchingWorkouts()
                                            }
                                        }
                                    },
                                    onClearData = {
                                        lifecycleScope.launch {
                                            try {
                                                healthConnectManager.deleteAllStretchingWorkouts()
                                                recentWorkouts = emptyList()
                                            } catch (_: Exception) { }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun fetchRecentWorkouts() {
        try {
            mainViewModel.init(this)
        } catch (_: Exception) { }
    }
}

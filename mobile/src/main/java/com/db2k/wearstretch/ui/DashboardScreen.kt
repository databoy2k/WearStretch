package com.db2k.wearstretch.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.health.connect.client.records.ExerciseSessionRecord
import com.db2k.wearstretch.health.HealthConnectManager
import com.db2k.wearstretch.health.WorkoutDetails
import com.db2k.wearstretch.ui.routine.ImportMode
import com.db2k.wearstretch.ui.routine.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("UNUSED_VALUE")
fun DashboardScreen(
    viewModel: MainViewModel,
    recentWorkouts: List<ExerciseSessionRecord>,
    onRefresh: () -> Unit,
    onClearData: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val healthConnectManager = remember { HealthConnectManager(context) }
    
    val showImportDialogState = remember { mutableStateOf(value = false) }
    val showExportDialogState = remember { mutableStateOf(value = false) }
    val importedJsonState = remember { mutableStateOf<String?>(value = null) }
    
    var selectedWorkout by remember { mutableStateOf<ExerciseSessionRecord?>(null) }
    var selectedWorkoutDetails by remember { mutableStateOf<WorkoutDetails?>(null) }
    var isLoadingDetails by remember { mutableStateOf(false) }

    var settingsExpanded by remember { mutableStateOf(value = false) }
    var historyExpanded by remember { mutableStateOf(value = true) }

    if (selectedWorkout != null) {
        WorkoutDetailsDialog(
            workout = selectedWorkout!!,
            details = selectedWorkoutDetails,
            isLoading = isLoadingDetails,
            onDismiss = {
                selectedWorkout = null
                selectedWorkoutDetails = null
            },
            onDelete = {
                scope.launch {
                    try {
                        healthConnectManager.deleteWorkout(selectedWorkout!!)
                        selectedWorkout = null
                        selectedWorkoutDetails = null
                        onRefresh()
                    } catch (_: Exception) {
                        Toast.makeText(context, "Failed to delete workout", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                val data = viewModel.getExportData()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        stream.write(data.toByteArray())
                    }
                }
                Toast.makeText(context, "Export saved!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val json = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
            if (json != null) {
                importedJsonState.value = json
                showImportDialogState.value = true
            }
        }
    }

    fun shareExportData() {
        val data = viewModel.getExportData()
        val file = File(context.cacheDir, "wear_stretch_export.json")
        file.writeText(data)
        
        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, contentUri)
            type = "application/json"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Routine Export"))
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Me") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Your Stretching Summary", style = MaterialTheme.typography.headlineMedium)
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            }

            item {
                ExpandableMeCard(
                    title = "Recent Sessions",
                    isExpanded = historyExpanded,
                    onToggle = { historyExpanded = !historyExpanded }
                ) {
                    if (recentWorkouts.isEmpty()) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.History, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "No workouts recorded in the last 30 days.")
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            recentWorkouts.forEach { workout ->
                                WorkoutSessionCard(workout) {
                                    selectedWorkout = workout
                                    isLoadingDetails = true
                                    scope.launch {
                                        selectedWorkoutDetails = healthConnectManager.getWorkoutDetails(workout.startTime, workout.endTime)
                                        isLoadingDetails = false
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                ExpandableMeCard(
                    title = "Settings",
                    isExpanded = settingsExpanded,
                    onToggle = { settingsExpanded = !settingsExpanded }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Track Heart Rate", modifier = Modifier.weight(1f))
                            Switch(
                                checked = viewModel.settings.value.trackHeartRate,
                                onCheckedChange = { viewModel.updateSettings(viewModel.settings.value.copy(trackHeartRate = it)) }
                            )
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Track Location", modifier = Modifier.weight(1f))
                            Switch(
                                checked = viewModel.settings.value.trackLocation,
                                onCheckedChange = { viewModel.updateSettings(viewModel.settings.value.copy(trackLocation = it)) }
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Track Steps", modifier = Modifier.weight(1f))
                            Switch(
                                checked = viewModel.settings.value.trackSteps,
                                onCheckedChange = { viewModel.updateSettings(viewModel.settings.value.copy(trackSteps = it)) }
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Show Animations", modifier = Modifier.weight(1f))
                            Switch(
                                checked = viewModel.settings.value.showAnimations,
                                onCheckedChange = { viewModel.updateSettings(viewModel.settings.value.copy(showAnimations = it)) }
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Start Delay")
                                Text(text = "${viewModel.settings.value.startDelaySeconds} seconds", style = MaterialTheme.typography.bodySmall)
                            }
                            Slider(
                                value = viewModel.settings.value.startDelaySeconds.toFloat(),
                                onValueChange = { viewModel.updateSettings(viewModel.settings.value.copy(startDelaySeconds = it.toInt())) },
                                valueRange = 0f..60f,
                                steps = 59,
                                modifier = Modifier.width(120.dp)
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        Text(text = "Data Management", style = MaterialTheme.typography.titleMedium)
                        Text(text = "Tip: Export your data first to get a sample JSON file you can edit in bulk.", style = MaterialTheme.typography.bodySmall)
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showExportDialogState.value = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Export")
                            }
                            Button(
                                onClick = { importLauncher.launch("application/json") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Import")
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        
                        Button(
                            onClick = onClearData,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Clear Health Connect Data")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { viewModel.resetToPresets() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(text = "Reset All Routines & Stretches")
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Wear Stretch v${com.db2k.wearstretch.BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        }
    }

    if (showExportDialogState.value) {
        AlertDialog(
            onDismissRequest = { 
                showExportDialogState.value = false 
            },
            title = { Text(text = "Export Data") },
            text = { Text(text = "Would you like to save the backup file locally or share it to another app (Email, Drive, etc.)?") },
            confirmButton = {
                Button(onClick = { 
                    showExportDialogState.value = false
                    exportLauncher.launch("wear_stretch_backup.json") 
                }) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Save Locally")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showExportDialogState.value = false
                    shareExportData() 
                }) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Share")
                }
            }
        )
    }

    val importedJson = importedJsonState.value
    if (showImportDialogState.value && (importedJson != null)) {
        val json = importedJson
        ImportOptionsDialog(
            onDismiss = { 
                showImportDialogState.value = false 
            },
            onImport = { mode ->
                viewModel.importData(json, mode)
                showImportDialogState.value = false
                Toast.makeText(context, "Import complete!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun ExpandableMeCard(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun WorkoutSessionCard(workout: ExerciseSessionRecord, onClick: () -> Unit) {
    val duration = Duration.between(workout.startTime, workout.endTime)
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
    val startTimeStr = formatter.format(workout.startTime.atZone(java.time.ZoneId.systemDefault()))

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = workout.title ?: "Stretching Session", style = MaterialTheme.typography.titleMedium)
            Text(text = startTimeStr, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Duration: ${duration.toMinutes()}m ${duration.seconds % 60}s",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun WorkoutDetailsDialog(
    workout: ExerciseSessionRecord,
    details: WorkoutDetails?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = workout.title ?: "Workout Details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (details != null) {
                    DetailRow(icon = Icons.Default.Favorite, label = "Avg Heart Rate", value = "${details.avgHeartRate} bpm")
                    DetailRow(icon = Icons.AutoMirrored.Filled.TrendingUp, label = "Max Heart Rate", value = "${details.maxHeartRate} bpm")
                    DetailRow(icon = Icons.AutoMirrored.Filled.DirectionsWalk, label = "Total Steps", value = "${details.totalSteps}")
                    
                    if (!workout.notes.isNullOrEmpty()) {
                        DetailRow(icon = Icons.Default.LocationOn, label = "Location", value = workout.notes!!)
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}

@Composable
fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun ImportOptionsDialog(onDismiss: () -> Unit, onImport: (ImportMode) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Import Options") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Choose how you want to import the stretches and routines:")
                Button(onClick = { onImport(ImportMode.ADD_ONLY) }, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Add Only (Keep existing)")
                }
                Button(onClick = { onImport(ImportMode.OVERWRITE) }, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Overwrite (Update existing IDs)")
                }
                Button(onClick = { onImport(ImportMode.REMOVE_MISSING) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text(text = "Full Sync (Remove items not in file)")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = "Cancel") }
        }
    )
}

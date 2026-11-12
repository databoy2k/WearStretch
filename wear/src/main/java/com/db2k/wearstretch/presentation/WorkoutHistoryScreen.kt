package com.db2k.wearstretch.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import com.db2k.wearstretch.data.WorkoutRepository

@Composable
fun WorkoutHistoryScreen(onBack: () -> Unit, onSyncClick: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { WorkoutRepository(context) }
    var history by remember { mutableStateOf(value = repository.getHistory()) }
    val listState = rememberTransformingLazyColumnState()
    
    BackHandler {
        onBack()
    }
    
    // Refresh history when screen is shown or when a sync might have happened
    LaunchedEffect(Unit) {
        history = repository.getHistory()
    }

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = contentPadding
        ) {
            item {
                ListHeader { Text(text = "History") }
            }

            if (history.isEmpty()) {
                item {
                    Text(text = "No recent workouts.", modifier = Modifier.padding(16.dp))
                }
            } else {
                items(history) { record ->
                    TitleCard(
                        onClick = { /* No-op */ },
                        title = { Text(text = record.routineName) },
                        subtitle = {
                            // When + how long, e.g. "Today 7:05 PM · 6m 12s" (was only "372s").
                            Text(text = historySubtitle(record.startTimeMillis, record.endTimeMillis))
                        }
                    ) {
                        if (record.isSynced) {
                            Icon(Icons.Default.CloudDone, contentDescription = "Sent to phone", tint = Color.Green)
                        } else {
                            Icon(Icons.Default.CloudOff, contentDescription = "Not sent yet", tint = Color.LightGray)
                        }
                    }
                }
                
                item {
                    Button(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        onClick = {
                            onSyncClick()
                            // Refresh so any newly-published records update their icon
                            history = repository.getHistory()
                        }
                    ) {
                        Text(text = "Resend to Phone")
                    }
                }
            }

            item {
                TextButton(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    onClick = onBack
                ) {
                    Text(text = "Back")
                }
            }
        }
    }
}

internal fun historySubtitle(startMillis: Long, endMillis: Long, now: java.time.LocalDate = java.time.LocalDate.now()): String {
    val zone = java.time.ZoneId.systemDefault()
    val start = java.time.Instant.ofEpochMilli(startMillis).atZone(zone)
    val day = when (start.toLocalDate()) {
        now -> "Today"
        now.minusDays(1) -> "Yesterday"
        else -> start.format(java.time.format.DateTimeFormatter.ofPattern("EEE MMM d"))
    }
    val time = start.format(java.time.format.DateTimeFormatter.ofLocalizedTime(java.time.format.FormatStyle.SHORT))
    return "$day $time · ${formatDuration((endMillis - startMillis) / 1000)}"
}

internal fun formatDuration(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0)
    return if (s >= 60) "${s / 60}m ${s % 60}s" else "${s}s"
}

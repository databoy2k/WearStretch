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
                            val duration = (record.endTimeMillis - record.startTimeMillis) / 1000
                            Text(text = "${duration}s")
                        }
                    ) {
                        if (record.isSynced) {
                            Icon(Icons.Default.CloudDone, contentDescription = "Synced", tint = Color.Green)
                        } else {
                            Icon(Icons.Default.CloudOff, contentDescription = "Not Synced", tint = Color.LightGray)
                        }
                    }
                }
                
                item {
                    Button(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        onClick = {
                            onSyncClick()
                            // Refresh history after a short delay to see updated sync status
                            history = repository.getHistory()
                        }
                    ) {
                        Text(text = "Retry Sync")
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

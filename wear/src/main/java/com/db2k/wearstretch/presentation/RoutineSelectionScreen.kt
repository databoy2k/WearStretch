package com.db2k.wearstretch.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import com.db2k.wearstretch.data.RoutineStore
import com.db2k.wearstretch.model.Routine

@Composable
fun RoutineSelectionScreen(onRoutineSelected: (Routine) -> Unit, onHistoryClick: () -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    
    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = contentPadding
        ) {
            item {
                ListHeader { Text(text = "Pick a Routine") }
            }
            
            if (RoutineStore.receivedRoutines.isEmpty()) {
                item {
                    Text(text = "No routines found. Sync from your phone!", modifier = Modifier.padding(16.dp))
                }
            } else {
                items(RoutineStore.receivedRoutines) { routine ->
                    TitleCard(
                        onClick = { onRoutineSelected(routine) },
                        title = { Text(text = routine.name) },
                        subtitle = { Text(text = "${routine.stretches.size} stretches") }
                    )
                }
            }

            item {
                Button(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    onClick = onHistoryClick
                ) {
                    Text(text = "Workout History")
                }
            }

            item {
                Text(
                    text = "v${com.db2k.wearstretch.BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyExtraSmall,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

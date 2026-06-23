package com.db2k.wearstretch.ui.routine

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.db2k.wearstretch.model.Routine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineListScreen(
    viewModel: MainViewModel,
    onEditRoutine: (Routine) -> Unit,
    onAddRoutine: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Routines") },
                actions = {
                    TextButton(onClick = { viewModel.clearAllDataLayer() }) {
                        Text("Reset Sync")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRoutine) {
                Icon(Icons.Default.Add, contentDescription = "Create Routine")
            }
        }
    ) { padding ->
        if (viewModel.routines.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No routines yet. Tap + to build one!")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.routines) { routine ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onEditRoutine(routine) }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(routine.name, style = MaterialTheme.typography.titleMedium)
                                Text("${routine.stretches.size} stretches", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { viewModel.deleteRoutine(routine) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Routine")
                            }
                        }
                    }
                }
            }
        }
    }
}

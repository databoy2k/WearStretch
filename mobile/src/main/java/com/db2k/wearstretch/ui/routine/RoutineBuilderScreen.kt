package com.db2k.wearstretch.ui.routine

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.db2k.wearstretch.data.PresetData
import com.db2k.wearstretch.model.Stretch
import com.db2k.wearstretch.ui.CategoryHeader
import com.db2k.wearstretch.ui.StretchEditDialog

@SuppressLint("DiscouragedApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("UNUSED_VALUE")
fun RoutineBuilderScreen(viewModel: MainViewModel, onSynced: () -> Unit) {
    val pkgName = LocalContext.current.packageName
    val currentContext = LocalContext.current
    val stretchToEditState = remember { mutableStateOf<Pair<Int, Stretch>?>(value = null) }
    
    var searchQuery by remember { mutableStateOf(value = "") }
    var isSearching by remember { mutableStateOf(value = false) }
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }

    // Initialize all categories as collapsed by default
    LaunchedEffect(Unit) {
        PresetData.presetCategories.forEach { expandedCategories[it] = false }
        expandedCategories["Other"] = false
    }

    val imageLoader = remember {
        ImageLoader.Builder(currentContext)
            .components {
                add(ImageDecoderDecoder.Factory())
                add(GifDecoder.Factory())
            }
            .build()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (isSearching) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(text = "Search available stretches...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    } else {
                        Text(text = if (viewModel.editingRoutine.value != null) "Edit Routine" else "Routine Builder") 
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        isSearching = !isSearching
                        if (!isSearching) searchQuery = ""
                    }) {
                        Icon(if (isSearching) Icons.Default.Close else Icons.Default.Search, contentDescription = "Search")
                    }
                    if (!isSearching) {
                        if (viewModel.editingRoutine.value != null) {
                            TextButton(onClick = {
                                viewModel.cancelEditingRoutine()
                                onSynced()
                            }) {
                                Text(text = "Cancel")
                            }
                        }
                        IconButton(onClick = { viewModel.saveAndSyncRoutine(onSynced) }) {
                            Icon(Icons.Default.Done, contentDescription = "Save and Sync")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            if (!isSearching) {
                TextField(
                    value = viewModel.currentRoutineName.value,
                    onValueChange = { viewModel.currentRoutineName.value = it },
                    label = { Text(text = "Routine Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = viewModel.defaultBreakDuration.value,
                        onValueChange = { viewModel.defaultBreakDuration.value = it },
                        label = { Text(text = "Global Transition Time (s)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val targetBreakTime = viewModel.defaultBreakDuration.value.toIntOrNull()
                    val isApplyAllEnabled = targetBreakTime != null &&
                            viewModel.selectedStretches.isNotEmpty() &&
                            viewModel.selectedStretches.any { it.breakDurationSeconds != targetBreakTime }
                    Button(
                        onClick = {
                            val time = viewModel.defaultBreakDuration.value.toIntOrNull() ?: 5
                            viewModel.applyGlobalBreakTimeToAll(time)
                        },
                        enabled = isApplyAllEnabled
                    ) {
                        Text(text = "Apply All")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(text = "Selected Stretches (${viewModel.selectedStretches.size})", style = MaterialTheme.typography.titleMedium)
                
                val selectedListModifier = if (viewModel.selectedStretches.size > 3) Modifier.height(200.dp) else Modifier.wrapContentHeight()
                
                LazyColumn(
                    modifier = selectedListModifier,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(viewModel.selectedStretches) { index, stretch ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { stretchToEditState.value = index to stretch }
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Reorder controls: Drag Handle + Up/Down Buttons
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.DragHandle,
                                        contentDescription = "Drag to reorder",
                                        modifier = Modifier
                                            .padding(bottom = 4.dp)
                                            .pointerInput(index) {
                                                var verticalDrag = 0f
                                                detectDragGesturesAfterLongPress(
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        verticalDrag += dragAmount.y
                                                        if (verticalDrag > 100f && index < viewModel.selectedStretches.size - 1) {
                                                            viewModel.moveStretch(index, index + 1)
                                                            verticalDrag = 0f
                                                        } else if (verticalDrag < -100f && index > 0) {
                                                            viewModel.moveStretch(index, index - 1)
                                                            verticalDrag = 0f
                                                        }
                                                    },
                                                    onDragEnd = { verticalDrag = 0f },
                                                    onDragCancel = { verticalDrag = 0f }
                                                )
                                            }
                                    )
                                    Row {
                                        IconButton(
                                            onClick = { if (index > 0) viewModel.moveStretch(index, index - 1) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowUpward, contentDescription = "Up", modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = { if (index < viewModel.selectedStretches.size - 1) viewModel.moveStretch(index, index + 1) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowDownward, contentDescription = "Down", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                if (stretch.imageKey != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(
                                            model = ImageRequest.Builder(currentContext)
                                                .data("android.resource://$pkgName/drawable/${stretch.imageKey}")
                                                .build(),
                                            imageLoader = imageLoader
                                        ),
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.size(32.dp).padding(end = 8.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = stretch.name, style = MaterialTheme.typography.bodyMedium)
                                }
                                IconButton(onClick = { viewModel.removeStretchFromRoutine(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            }
            
            Text(text = "Available Stretches", style = MaterialTheme.typography.titleMedium)
            
            val filteredStretches = viewModel.libraryStretches.filter {
                it.name.contains(searchQuery, ignoreCase = true) || 
                it.description.contains(searchQuery, ignoreCase = true)
            }
            val groupedStretches = filteredStretches.groupBy { it.category }
            val sortedCategories = PresetData.presetCategories + "Other"

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                sortedCategories.forEach { category ->
                    val categoryStretches = groupedStretches[category] ?: emptyList()
                    if (categoryStretches.isNotEmpty()) {
                        item {
                            CategoryHeader(
                                name = category,
                                isExpanded = expandedCategories[category] ?: false,
                                onToggle = { expandedCategories[category] = !(expandedCategories[category] ?: false) }
                            )
                        }

                        if (expandedCategories[category] == true) {
                            items(categoryStretches) { stretch ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (stretch.imageKey != null) {
                                            Image(
                                                painter = rememberAsyncImagePainter(
                                                    model = ImageRequest.Builder(currentContext)
                                                        .data("android.resource://$pkgName/drawable/${stretch.imageKey}")
                                                        .build(),
                                                    imageLoader = imageLoader
                                                ),
                                                contentDescription = null,
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier.size(40.dp).padding(end = 8.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = stretch.name, style = MaterialTheme.typography.bodyLarge)
                                            Text(text = "${stretch.durationSeconds}s", style = MaterialTheme.typography.bodySmall)
                                        }
                                        IconButton(onClick = { viewModel.addStretchToRoutine(stretch) }) {
                                            Icon(Icons.Default.Add, contentDescription = "Add")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val currentStretchToEdit = stretchToEditState.value
    if (currentStretchToEdit != null) {
        StretchEditDialog(
            stretch = currentStretchToEdit.second,
            onDismiss = { 
                stretchToEditState.value = null 
            },
            onSave = { updated ->
                viewModel.selectedStretches[currentStretchToEdit.first] = updated
                stretchToEditState.value = null
            }
        )
    }
}

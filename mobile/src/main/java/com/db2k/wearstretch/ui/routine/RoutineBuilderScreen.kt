package com.db2k.wearstretch.ui.routine

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.db2k.wearstretch.data.PresetData
import com.db2k.wearstretch.model.Stretch
import com.db2k.wearstretch.ui.CategoryHeader
import com.db2k.wearstretch.ui.StretchEditDialog
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@SuppressLint("DiscouragedApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineBuilderScreen(viewModel: MainViewModel, onSynced: () -> Unit) {
    val pkgName = LocalContext.current.packageName
    val currentContext = LocalContext.current
    val stretchToEditState = remember { mutableStateOf<Pair<Int, Stretch>?>(value = null) }
    var showAddDialog by remember { mutableStateOf(value = false) }

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
                    Text(text = if (viewModel.editingRoutine.value != null) "Edit Routine" else "Routine Builder")
                },
                actions = {
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
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(text = "Add Stretches") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
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
            Text(
                text = "Long-press and drag to reorder",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (viewModel.selectedStretches.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No stretches yet.\nTap \"Add Stretches\" to build your routine.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                val scope = rememberCoroutineScope()
                var overscrollJob by remember { mutableStateOf<Job?>(null) }
                val dragDropState = rememberDragDropListState(onMove = viewModel::moveStretch)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDrag = { change, offset ->
                                    change.consume()
                                    dragDropState.onDrag(offset)

                                    if (overscrollJob?.isActive != true) {
                                        val overscroll = dragDropState.checkForOverScroll()
                                        if (overscroll != 0f) {
                                            overscrollJob = scope.launch {
                                                dragDropState.lazyListState.scrollBy(overscroll)
                                            }
                                        } else {
                                            overscrollJob?.cancel()
                                        }
                                    }
                                },
                                onDragStart = { offset -> dragDropState.onDragStart(offset) },
                                onDragEnd = { dragDropState.onDragInterrupted() },
                                onDragCancel = { dragDropState.onDragInterrupted() }
                            )
                        },
                    state = dragDropState.lazyListState,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(viewModel.selectedStretches) { index, stretch ->
                        val isDragging = index == dragDropState.currentIndexOfDraggedItem
                        val displacement = if (isDragging) dragDropState.elementDisplacement ?: 0f else 0f
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer { translationY = displacement },
                            onClick = { stretchToEditState.value = index to stretch }
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.DragHandle,
                                    contentDescription = "Drag to reorder",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 8.dp)
                                )

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
            }
        }
    }

    if (showAddDialog) {
        AddStretchesDialog(
            libraryStretches = viewModel.libraryStretches,
            imageLoader = imageLoader,
            pkgName = pkgName,
            onDismiss = { showAddDialog = false },
            onAdd = { stretches ->
                viewModel.addStretchesToRoutine(stretches)
                showAddDialog = false
            }
        )
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

@SuppressLint("DiscouragedApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStretchesDialog(
    libraryStretches: List<Stretch>,
    imageLoader: ImageLoader,
    pkgName: String,
    onDismiss: () -> Unit,
    onAdd: (List<Stretch>) -> Unit
) {
    val currentContext = LocalContext.current
    var searchQuery by remember { mutableStateOf(value = "") }
    val selectedIds = remember { mutableStateListOf<String>() }
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(Unit) {
        PresetData.presetCategories.forEach { expandedCategories[it] = false }
        expandedCategories["Other"] = false
    }

    val filteredStretches = libraryStretches.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.description.contains(searchQuery, ignoreCase = true)
    }
    val groupedStretches = filteredStretches.groupBy { it.category }
    val sortedCategories = PresetData.presetCategories + "Other"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Stretches") },
        text = {
            Column {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(text = "Search stretches...") },
                    singleLine = true,
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // When searching, show a flat list so results aren't hidden inside collapsed categories.
                    if (searchQuery.isNotBlank()) {
                        items(filteredStretches) { stretch ->
                            SelectableStretchRow(
                                stretch = stretch,
                                isSelected = selectedIds.contains(stretch.id),
                                imageLoader = imageLoader,
                                pkgName = pkgName,
                                context = currentContext,
                                onToggle = {
                                    if (selectedIds.contains(stretch.id)) selectedIds.remove(stretch.id)
                                    else selectedIds.add(stretch.id)
                                }
                            )
                        }
                    } else {
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
                                        SelectableStretchRow(
                                            stretch = stretch,
                                            isSelected = selectedIds.contains(stretch.id),
                                            imageLoader = imageLoader,
                                            pkgName = pkgName,
                                            context = currentContext,
                                            onToggle = {
                                                if (selectedIds.contains(stretch.id)) selectedIds.remove(stretch.id)
                                                else selectedIds.add(stretch.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val toAdd = selectedIds.mapNotNull { id -> libraryStretches.firstOrNull { it.id == id } }
                    onAdd(toAdd)
                },
                enabled = selectedIds.isNotEmpty()
            ) {
                Text(text = if (selectedIds.isEmpty()) "Add" else "Add (${selectedIds.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = "Cancel") }
        }
    )
}

@SuppressLint("DiscouragedApi")
@Composable
private fun SelectableStretchRow(
    stretch: Stretch,
    isSelected: Boolean,
    imageLoader: ImageLoader,
    pkgName: String,
    context: android.content.Context,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggle,
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = isSelected, onCheckedChange = { onToggle() })
            if (stretch.imageKey != null) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(context)
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
        }
    }
}

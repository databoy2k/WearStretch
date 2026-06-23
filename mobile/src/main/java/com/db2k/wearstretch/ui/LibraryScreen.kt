package com.db2k.wearstretch.ui

import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.db2k.wearstretch.ui.routine.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("UNUSED_VALUE")
fun LibraryScreen(viewModel: MainViewModel) {
    val stretchToEditState = remember { mutableStateOf<Stretch?>(value = null) }
    var searchQuery by remember { mutableStateOf(value = "") }
    var isSearching by remember { mutableStateOf(value = false) }
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }

    // Initialize all categories as collapsed by default
    LaunchedEffect(Unit) {
        PresetData.presetCategories.forEach { expandedCategories[it] = false }
        expandedCategories["Other"] = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(text = "Search stretches...") },
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
                        Text(text = "Stretch Library")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        isSearching = !isSearching
                        if (!isSearching) searchQuery = ""
                    }) {
                        Icon(if (isSearching) Icons.Default.Close else Icons.Default.Search, contentDescription = "Search")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                val newStretch = Stretch(
                    id = java.util.UUID.randomUUID().toString(),
                    name = "New Stretch",
                    description = "",
                    durationSeconds = 30,
                    breakDurationSeconds = 5,
                    category = PresetData.presetCategories.first()
                )
                stretchToEditState.value = newStretch
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Stretch")
            }
        }
    ) { padding ->
        val filteredStretches = viewModel.libraryStretches.filter {
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.description.contains(searchQuery, ignoreCase = true)
        }

        val groupedStretches = filteredStretches.groupBy { 
            @Suppress("SENSELESS_COMPARISON")
            if (it.category == null) "Other" else it.category
        }
        val sortedCategories = PresetData.presetCategories + "Other"

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sortedCategories.forEach { category ->
                val categoryStretches = groupedStretches[category] ?: emptyList()
                if (categoryStretches.isNotEmpty() || (!isSearching && PresetData.presetCategories.contains(category))) {
                    item {
                        CategoryHeader(
                            name = category,
                            isExpanded = expandedCategories[category] ?: false,
                            onToggle = { expandedCategories[category] = !(expandedCategories[category] ?: false) }
                        )
                    }

                    if (expandedCategories[category] == true) {
                        items(categoryStretches) { stretchItem ->
                            StretchItem(stretchItem, onEdit = { stretchToEditState.value = stretchItem })
                        }
                    }
                }
            }
        }
    }

    val stretchToEdit = stretchToEditState.value
    if (stretchToEdit != null) {
        StretchEditDialog(
            stretch = stretchToEdit,
            onDismiss = { 
                stretchToEditState.value = null 
            },
            onSave = { updated ->
                viewModel.saveStretch(updated)
                stretchToEditState.value = null
            }
        )
    }
}

@Composable
fun CategoryHeader(name: String, isExpanded: Boolean, onToggle: () -> Unit) {
    Surface(
        onClick = onToggle,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null
            )
        }
    }
}

@SuppressLint("DiscouragedApi")
@Composable
fun StretchItem(stretch: Stretch, onEdit: () -> Unit) {
    val currentContext = LocalContext.current
    val pkgName = LocalContext.current.packageName
    
    val imageLoader = remember {
        ImageLoader.Builder(currentContext)
            .components {
                add(ImageDecoderDecoder.Factory())
                add(GifDecoder.Factory())
            }
            .build()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val model = stretch.customImageUri?.let { File(it) } ?: stretch.imageKey?.let { 
                "android.resource://$pkgName/drawable/$it"
            }
            
            if (model != null) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(currentContext)
                            .data(model)
                            .build(),
                        imageLoader = imageLoader
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(64.dp).padding(end = 16.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stretch.name, style = MaterialTheme.typography.titleLarge)
                Text(text = stretch.description, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                val typeLabel = if (stretch.isSplit) "Split Stretch" else "Normal Stretch"
                Text(
                    text = "$typeLabel | Duration: ${stretch.durationSeconds}s",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

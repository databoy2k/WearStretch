package com.db2k.wearstretch.ui

import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.PhotoLibrary
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@SuppressLint("DiscouragedApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StretchEditDialog(
    stretch: Stretch,
    onDismiss: () -> Unit,
    onSave: (Stretch) -> Unit
) {
    val currentContext = LocalContext.current
    val pkgName = LocalContext.current.packageName
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(stretch.name) }
    var description by remember { mutableStateOf(stretch.description) }
    var duration by remember { mutableStateOf(stretch.durationSeconds.toString()) }
    var breakDuration by remember { mutableStateOf(stretch.breakDurationSeconds.toString()) }
    var category by remember { mutableStateOf(stretch.category) }
    var isSplit by remember { mutableStateOf(stretch.isSplit) }
    var splitBreakDuration by remember { mutableStateOf(stretch.splitBreakDurationSeconds.toString()) }
    var imageKey by remember { mutableStateOf(stretch.imageKey) }
    var customImageUri by remember { mutableStateOf(stretch.customImageUri) }

    var showCategoryDropdown by remember { mutableStateOf(value = false) }

    val imageLoader = remember {
        ImageLoader.Builder(currentContext)
            .components {
                add(ImageDecoderDecoder.Factory())
                add(GifDecoder.Factory())
            }
            .build()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val fileName = "custom_stretch_${System.currentTimeMillis()}.png"
                val file = File(currentContext.filesDir, fileName)
                withContext(Dispatchers.IO) {
                    currentContext.contentResolver.openInputStream(it)?.use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                customImageUri = file.absolutePath
                imageKey = null
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Edit Stretch") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextField(value = name, onValueChange = { name = it }, label = { Text(text = "Name") }, modifier = Modifier.fillMaxWidth())
                TextField(value = description, onValueChange = { description = it }, label = { Text(text = "Description") }, modifier = Modifier.fillMaxWidth())
                
                ExposedDropdownMenuBox(
                    expanded = showCategoryDropdown,
                    onExpandedChange = { showCategoryDropdown = !showCategoryDropdown }
                ) {
                    TextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(text = "Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                        modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryEditable, enabled = true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        (PresetData.presetCategories + "Other").forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(text = cat) },
                                onClick = {
                                    category = cat
                                    showCategoryDropdown = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text(text = "Stretch (s)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    TextField(
                        value = breakDuration,
                        onValueChange = { breakDuration = it },
                        label = { Text(text = "Transition (s)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Split Stretch (Switch Sides)", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isSplit, onCheckedChange = { isSplit = it })
                }

                if (isSplit) {
                    TextField(
                        value = splitBreakDuration,
                        onValueChange = { splitBreakDuration = it },
                        label = { Text(text = "Switch Side Time (s)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Text(text = "Illustration", style = MaterialTheme.typography.titleSmall)
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    item {
                        OutlinedCard(
                            onClick = { 
                                imageKey = null
                                customImageUri = null
                            },
                            modifier = Modifier.size(80.dp),
                            border = if (imageKey == null && (customImageUri == null)) {
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                            } else {
                                CardDefaults.outlinedCardBorder()
                            }
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Block, contentDescription = null)
                                    Text(text = "None", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    item {
                        OutlinedCard(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.size(80.dp),
                            border = if (customImageUri != null) {
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                            } else {
                                CardDefaults.outlinedCardBorder()
                            }
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                    Text(text = "Gallery", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    
                    items(PresetData.presetAnimations) { key ->
                        OutlinedCard(
                            onClick = { 
                                imageKey = key
                                customImageUri = null
                            },
                            modifier = Modifier.size(80.dp),
                            border = if (imageKey == key) {
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                            } else {
                                CardDefaults.outlinedCardBorder()
                            }
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = ImageRequest.Builder(currentContext)
                                        .data("android.resource://$pkgName/drawable/$key")
                                        .build(),
                                    imageLoader = imageLoader
                                ),
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize().padding(4.dp)
                            )
                        }
                    }
                }

                if (customImageUri != null || (imageKey != null)) {
                    Text(text = "Preview:", style = MaterialTheme.typography.labelSmall)
                    val model = customImageUri?.let { File(it) } ?: imageKey?.let { 
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
                            modifier = Modifier.size(100.dp).align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(stretch.copy(
                    name = name,
                    description = description,
                    durationSeconds = duration.toIntOrNull() ?: 0,
                    breakDurationSeconds = breakDuration.toIntOrNull() ?: 0,
                    category = category,
                    isSplit = isSplit,
                    splitBreakDurationSeconds = splitBreakDuration.toIntOrNull() ?: 0,
                    imageKey = imageKey,
                    customImageUri = customImageUri
                ))
            }) {
                Text(text = "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = "Cancel") }
        }
    )
}

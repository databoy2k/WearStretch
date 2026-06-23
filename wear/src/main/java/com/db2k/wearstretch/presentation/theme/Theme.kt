package com.db2k.wearstretch.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.dynamicColorScheme

@Composable
fun WearStretchTheme(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = dynamicColorScheme(context) ?: WearStretchColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

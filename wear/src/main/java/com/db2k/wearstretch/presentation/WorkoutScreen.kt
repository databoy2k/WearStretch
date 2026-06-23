package com.db2k.wearstretch.presentation

import android.annotation.SuppressLint
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.*
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.db2k.wearstretch.data.SettingsStore
import com.db2k.wearstretch.model.Stretch
import java.io.File

@Composable
fun WorkoutScreen(
    stretch: Stretch,
    timeLeft: Int,
    isPaused: Boolean,
    isBreak: Boolean,
    isSwitchingSides: Boolean,
    isStarting: Boolean,
    isSplitSecondHalf: Boolean,
    nextStretchName: String?,
    currentHeartRate: Int,
    currentSteps: Int,
    currentLocation: String?,
    onPauseToggle: () -> Unit,
    onSkip: () -> Unit,
    onCancel: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })

    HorizontalPagerScaffold(
        pagerState = pagerState,
        modifier = Modifier.fillMaxSize()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> WorkoutInfoPage(
                    stretch = stretch,
                    timeLeft = timeLeft,
                    isBreak = isBreak,
                    isSwitchingSides = isSwitchingSides,
                    isStarting = isStarting,
                    isSplitSecondHalf = isSplitSecondHalf,
                    nextStretchName = nextStretchName,
                    currentHeartRate = currentHeartRate,
                    currentSteps = currentSteps,
                    currentLocation = currentLocation
                )
                1 -> WorkoutControlsPage(isPaused, onPauseToggle, onSkip, onCancel)
            }
        }
    }
}

@SuppressLint("DiscouragedApi")
@Composable
private fun WorkoutInfoPage(
    stretch: Stretch,
    timeLeft: Int,
    isBreak: Boolean,
    isSwitchingSides: Boolean,
    isStarting: Boolean,
    isSplitSecondHalf: Boolean,
    nextStretchName: String?,
    currentHeartRate: Int,
    currentSteps: Int,
    currentLocation: String?
) {
    val listState = rememberTransformingLazyColumnState()
    val currentContext = LocalContext.current
    val pkgName = LocalContext.current.packageName
    val isTransition = isBreak || isSwitchingSides || isStarting
    val activeColor = if (isStarting) Color(0xFFFF9100) else if (isTransition) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary
    
    val remainingSplits = if (stretch.isSplit && !isStarting) {
        if (isSwitchingSides || (isSplitSecondHalf && !isBreak)) 1 else 2
    } else {
        0
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val imageLoader = remember {
        ImageLoader.Builder(currentContext)
            .components {
                add(ImageDecoderDecoder.Factory())
                add(GifDecoder.Factory())
            }
            .build()
    }

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = if (isStarting) "Get Ready!" else if (isSwitchingSides) "Switch Sides" else if (isBreak) "Change Position" else stretch.name,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = if (isStarting) Color(0xFFFF9100) else if (isTransition) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                Text(
                    text = "$timeLeft",
                    style = MaterialTheme.typography.displayMedium,
                    color = activeColor
                )
            }

            if (remainingSplits > 0) {
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                    SplitIndicator(remaining = remainingSplits)
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            if (SettingsStore.trackHeartRate.value || SettingsStore.trackSteps.value) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (SettingsStore.trackHeartRate.value && currentHeartRate > 0) {
                            Text(text = "❤ $currentHeartRate", color = Color.Red, style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        if (SettingsStore.trackSteps.value) {
                            Text(text = "👣 $currentSteps", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            if (SettingsStore.trackLocation.value) {
                item {
                    Text(
                        text = if (currentLocation != null) "📍 Location Set" else "📍 Searching...",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (currentLocation != null) Color.Green else Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            if (isTransition && nextStretchName != null) {
                item {
                    Text(
                        text = if (isStarting) "First: $nextStretchName" else if (isSwitchingSides) "Next: Side 2" else "Next: $nextStretchName",
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        color = if (isStarting) Color(0xFFFF9100) else Color(0xFF00E5FF),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            // Stretch image/animation
            if (SettingsStore.showAnimations.value) {
                val model = stretch.customImageUri?.let { File(it) } ?: stretch.imageKey?.let { 
                    "android.resource://$pkgName/drawable/$it"
                }

                if (model != null) {
                    item {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = ImageRequest.Builder(currentContext)
                                    .data(model)
                                    .build(),
                                imageLoader = imageLoader
                            ),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(140.dp) 
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .padding(8.dp)
                        )
                    }
                }
            }

            item {
                Text(
                    text = stretch.description,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun WorkoutControlsPage(
    isPaused: Boolean,
    onPauseToggle: () -> Unit,
    onSkip: () -> Unit,
    onCancel: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val listState = rememberTransformingLazyColumnState()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                ListHeader {
                    Text(text = "Controls")
                }
            }

            item {
                Button(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPauseToggle()
                    }
                ) {
                    Text(text = if (isPaused) "Resume" else "Pause")
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSkip()
                    }
                ) {
                    Text(text = "Skip")
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCancel()
                    }
                ) {
                    Text(text = "Cancel Workout", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun SplitIndicator(remaining: Int) {
    val (backgroundColor, textColor) = when (remaining) {
        2 -> Pair(Color(0xFFFFB300), Color.Black) // Amber/Yellow
        1 -> Pair(Color(0xFFE91E63), Color.White) // Rose/Pink
        else -> Pair(Color(0xFF00E676), Color.Black) // Vibrant Green
    }
    
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(color = backgroundColor, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$remaining",
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

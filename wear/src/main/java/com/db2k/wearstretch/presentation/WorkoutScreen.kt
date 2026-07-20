package com.db2k.wearstretch.presentation

import android.annotation.SuppressLint
import android.graphics.drawable.Animatable
import android.widget.ImageView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.viewinterop.AndroidView
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

// Pager pages: left = animation/description, middle = live workout, right = controls.
private const val PAGE_DESCRIPTION = 0
private const val PAGE_WORKOUT = 1
private const val PAGE_CONTROLS = 2

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
    onPause: () -> Unit,
    onSkip: () -> Unit,
    onCancel: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = PAGE_WORKOUT, pageCount = { 3 })

    // Landing on the description/animation page pauses the workout so the user can
    // read and follow along without the timer running out.
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage == PAGE_DESCRIPTION) {
            onPause()
        }
    }

    HorizontalPagerScaffold(
        pagerState = pagerState,
        modifier = Modifier.fillMaxSize()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                PAGE_DESCRIPTION -> WorkoutDescriptionPage(
                    stretch = stretch,
                    isBreak = isBreak,
                    isSwitchingSides = isSwitchingSides,
                    isStarting = isStarting
                )
                PAGE_WORKOUT -> WorkoutInfoPage(
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
                PAGE_CONTROLS -> WorkoutControlsPage(isPaused, onPauseToggle, onSkip, onCancel)
            }
        }
    }
}

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
    val isTransition = isBreak || isSwitchingSides || isStarting
    val activeColor = if (isStarting) Color(0xFFFF9100) else if (isTransition) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary

    // Which side of a split stretch we're currently on: 1 for the first side, 2 for the second.
    val currentSide = if (stretch.isSplit && !isStarting) {
        if (isSwitchingSides || (isSplitSecondHalf && !isBreak)) 2 else 1
    } else {
        0
    }

    // Simplified, vertically-centered layout so it reads well on circular watches.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            Text(
                text = if (isStarting) "Get Ready!" else if (isSwitchingSides) "Switch Sides" else if (isBreak) "Change Position" else stretch.name,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = if (isStarting) Color(0xFFFF9100) else if (isTransition) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Text(
                text = "$timeLeft",
                style = MaterialTheme.typography.displayMedium,
                color = activeColor
            )

            if (currentSide > 0) {
                SplitIndicator(side = currentSide)
            }

            if ((SettingsStore.trackHeartRate.value && currentHeartRate > 0) ||
                (SettingsStore.trackSteps.value)
            ) {
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

            if (SettingsStore.trackLocation.value) {
                Text(
                    text = if (currentLocation != null) "📍 Location Set" else "📍 Searching...",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (currentLocation != null) Color.Green else Color.Gray
                )
            }

            if (isTransition && nextStretchName != null) {
                Text(
                    text = if (isStarting) "First: $nextStretchName" else if (isSwitchingSides) "Next: Side 2" else "Next: $nextStretchName",
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    color = if (isStarting) Color(0xFFFF9100) else Color(0xFF00E5FF),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@SuppressLint("DiscouragedApi")
@Composable
private fun WorkoutDescriptionPage(
    stretch: Stretch,
    isBreak: Boolean,
    isSwitchingSides: Boolean,
    isStarting: Boolean
) {
    val listState = rememberTransformingLazyColumnState()
    val currentContext = LocalContext.current
    val pkgName = LocalContext.current.packageName
    val isTransition = isBreak || isSwitchingSides || isStarting

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

    // Resolve any built-in AnimatedVectorDrawable up-front (must be in composable scope,
    // not inside the LazyColumn DSL). 0 means "not an AVD" -> fall back to the Coil path.
    val avdResId = remember(stretch.imageKey, stretch.customImageUri) {
        val key = stretch.imageKey
        if (stretch.customImageUri == null && key != null) {
            val id = currentContext.resources.getIdentifier(key, "drawable", pkgName)
            if (id != 0 && currentContext.getDrawable(id) is Animatable) id else 0
        } else 0
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
                    text = stretch.name,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = if (isTransition) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Stretch image/animation
            if (SettingsStore.showAnimations.value) {
                // AnimatedVectorDrawable (built-in vector animations) render via ImageView so
                // the drawable's ObjectAnimators actually run — Coil only draws a static first
                // frame. Custom GIF/WebP files still flow through Coil below. avdResId is
                // resolved above in composable scope.
                if (avdResId != 0) {
                    item {
                        AnimatedVectorImage(
                            resId = avdResId,
                            contentDescription = stretch.name,
                            modifier = Modifier
                                .size(140.dp)
                                .padding(8.dp)
                        )
                    }
                } else {
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
                                contentDescription = stretch.name,
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
private fun AnimatedVectorImage(
    resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            ImageView(ctx).apply {
                this.contentDescription = contentDescription
                setImageResource(resId)
                (drawable as? Animatable)?.start()
            }
        },
        update = { view ->
            if (view.drawable == null || view.tag != resId) {
                view.tag = resId
                view.setImageResource(resId)
            }
            (view.drawable as? Animatable)?.start()
        }
    )
}

@Composable
private fun SplitIndicator(side: Int) {
    val (backgroundColor, textColor) = when (side) {
        1 -> Pair(Color(0xFFFFB300), Color.Black) // Side 1: Amber
        2 -> Pair(Color(0xFFE91E63), Color.White) // Side 2: Rose
        else -> Pair(Color(0xFF00E676), Color.Black) // Vibrant Green
    }

    Box(
        modifier = Modifier
            .size(24.dp)
            .background(color = backgroundColor, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$side",
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

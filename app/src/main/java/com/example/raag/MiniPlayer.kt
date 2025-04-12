package com.example.raag

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.isActive

@SuppressLint("UseOfNonLambdaOffsetOverload")
@Composable
fun MiniPlayer(
    navController: NavController,
    viewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier,
    playerScreenOffset: Float = 0f
) {
    val currentSong by viewModel.currentSong
    val isPlaying by viewModel.isPlaying
    val currentPosition by viewModel.currentPosition

    val density = LocalDensity.current
    val miniPlayerHeight = 120.dp
    val miniPlayerHeightPx = with(density) { miniPlayerHeight.toPx() }

    // Calculate screen height for swipe thresholds
    val screenHeight = with(density) {
        LocalConfiguration.current.screenHeightDp.dp.toPx()
    }

    // Track swipe-up gesture state
    var swipeUpOffset by remember { mutableFloatStateOf(0f) }
    var isDraggingUp by remember { mutableStateOf(false) }

    // Threshold for transitioning to player screen (30% of screen height)
    val swipeUpThreshold = screenHeight * 0.3f

    // Calculate opacity for mini-player during swipe-up
    val miniPlayerAlpha = if (isDraggingUp) {
        (1f - (swipeUpOffset / swipeUpThreshold).coerceIn(0f, 1f))
    } else 1f

    // Calculate animation offset for swipe-down from player
    val miniPlayerOffset by animateFloatAsState(
        targetValue = if (playerScreenOffset > 0f) playerScreenOffset else 0f,
        label = "miniPlayerOffset"
    )

    // Update position every 100ms when the song is playing
    LaunchedEffect(isPlaying) {
        while(isActive && isPlaying) {
            viewModel.updatePosition()
            kotlinx.coroutines.delay(100)
        }
    }

    // Reset swipe state when navigating
    LaunchedEffect(currentSong) {
        swipeUpOffset = 0f
        isDraggingUp = false
    }

    AnimatedVisibility(
        visible = currentSong != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
            .zIndex(1f)
            .offset(y = (miniPlayerOffset - miniPlayerHeightPx).coerceAtLeast(0f).dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(miniPlayerHeight)
                .padding(horizontal = 8.dp)
                .graphicsLayer {
                    // Apply transformations for swipe-up animation
                    translationY = -swipeUpOffset
                    alpha = miniPlayerAlpha
                }
                .clickable {
                    currentSong?.let { song ->
                        navController.navigate("player/${song.id}")
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDraggingUp = true },
                        onDragEnd = {
                            // Determine whether to navigate based on threshold
                            if (swipeUpOffset > swipeUpThreshold) {
                                currentSong?.let { song ->
                                    // Navigate to player screen with fade animation
                                    navController.navigate("player/${song.id}")
                                }
                            }
                            // Reset state when gesture ends
                            swipeUpOffset = 0f
                            isDraggingUp = false
                        },
                        onDragCancel = {
                            // Reset state on cancel
                            swipeUpOffset = 0f
                            isDraggingUp = false
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val (_, y) = dragAmount
                            if (y < 0) { // Only detect upward motion
                                swipeUpOffset = (swipeUpOffset - y).coerceIn(0f, swipeUpThreshold * 1.2f)
                            }
                        }
                    )
                },
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            )
        ) {
            currentSong?.let { song ->
                Column {
                    // Wiggly Progress Bar with playing state
                    WigglyLinearProgressIndicator(
                        progress = currentPosition.toFloat() / song.duration.toFloat(),
                        isPlaying = isPlaying,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        onProgressChange = { progress ->
                            val newPosition = (progress * song.duration).toLong()
                            viewModel.seekTo(newPosition)
                        }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Album art with rounded corners
                        Box(
                            modifier = Modifier
                                .size(78.dp)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = song.albumArtUri,
                                contentDescription = "Album art",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                error = rememberVectorPainter(Icons.Default.MusicNote)
                            )
                        }

                        // Song info
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Play/pause button with rounded background
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(
                                onClick = { viewModel.togglePlayPause() },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Show a fading overlay for the Now Playing screen transition
    if (isDraggingUp && swipeUpOffset > 0) {
        // This overlay appears as the mini player is swiped up
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = (swipeUpOffset / swipeUpThreshold).coerceIn(0f, 0.95f)
                }
                .background(MaterialTheme.colorScheme.background)
                .zIndex(0.5f)
        )
    }
}

package com.example.raag

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
                .clickable {
                    currentSong?.let { song ->
                        navController.navigate("player/${song.id}")
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val (_, y) = dragAmount
                        if (y < -50) { // Swipe up
                            currentSong?.let { song ->
                                navController.navigate("player/${song.id}")
                            }
                        }
                    }
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
}

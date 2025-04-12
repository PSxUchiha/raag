package com.example.raag

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    songId: Long,
    navController: NavController,
    viewModel: MusicPlayerViewModel
) {
    val currentSong by viewModel.currentSong
    val isPlaying by viewModel.isPlaying
    val currentPosition by viewModel.currentPosition
    val context = LocalContext.current
    val density = LocalDensity.current

    // Animation state for swipe down
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDraggingDown by remember { mutableStateOf(false) }
    var alphaState by remember { mutableFloatStateOf(1f) }

    // Calculate screen height for animation thresholds
    val screenHeight = with(density) {
        androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp.toPx()
    }

    // Threshold where mini-player starts to appear (20% down the screen - reduced from 40%)
    val miniPlayerThreshold = screenHeight * 0.2f

    // Calculate mini-player visibility and offset
    val miniPlayerAlpha = (offsetY / (miniPlayerThreshold * 0.5f)).coerceIn(0f, 1f)
    val miniPlayerOffsetY = (screenHeight - offsetY).coerceAtLeast(0f)

    // Set up draggable state for more reliable swipe detection
    val dragState = rememberDraggableState { delta ->
        if (delta > 0) { // Only allow downward dragging
            offsetY += delta
            // Gradually reduce alpha as we drag down
            alphaState = (1 - (offsetY / (screenHeight * 0.5f))).coerceIn(0f, 1f)
        }
    }

    // Load the song if not already playing
    LaunchedEffect(songId) {
        if (currentSong?.id != songId) {
            viewModel.playSongById(songId)
        }
    }

    // Update position every 100ms while the song is playing
    LaunchedEffect(isPlaying) {
        while(isActive && isPlaying) {
            viewModel.updatePosition()
            delay(100)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Player Screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = offsetY
                    alpha = alphaState
                }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Vertical,
                    onDragStarted = { isDraggingDown = true },
                    onDragStopped = { velocity ->
                        // Use both position and velocity to determine whether to dismiss
                        if (offsetY > screenHeight * 0.15f || velocity > 300f) {
                            // Lower threshold (15% of screen) OR velocity-based detection
                            alphaState = 0f
                            navController.popBackStack()
                        } else {
                            // Spring back with animation
                            offsetY = 0f
                            isDraggingDown = false
                        }
                    }
                )
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Now Playing") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    currentSong?.let { song ->
                        Spacer(modifier = Modifier.height(16.dp))

                        // Larger Album Art
                        Box(
                            modifier = Modifier
                                .size(320.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (song.albumArtUri != null) {
                                AsyncImage(
                                    model = song.albumArtUri,
                                    contentDescription = "Album art",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    error = rememberVectorPainter(Icons.Default.MusicNote)
                                )
                            } else {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = "Music",
                                        modifier = Modifier.size(120.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Song Info
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = song.album,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Playback progress controls
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Add a Box with an overlay to hide wiggly line glitches at the start
                            Box(modifier = Modifier.fillMaxWidth()) {
                                // Wiggly progress with playing state
                                WigglyLinearProgressIndicator(
                                    progress = currentPosition.toFloat() / song.duration.toFloat(),
                                    isPlaying = isPlaying,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(20.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    onProgressChange = { progress ->
                                        val newPosition = (progress * song.duration).toLong()
                                        viewModel.seekTo(newPosition)
                                    }
                                )

                                // Transparent gradient overlay to hide glitching at the start
                                Box(
                                    modifier = Modifier
                                        .width(16.dp)
                                        .height(20.dp)
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.background,
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatDuration(currentPosition),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = formatDuration(song.duration),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Playback Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.skipToPrevious() }) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous",
                                    modifier = Modifier.size(48.dp)
                                )
                            }

                            FilledIconButton(
                                onClick = { viewModel.togglePlayPause() },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            IconButton(onClick = { viewModel.skipToNext() }) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next",
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    } ?: run {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }

        // Emerging Mini Player (only visible during swipe-down transition)
        if (offsetY > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)  // Fixed padding to avoid navigation bar
                    .graphicsLayer {
                        alpha = miniPlayerAlpha
                        translationY = miniPlayerOffsetY
                    }
            ) {
                currentSong?.let { song ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)  // Increased from 80dp (50% thicker)
                            .padding(horizontal = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column {
                            // Wiggly Progress Bar
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
                                        .size(90.dp)  // Larger album art
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
                                        style = MaterialTheme.typography.titleLarge,  // Larger title
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface  // Dynamic color
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
        }
    }
}

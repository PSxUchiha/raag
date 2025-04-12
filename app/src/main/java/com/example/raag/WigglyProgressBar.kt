package com.example.raag

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.sin

@Composable
fun WigglyLinearProgressIndicator(
    progress: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    onProgressChange: ((Float) -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wiggly_animation")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase_animation"
    )

    // Animate the amplitude based on playing state
    val amplitudeState by animateFloatAsState(
        targetValue = if (isPlaying) 0.25f else 0f,
        animationSpec = tween(500),
        label = "amplitude_animation"
    )

    var progressState by remember { mutableFloatStateOf(progress) }
    var isDragging by remember { mutableStateOf(false) }

    // Get the background color for the gradient
    val backgroundColor = MaterialTheme.colorScheme.background

    LaunchedEffect(progress) {
        if (!isDragging) {
            progressState = progress
        }
    }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { position ->
                    val newProgress = (position.x / size.width).coerceIn(0f, 1f)
                    progressState = newProgress
                    onProgressChange?.invoke(newProgress)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        onProgressChange?.invoke(progressState)
                    },
                    onDragCancel = { isDragging = false }
                ) { change, _ ->
                    change.consume()
                    val newProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                    progressState = newProgress
                    onProgressChange?.invoke(newProgress)
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val progressWidth = width * progressState

        // Padding to avoid boundary issues
        val horizontalPadding = width * 0.02f  // 2% of width as padding

        // Draw track only for the part not yet played
        drawLine(
            color = trackColor,
            start = Offset(progressWidth, height / 2),
            end = Offset(width - horizontalPadding, height / 2),
            strokeWidth = height * 0.3f,
            cap = StrokeCap.Round
        )

        // Draw wiggly/straight progress line
        if (progressState > 0f) {
            val path = Path()
            val amplitude = height * amplitudeState  // Animated amplitude
            val frequency = 0.08f
            val lineThickness = height * 0.3f

            // Start the path slightly inside the boundary
            path.moveTo(horizontalPadding, height / 2)

            for (x in horizontalPadding.toInt()..progressWidth.toInt() step 1) {
                val xFloat = x.toFloat()
                val y = height / 2 + amplitude * sin(xFloat * frequency + phase)
                path.lineTo(xFloat, y)
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = lineThickness, cap = StrokeCap.Round)
            )

            // Draw a stationary vertical line at the current position
            if (progressState > 0.02f) {
                drawLine(
                    color = color,
                    start = Offset(progressWidth, height * 0.25f),
                    end = Offset(progressWidth, height * 0.75f),
                    strokeWidth = lineThickness * 0.8f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Draw gradient overlay at the left edge to hide glitching
        val gradientWidth = width * 0.05f  // 5% of width for gradient

        // Only draw gradient if we're close to the left edge
        if (progressState < 0.1f || !isPlaying) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        backgroundColor,
                        backgroundColor.copy(alpha = 0.7f),
                        backgroundColor.copy(alpha = 0.0f)
                    ),
                    startX = 0f,
                    endX = gradientWidth
                ),
                topLeft = Offset(0f, 0f),
                size = Size(gradientWidth, height)
            )
        }
    }
}

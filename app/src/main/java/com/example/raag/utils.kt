package com.example.raag

/**
 * Format duration from milliseconds to MM:SS format
 */
fun formatDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis / 1000
    val minutes = totalSeconds / 60
    val remainingSeconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}

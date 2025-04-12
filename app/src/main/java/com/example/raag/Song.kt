package com.example.raag

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val filePath: String,
    val albumArtUri: String?
)

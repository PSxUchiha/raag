package com.example.raag.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.raag.MainActivity
import com.example.raag.R
import com.example.raag.Song
import kotlinx.coroutines.launch
import androidx.core.net.toUri

class MusicPlayerService : LifecycleService() {
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "music_player_channel"
    }

    private val binder = MusicPlayerBinder()
    private var player: ExoPlayer? = null
    private var currentSong: Song? = null

    inner class MusicPlayerBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializePlayer()
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        startForegroundService()
                    }
                }
            })
        }
    }

    fun playSong(song: Song) {
        player?.let { exoPlayer ->
            currentSong = song

            val mediaItem = MediaItem.fromUri(song.filePath.toUri())
            exoPlayer.clearMediaItems()
            exoPlayer.addMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()

            startForegroundService()
        }
    }

    fun togglePlayPause() {
        player?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun isPlaying(): Boolean = player?.isPlaying ?: false

    fun getCurrentPosition(): Long = player?.currentPosition ?: 0

    fun seekTo(position: Long) {
        player?.seekTo(position)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Player",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music Player Controls"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        currentSong?.let { song ->
            lifecycleScope.launch {
                val notification = createNotification(song)
                startForeground(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun createNotification(song: Song): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(song.title)
            .setContentText(song.artist)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }
}

package com.example.raag

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.raag.service.MusicPlayerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicPlayerViewModel : ViewModel() {
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _currentSong = mutableStateOf<Song?>(null)
    val currentSong: State<Song?> = _currentSong

    private val _isPlaying = mutableStateOf(false)
    val isPlaying: State<Boolean> = _isPlaying

    private val _currentPosition = mutableLongStateOf(0L)
    val currentPosition: State<Long> = _currentPosition

    @SuppressLint("StaticFieldLeak")
    private var musicService: MusicPlayerService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicPlayerService.MusicPlayerBinder
            musicService = binder.getService()
            serviceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            serviceBound = false
        }
    }

    fun bindToService(context: Context) {
        val intent = Intent(context, MusicPlayerService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        context.startService(intent)
    }

    fun unbindFromService(context: Context) {
        if (serviceBound) {
            context.unbindService(serviceConnection)
            serviceBound = false
        }
    }

    fun loadSongs(context: Context) {
        viewModelScope.launch {
            _songs.value = SongRepository.getSongsFromDevice(context)
        }
    }

    fun playSong(song: Song) {
        _currentSong.value = song
        musicService?.playSong(song)
        _isPlaying.value = true
    }

    fun playSongById(songId: Long) {
        val song = _songs.value.find { it.id == songId }
        song?.let { playSong(it) }
    }

    fun togglePlayPause() {
        musicService?.togglePlayPause()
        _isPlaying.value = musicService?.isPlaying() ?: false
    }

    fun skipToNext() {
        _currentSong.value?.let { currentSong ->
            val currentIndex = _songs.value.indexOfFirst { it.id == currentSong.id }
            if (currentIndex < _songs.value.size - 1) {
                playSong(_songs.value[currentIndex + 1])
            }
        }
    }

    fun skipToPrevious() {
        _currentSong.value?.let { currentSong ->
            val currentIndex = _songs.value.indexOfFirst { it.id == currentSong.id }
            if (currentIndex > 0) {
                playSong(_songs.value[currentIndex - 1])
            }
        }
    }

    fun seekTo(position: Long) {
        musicService?.seekTo(position)
    }

    fun updatePosition() {
        _currentPosition.longValue = musicService?.getCurrentPosition() ?: 0
    }
}

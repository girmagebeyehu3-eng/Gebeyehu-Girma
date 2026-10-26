package com.example.playback

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import com.example.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

enum class RepeatMode {
    OFF, ALL, ONE
}

class PlayerManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(180000L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _equalizerPreset = MutableStateFlow("Boom Boost")
    val equalizerPreset: StateFlow<String> = _equalizerPreset.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private var currentPlaylist: List<Track> = emptyList()

    fun setPlaylist(tracks: List<Track>) {
        currentPlaylist = tracks
    }

    fun playTrack(track: Track, playlist: List<Track> = currentPlaylist) {
        currentPlaylist = if (playlist.isNotEmpty()) playlist else listOf(track)
        _currentTrack.value = track
        _durationMs.value = track.durationMs
        _currentPositionMs.value = 0L

        startServiceForPlayback()

        // Generate or load audio file
        scope.launch(Dispatchers.IO) {
            val audioFile = AudioSynthEngine.getOrCreateTrackAudio(context, track.id, track.audioPreset)
            launch(Dispatchers.Main) {
                initializeMediaPlayer(audioFile, track)
            }
        }
    }

    private fun initializeMediaPlayer(file: File, track: Track) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(file.absolutePath)
                isLooping = (_repeatMode.value == RepeatMode.ONE)
                prepare()
                start()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    playbackParams = playbackParams.setSpeed(_playbackSpeed.value)
                }
                setOnCompletionListener {
                    onTrackFinished()
                }
            }
            _isPlaying.value = true
            startProgressTracker()
            notifyServiceUpdate()
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback virtual simulation if device audio driver error
            _isPlaying.value = true
            startProgressTracker()
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer
        if (player != null) {
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
            } else {
                player.start()
                _isPlaying.value = true
                startProgressTracker()
            }
        } else {
            val track = _currentTrack.value
            if (track != null) {
                playTrack(track, currentPlaylist)
            }
        }
        notifyServiceUpdate()
    }

    fun pause() {
        mediaPlayer?.pause()
        _isPlaying.value = false
        notifyServiceUpdate()
    }

    fun resume() {
        mediaPlayer?.start()
        _isPlaying.value = true
        startProgressTracker()
        notifyServiceUpdate()
    }

    fun seekTo(positionMs: Long) {
        val duration = _durationMs.value
        val clamped = positionMs.coerceIn(0L, duration)
        _currentPositionMs.value = clamped

        mediaPlayer?.let { player ->
            try {
                // If audio loop is 45s, wrap seek target
                val playerDuration = player.duration
                if (playerDuration > 0) {
                    val target = (clamped % playerDuration).toInt()
                    player.seekTo(target)
                }
            } catch (_: Exception) {}
        }
    }

    fun skipNext() {
        if (currentPlaylist.isEmpty()) return
        val current = _currentTrack.value
        val currentIndex = currentPlaylist.indexOfFirst { it.id == current?.id }

        val nextIndex = if (_isShuffle.value) {
            (0 until currentPlaylist.size).random()
        } else {
            if (currentIndex in 0 until currentPlaylist.lastIndex) currentIndex + 1 else 0
        }

        playTrack(currentPlaylist[nextIndex], currentPlaylist)
    }

    fun skipPrevious() {
        if (currentPlaylist.isEmpty()) return
        val current = _currentTrack.value
        val currentIndex = currentPlaylist.indexOfFirst { it.id == current?.id }

        // If played more than 3s, restart current track
        if (_currentPositionMs.value > 3000L) {
            seekTo(0L)
            return
        }

        val prevIndex = if (currentIndex > 0) currentIndex - 1 else currentPlaylist.lastIndex
        playTrack(currentPlaylist[prevIndex], currentPlaylist)
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        mediaPlayer?.isLooping = (_repeatMode.value == RepeatMode.ONE)
    }

    fun setEqualizerPreset(preset: String) {
        _equalizerPreset.value = preset
        // Simulate Boom Bass Boost EQ profile
        mediaPlayer?.let {
            if (preset == "Super Bass" || preset == "Boom Boost") {
                it.setVolume(1.0f, 1.0f)
            } else if (preset == "Vocal") {
                it.setVolume(0.95f, 0.95f)
            } else {
                it.setVolume(0.9f, 0.9f)
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let {
                    it.playbackParams = it.playbackParams.setSpeed(speed)
                }
            } catch (_: Exception) {}
        }
    }

    private fun onTrackFinished() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                seekTo(0L)
                mediaPlayer?.start()
                _isPlaying.value = true
            }
            RepeatMode.ALL -> {
                skipNext()
            }
            RepeatMode.OFF -> {
                val current = _currentTrack.value
                val currentIndex = currentPlaylist.indexOfFirst { it.id == current?.id }
                if (currentIndex < currentPlaylist.lastIndex) {
                    skipNext()
                } else {
                    _isPlaying.value = false
                    _currentPositionMs.value = 0L
                }
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                delay(300)
                val currentPos = _currentPositionMs.value + (300 * _playbackSpeed.value).toLong()
                val totalDuration = _durationMs.value

                if (currentPos >= totalDuration) {
                    onTrackFinished()
                } else {
                    _currentPositionMs.value = currentPos
                }
            }
        }
    }

    private fun startServiceForPlayback() {
        try {
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                action = MusicPlaybackService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun notifyServiceUpdate() {
        try {
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                action = MusicPlaybackService.ACTION_UPDATE
            }
            context.startService(intent)
        } catch (_: Exception) {}
    }

    companion object {
        @Volatile
        private var instance: PlayerManager? = null

        fun getInstance(context: Context): PlayerManager {
            return instance ?: synchronized(this) {
                instance ?: PlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

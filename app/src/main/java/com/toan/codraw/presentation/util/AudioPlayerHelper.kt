package com.toan.codraw.presentation.util

import android.media.AudioAttributes
import android.media.MediaPlayer
import java.io.IOException

class AudioPlayerHelper {
    private var player: MediaPlayer? = null
    private var isPlaying = false

    fun playAudio(url: String, onComplete: () -> Unit) {
        if (this.isPlaying) {
            stopAudio()
        }
        player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            try {
                setDataSource(url)
                prepareAsync()
                setOnPreparedListener {
                    start()
                    this@AudioPlayerHelper.isPlaying = true
                }
                setOnCompletionListener {
                    stopAudio()
                    onComplete()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun stopAudio() {
        player?.apply {
            if (this@AudioPlayerHelper.isPlaying) {
                stop()
            }
            release()
        }
        player = null
        isPlaying = false
    }
}

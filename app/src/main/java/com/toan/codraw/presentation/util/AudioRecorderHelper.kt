package com.toan.codraw.presentation.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException

class AudioRecorderHelper(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var audioFile: File? = null

    fun startRecording(): File? {
        val outputDir = context.cacheDir
        audioFile = File.createTempFile("voice_record_", ".webm", outputDir)
        
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.WEBM)
            setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
            setOutputFile(audioFile?.absolutePath)
            
            try {
                prepare()
                start()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return audioFile
    }

    fun stopRecording(): File? {
        recorder?.apply {
            try {
                stop()
            } catch (e: RuntimeException) {
                // Ignore if called immediately after start before audio data is ready
            }
            release()
        }
        recorder = null
        return audioFile
    }
}

package com.saarthi.app

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/** Records a short voice clip to send to Whisper. Tap to start, tap when done. */
class AudioRecorder(private val ctx: Context) {
    private var recorder: MediaRecorder? = null
    private var outFile: File? = null

    fun start(): Boolean {
        return try {
            val f = File(ctx.cacheDir, "voice.m4a")
            if (f.exists()) f.delete()
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(ctx)
                    else @Suppress("DEPRECATION") MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(96000)
            r.setAudioSamplingRate(16000)          // Whisper likes 16 kHz
            r.setOutputFile(f.absolutePath)
            r.prepare(); r.start()
            recorder = r; outFile = f
            true
        } catch (_: Exception) { stop(); false }
    }

    /** Stops and returns the recorded file, or null on failure. */
    fun stop(): File? {
        return try {
            recorder?.apply { stop(); release() }
            recorder = null
            outFile
        } catch (_: Exception) {
            try { recorder?.release() } catch (_: Exception) {}
            recorder = null
            null
        }
    }
}

package com.lifeos.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * The voice of LifeOS.
 *  - speaks SLOWLY and gently (rate from settings, default 0.8)
 *  - listens in the language chosen in settings
 * The phone's own Google speech engine handles the many languages.
 */
class Voice(private val ctx: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech = TextToSpeech(ctx, this)
    private var ready = false
    private var recognizer: SpeechRecognizer? = null

    private fun locale(): Locale {
        val s = Store.settings(ctx).lang            // e.g. "hi-IN"
        val parts = s.split("-")
        return if (parts.size >= 2) Locale(parts[0], parts[1]) else Locale(parts[0])
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ready = true
            applyVoicePrefs()
        }
    }

    private fun applyVoicePrefs() {
        val s = Store.settings(ctx)
        try { tts.language = locale() } catch (_: Exception) {}
        tts.setSpeechRate(s.rate)                    // slow
        tts.setPitch(1.05f)                          // warm
    }

    /** Speak a short, simple sentence. */
    fun speak(text: String) {
        if (!ready) return
        applyVoicePrefs()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lifeos")
    }

    fun stopSpeaking() { try { tts.stop() } catch (_: Exception) {} }

    /**
     * Listen once. onHeard gets the best transcript; onDone always fires.
     * Caller must already hold RECORD_AUDIO permission.
     */
    fun listen(onHeard: (String) -> Unit, onDone: () -> Unit, onError: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
            onError("Voice typing is not set up on this phone.")
            onDone(); return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(ctx)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Store.settings(ctx).lang)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                onHeard(list?.firstOrNull() ?: "")
                onDone()
            }
            override fun onError(error: Int) { onError(errorText(error)); onDone() }
            override fun onReadyForSpeech(p0: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(p0: Float) {}
            override fun onBufferReceived(p0: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(p0: Bundle?) {}
            override fun onEvent(p0: Int, p1: Bundle?) {}
        })
        recognizer?.startListening(intent)
    }

    private fun errorText(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_NO_MATCH -> "Sorry, I did not hear you. Please try again."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I did not hear anything."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "I need permission to hear you."
        else -> "Please try again."
    }

    fun release() {
        try { tts.shutdown() } catch (_: Exception) {}
        try { recognizer?.destroy() } catch (_: Exception) {}
    }
}

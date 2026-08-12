package com.lifeos.app.core.interaction

import android.content.Context
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Android's own free, offline speech engine — the same "phone's own
 * voice, slow and friendly" approach the whole project has used from the
 * start (Engineering Master Plan §3: TTS is "already solved," no reason to
 * add a dependency). No AI provider, no network call, no key.
 *
 * Relocated here from feature:voice (zero logic changes) so feature:medicines
 * can also inject it for spoken reminder escalation — core:* modules can't
 * depend on feature:voice, but every feature module can depend on core:interaction.
 */
@Singleton
class VoiceEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var ready = false
    private var recognizer: SpeechRecognizer? = null

    private val _speaking = MutableStateFlow(false)

    /** True for the duration of a [speak] utterance — the real signal behind Presence's Speaking state. */
    val speaking: StateFlow<Boolean> = _speaking

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ready = true
            tts.language = Locale.getDefault()
            tts.setSpeechRate(0.85f) // slow, per Philosophy §1/§4
            tts.setPitch(1.05f) // warm, not robotic-flat
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _speaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _speaking.value = false
                }

                override fun onError(utteranceId: String?) {
                    _speaking.value = false
                }
            })
        }
    }

    fun speak(text: String) {
        if (!ready) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lifeos_utterance")
    }

    /** One-shot listen. [onResult] fires with the best transcript; [onDone] always fires. */
    fun listen(onResult: (String) -> Unit, onError: (String) -> Unit, onDone: () -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Voice input isn't available on this phone.")
            onDone()
            return
        }
        recognizer?.destroy()
        val rec = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = rec

        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        rec.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val best = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (best.isNullOrBlank()) onError("I didn't catch that.") else onResult(best)
                onDone()
            }

            override fun onError(error: Int) {
                onError(
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                            "I didn't hear anything. Please try again."
                        else -> "Please try again."
                    },
                )
                onDone()
            }

            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        try {
            rec.startListening(intent)
        } catch (e: Exception) {
            onError("Please try again.")
            onDone()
        }
    }

    fun release() {
        try {
            tts.shutdown()
        } catch (e: Exception) {
            // Nothing to do — we're tearing down anyway.
        }
        try {
            recognizer?.destroy()
        } catch (e: Exception) {
            // Same.
        }
    }
}

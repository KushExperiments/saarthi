package com.lifeos.app.feature.voice

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import com.lifeos.app.core.interaction.WakeSignal
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val CHANNEL_ID = "juno_wake_word"
private const val NOTIFICATION_ID = 1001
private const val WAKE_WORD = "juno"
private const val RESTART_DELAY_MS = 300L

/**
 * Continuous background listening for the wake word only — deliberately
 * narrow. On hearing "Juno" it fires [WakeSignal] (so VoiceViewModel starts
 * a normal listen cycle through the exact same DialogueManager/
 * CommandRouter/TTS pipeline tap-to-talk already uses, instead of
 * duplicating that whole pipeline here where it can't be verified without a
 * device) and makes a best-effort attempt to bring the app to the
 * foreground.
 *
 * That "best-effort" qualifier matters: modern Android (10+) restricts
 * activities being started from a background service unless the app holds
 * assistant-role privileges, which a regular third-party app doesn't have.
 * startActivity() below may simply be silently blocked on some devices/
 * Android versions — there's no way to guarantee it from a plain foreground
 * service, and no way to verify which outcome happens without a real
 * device. The notification is the reliable fallback either way: it always
 * carries a tap-to-open PendingIntent, so hands-free degrades to
 * "say Juno, then tap the notification" rather than silently doing nothing.
 *
 * Requires FOREGROUND_SERVICE_MICROPHONE (Android 14+) and a visible
 * notification the whole time this runs — both present on purpose, not
 * incidental; Android will kill the service without them.
 */
@AndroidEntryPoint
class WakeWordService : Service() {

    @Inject lateinit var wakeSignal: WakeSignal

    private var recognizer: SpeechRecognizer? = null
    private var stopping = false
    private val restartHandler = Handler(mainLooper)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopping = false
        startListenLoop()
        return START_STICKY
    }

    override fun onDestroy() {
        stopping = true
        restartHandler.removeCallbacksAndMessages(null)
        try {
            recognizer?.destroy()
        } catch (e: Exception) {
            // Tearing down anyway.
        }
        recognizer = null
        super.onDestroy()
    }

    private fun startListenLoop() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            stopSelf()
            return
        }
        recognizer?.destroy()
        val rec = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer = rec

        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        }

        rec.setRecognitionListener(
            object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    val heard = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    if (heard.any { it.contains(WAKE_WORD, ignoreCase = true) }) onWakeWordHeard()
                    restartIfNotStopping()
                }

                override fun onError(error: Int) = restartIfNotStopping()
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            },
        )

        try {
            rec.startListening(recognizerIntent)
        } catch (e: Exception) {
            restartIfNotStopping()
        }
    }

    /** A real gap, not a busy loop — SpeechRecognizer can silently no-op if restarted immediately. */
    private fun restartIfNotStopping() {
        if (stopping) return
        restartHandler.postDelayed({ if (!stopping) startListenLoop() }, RESTART_DELAY_MS)
    }

    private fun onWakeWordHeard() {
        wakeSignal.fire()
        try {
            packageManager.getLaunchIntentForPackage(packageName)
                ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) }
                ?.let { startActivity(it) }
        } catch (e: Exception) {
            // Best-effort only — see the class doc. The notification's own
            // PendingIntent is the guaranteed way in if this is blocked.
        }
    }

    private fun openAppPendingIntent(): PendingIntent? {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(this, 0, launchIntent, flags)
    }

    private fun buildNotification(): android.app.Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(CHANNEL_ID, "Juno hands-free", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Juno is listening")
            .setContentText("Say \"Juno\" any time — tap to open.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openAppPendingIntent())
            .setOngoing(true)
            .build()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, WakeWordService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WakeWordService::class.java))
        }
    }
}

package com.saarthi.app

import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The loud, simple, full-screen "take your medicine" popup. */
class ReminderActivity : ComponentActivity() {

    private lateinit var voice: Voice
    private var medId = ""
    private var time = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Show over the lock screen and wake the screen.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true); setTurnScreenOn(true)
        }
        medId = intent.getStringExtra(Reminders.EXTRA_MED) ?: ""
        time = intent.getStringExtra(Reminders.EXTRA_TIME) ?: ""
        val name = intent.getStringExtra("name") ?: "your medicine"

        voice = Voice(this)
        val userName = Store.settings(this).userName.let { if (it.isNotBlank()) ", $it" else "" }
        window.decorView.postDelayed({
            voice.speak("It is time for your medicine$userName. Please take $name.")
        }, 700)
        vibrate()

        setContent {
            MaterialTheme(colorScheme = lightGreen()) {
                ReminderScreen(name,
                    onTook = {
                        Reminders.markTaken(this, medId, time)
                        voice.speak("Very good. Take your time.")
                        window.decorView.postDelayed({ finishAndRemoveTask() }, 1200)
                    },
                    onSnooze = {
                        Reminders.snooze(this, medId, time, 5)
                        finishAndRemoveTask()
                    })
            }
        }
    }

    private fun vibrate() {
        val v = getSystemService(VIBRATOR_SERVICE) as? Vibrator ?: return
        val pattern = longArrayOf(0, 500, 300, 500, 300, 500)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            v.vibrate(VibrationEffect.createWaveform(pattern, -1))
        else @Suppress("DEPRECATION") v.vibrate(pattern, -1)
    }

    override fun onDestroy() { super.onDestroy(); voice.release() }
}

@Composable
private fun ReminderScreen(name: String, onTook: () -> Unit, onSnooze: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("💊", fontSize = 96.sp)
        Spacer(Modifier.height(16.dp))
        Text("Time for your medicine", fontSize = 30.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(name, fontSize = 40.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onTook,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().height(90.dp)
        ) { Text("✅  I took it", fontSize = 30.sp) }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onSnooze,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().height(70.dp)
        ) { Text("⏰  Remind me in 5 minutes", fontSize = 22.sp) }
    }
}

fun lightGreen() = lightColorScheme(
    primary = Color(0xFF1F9D6B),
    onPrimary = Color.White,
    background = Color(0xFFF3F7F4),
    surface = Color.White
)

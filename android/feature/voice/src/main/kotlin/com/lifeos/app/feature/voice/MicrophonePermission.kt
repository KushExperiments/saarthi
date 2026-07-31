package com.lifeos.app.feature.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Tracks whether RECORD_AUDIO is granted and exposes [request] to launch the
 * system permission dialog. The manifest declaration alone (already present)
 * isn't enough on API 23+ — the app has to ask at runtime too, which nothing
 * in this project did yet despite every voice feature needing the mic.
 */
@Stable
class MicrophonePermissionState internal constructor(initialGranted: Boolean) {
    var granted: Boolean by mutableStateOf(initialGranted)
        internal set

    internal var onRequest: () -> Unit = {}

    fun request() = onRequest()
}

@Composable
fun rememberMicrophonePermissionState(): MicrophonePermissionState {
    val context = LocalContext.current
    val state = remember {
        MicrophonePermissionState(
            initialGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        state.granted = isGranted
    }
    state.onRequest = { launcher.launch(Manifest.permission.RECORD_AUDIO) }

    return state
}

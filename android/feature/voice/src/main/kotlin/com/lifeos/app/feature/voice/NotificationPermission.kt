package com.lifeos.app.feature.voice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
 * POST_NOTIFICATIONS is only a real runtime permission on API 33+ (below
 * that, notifications just work) — WakeWordService's persistent "Juno is
 * listening" notification needs this granted or it runs invisibly, which
 * defeats the point of a notification that's supposed to make an always-on
 * microphone service transparent rather than a surprise.
 */
@Stable
class NotificationPermissionState internal constructor(initialGranted: Boolean) {
    var granted: Boolean by mutableStateOf(initialGranted)
        internal set

    internal var onRequest: () -> Unit = {}

    fun request() = onRequest()
}

@Composable
fun rememberNotificationPermissionState(): NotificationPermissionState {
    val context = LocalContext.current
    val state = remember {
        val alreadyGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        NotificationPermissionState(initialGranted = alreadyGranted)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        state.granted = isGranted
    }
    state.onRequest = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            state.granted = true
        }
    }

    return state
}

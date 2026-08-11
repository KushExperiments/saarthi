package com.lifeos.app.feature.voice

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * The single most common real reason a foreground service dies on a real
 * device: OEM battery managers (Xiaomi/Oppo/Vivo/Samsung especially) kill
 * background work within minutes regardless of a correctly-declared
 * FOREGROUND_SERVICE, unless the app is explicitly whitelisted. This is a
 * one-tap system dialog requesting exactly that — not a permission in the
 * RECORD_AUDIO/POST_NOTIFICATIONS sense (it's a direct Settings Intent,
 * gated by the REQUEST_IGNORE_BATTERY_OPTIMIZATIONS manifest permission
 * rather than a runtime grant), so there's no ActivityResultContract for
 * it — just re-check PowerManager after returning from Settings.
 */
@Stable
class BatteryOptimizationState internal constructor(initialIgnoring: Boolean) {
    var ignoring: Boolean by mutableStateOf(initialIgnoring)
        internal set
}

@Composable
fun rememberBatteryOptimizationState(): BatteryOptimizationState {
    val context = LocalContext.current
    val powerManager = remember { context.getSystemService(PowerManager::class.java) }
    return remember {
        BatteryOptimizationState(initialIgnoring = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true)
    }
}

/**
 * Best-effort — declining this doesn't block hands-free from turning on,
 * it just means the service is more likely to get killed in the
 * background on aggressive OEM skins. Re-checks and updates [state] on
 * return so a subsequent recomposition sees whether it was actually
 * granted, without needing a formal ActivityResult callback.
 */
fun requestIgnoreBatteryOptimizations(context: android.content.Context, state: BatteryOptimizationState) {
    try {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Some OEMs don't implement this action at all — not fatal, hands-free
        // still works, just more exposed to being killed in the background.
    }
}

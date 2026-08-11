package com.lifeos.app

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.lifeos.app.feature.voice.VoiceSettingsPrefs
import com.lifeos.app.feature.voice.WakeWordService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LifeOSApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // WakeWordService only starts reactively when the Settings toggle is
        // flipped — without this, killing the app process (or a reboot)
        // would silently stop background listening while Settings still
        // shows the toggle as on, with no way back to life short of
        // manually flipping it off and back on. Re-checks permissions
        // rather than trusting the stored preference alone, in case they
        // were revoked from Android's own Settings after being enabled here.
        if (VoiceSettingsPrefs.isAlwaysListeningEnabled(this) && hasRequiredPermissions()) {
            WakeWordService.start(this)
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        val micGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        val notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return micGranted && notificationsGranted
    }
}

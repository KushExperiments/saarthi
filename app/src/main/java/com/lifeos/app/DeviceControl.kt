package com.lifeos.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings

/**
 * Real device control — the reason this app is native.
 * Torch, volume, brightness, calls, WhatsApp, SMS, YouTube, apps.
 */
object DeviceControl {

    // ---------- Torch ----------
    private var torchOn = false
    fun toggleTorch(ctx: Context): Boolean { setTorch(ctx, !torchOn); return torchOn }
    fun setTorch(ctx: Context, on: Boolean) {
        try {
            val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.firstOrNull { camId ->
                cm.getCameraCharacteristics(camId)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return
            cm.setTorchMode(id, on)
            torchOn = on
        } catch (_: Exception) { torchOn = false }
    }

    // ---------- Volume ----------
    fun volumeUp(ctx: Context)   = changeVolume(ctx, +1)
    fun volumeDown(ctx: Context) = changeVolume(ctx, -1)
    fun volumeMax(ctx: Context) {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), AudioManager.FLAG_SHOW_UI)
    }
    private fun changeVolume(ctx: Context, dir: Int) {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (dir > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI
        )
    }

    // ---------- Brightness (needs WRITE_SETTINGS, granted via a settings screen) ----------
    fun canWriteSettings(ctx: Context): Boolean =
        Settings.System.canWrite(ctx)

    fun requestWriteSettings(ctx: Context) {
        val i = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + ctx.packageName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
    }
    /** level 0.0 .. 1.0 */
    fun setBrightness(ctx: Context, level: Float): Boolean {
        if (!canWriteSettings(ctx)) return false
        val v = (level.coerceIn(0f, 1f) * 255).toInt()
        Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        Settings.System.putInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS, v)
        return true
    }

    // ---------- Communication ----------
    private fun clean(p: String) = p.replace(Regex("[^0-9+]"), "")

    /** Places the call directly (needs CALL_PHONE granted). */
    fun call(ctx: Context, phone: String) {
        val i = Intent(Intent.ACTION_CALL, Uri.parse("tel:" + clean(phone)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { ctx.startActivity(i) } catch (_: SecurityException) { dial(ctx, phone) }
    }
    /** Opens the dialer without auto-calling (fallback). */
    fun dial(ctx: Context, phone: String) {
        ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + clean(phone)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun whatsapp(ctx: Context, phone: String, message: String = "") {
        val num = clean(phone).removePrefix("+")
        val uri = Uri.parse("https://wa.me/$num" + if (message.isNotBlank()) "?text=" + Uri.encode(message) else "")
        openUrl(ctx, uri)
    }

    fun sms(ctx: Context, phone: String, message: String = "") {
        val i = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + clean(phone)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (message.isNotBlank()) i.putExtra("sms_body", message)
        try { ctx.startActivity(i) } catch (_: Exception) {}
    }

    fun youtube(ctx: Context, query: String) {
        val uri = if (query.isBlank())
            Uri.parse("https://www.youtube.com/")
        else
            Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(query))
        openUrl(ctx, uri)
    }

    fun openUrl(ctx: Context, uri: Uri) {
        try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {}
    }

    /** Open the phone's WiFi settings panel (toggling WiFi silently is blocked on modern Android). */
    fun openWifiSettings(ctx: Context) {
        ctx.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

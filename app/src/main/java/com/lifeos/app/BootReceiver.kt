package com.lifeos.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-arms all medicine reminders after the phone restarts or the app updates. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Reminders.scheduleAll(ctx)
    }
}

package com.saarthi.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fires when a medicine dose is due (or when nagging again). */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val medId = intent.getStringExtra(Reminders.EXTRA_MED) ?: return
        val time = intent.getStringExtra(Reminders.EXTRA_TIME) ?: return
        Reminders.onAlarm(ctx, medId, time)
    }
}

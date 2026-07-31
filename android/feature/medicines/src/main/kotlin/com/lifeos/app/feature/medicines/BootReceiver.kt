package com.lifeos.app.feature.medicines

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Re-arms every medicine's alarms after a reboot or app update — Reminder
 * State (Architecture §8) must survive process death, and a reboot is the
 * most complete form of that.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: MedicineRepository
    @Inject lateinit var scheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.observeAll().first().forEach { scheduler.scheduleAll(it) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

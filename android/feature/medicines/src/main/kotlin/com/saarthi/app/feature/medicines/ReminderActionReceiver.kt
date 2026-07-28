package com.saarthi.app.feature.medicines

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Handles the "I took it" action button tapped directly from the notification shade. */
@AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: MedicineRepository
    @Inject lateinit var scheduler: ReminderScheduler
    @Inject lateinit var notifier: ReminderNotifier

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MARK_TAKEN) return
        val medicineId = ReminderScheduler.medicineId(intent) ?: return
        val time = ReminderScheduler.time(intent) ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.markTaken(medicineId, time)
                scheduler.confirmedStopNagging(medicineId, time)
                notifier.cancel(medicineId, time)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

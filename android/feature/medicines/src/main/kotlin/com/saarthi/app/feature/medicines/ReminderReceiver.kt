package com.saarthi.app.feature.medicines

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fires when a medicine dose is due, or when nagging again (Reminders §12
 * of the original Architecture pipeline design). Never gives up on its
 * own — the nag continues until [ReminderActionReceiver] marks it taken.
 */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: MedicineRepository
    @Inject lateinit var scheduler: ReminderScheduler
    @Inject lateinit var notifier: ReminderNotifier

    override fun onReceive(context: Context, intent: Intent) {
        val medicineId = ReminderScheduler.medicineId(intent) ?: return
        val time = ReminderScheduler.time(intent) ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val medicine = repository.getById(medicineId)
                if (medicine == null || medicine.isConfirmed(time)) {
                    // Deleted, or already confirmed (e.g. via the in-app
                    // screen) between scheduling and firing — stop quietly.
                    return@launch
                }
                notifier.show(medicineId, medicine.name, time)
                scheduler.nagAgain(medicineId, time)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

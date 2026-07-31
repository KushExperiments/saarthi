package com.lifeos.app.feature.medicines

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.lifeos.app.core.interaction.ProsodyController
import com.lifeos.app.core.interaction.ReminderConversationFlow
import com.lifeos.app.core.interaction.VoiceEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val CHANNEL_ID = "medicine_reminders"
internal const val ACTION_MARK_TAKEN = "com.lifeos.app.feature.medicines.ACTION_MARK_TAKEN"

@Singleton
class ReminderNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val voiceEngine: VoiceEngine,
    private val reminderConversationFlow: ReminderConversationFlow,
    private val prosodyController: ProsodyController,
) {
    private val manager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun ensureChannel() {
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Medicine reminders",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Reminds you to take your medicine"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun show(medicineId: String, medicineName: String, time: String, attempt: Int) {
        ensureChannel()

        // Spoken escalation alongside the notification, not instead of it — full-screen
        // alert + voice + vibration together, per the medicine-reminder feature's own spec.
        val phrase = reminderConversationFlow.escalationPhrase(medicineName, attempt)
        voiceEngine.speak(prosodyController.insertBreathPauses(phrase))

        val fullScreenIntent = Intent(context, ReminderActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        ReminderScheduler.putExtras(fullScreenIntent, medicineId, time)
        val fullScreenPending = PendingIntent.getActivity(
            context,
            id(medicineId, time),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val takenIntent = Intent(context, ReminderActionReceiver::class.java).setAction(ACTION_MARK_TAKEN)
        ReminderScheduler.putExtras(takenIntent, medicineId, time)
        val takenPending = PendingIntent.getBroadcast(
            context,
            id(medicineId, time) + 1,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Time for your medicine")
            .setContentText("Please take $medicineName")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPending, true)
            .setContentIntent(fullScreenPending)
            .addAction(0, "I took it", takenPending)
            .build()

        manager.notify(id(medicineId, time), notification)
    }

    fun cancel(medicineId: String, time: String) {
        manager.cancel(id(medicineId, time))
    }

    private fun id(medicineId: String, time: String): Int = (medicineId + time).hashCode()
}

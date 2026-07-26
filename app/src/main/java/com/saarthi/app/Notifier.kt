package com.saarthi.app

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

/** Builds the heads-up / lock-screen medicine notification. */
object Notifier {

    private fun id(medId: String, time: String) = (medId + time).hashCode()

    fun showReminder(ctx: Context, medName: String, medId: String, time: String) {
        Reminders.ensureChannel(ctx)

        val open = Intent(ctx, ReminderActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(Reminders.EXTRA_MED, medId)
            .putExtra(Reminders.EXTRA_TIME, time)
            .putExtra("name", medName)
        val pi = PendingIntent.getActivity(
            ctx, id(medId, time), open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val n = NotificationCompat.Builder(ctx, Reminders.CHANNEL)
            .setSmallIcon(R.drawable.ic_pill)
            .setContentTitle("Medicine time")
            .setContentText("Please take $medName")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setOngoing(true)
            .setFullScreenIntent(pi, true)     // pops over the lock screen
            .setContentIntent(pi)
            .build()

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(id(medId, time), n)
    }

    fun cancel(ctx: Context, medId: String, time: String) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(id(medId, time))
    }
}

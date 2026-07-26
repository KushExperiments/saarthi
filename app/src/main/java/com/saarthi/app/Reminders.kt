package com.saarthi.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Medicine reminders that DO NOT give up.
 * When a dose is due it opens a full-screen reminder and, if it is not
 * confirmed, it fires again every few minutes until the elder taps
 * "I took it". Alarms survive the app being closed and a reboot.
 */
object Reminders {
    const val CHANNEL = "meds"
    const val EXTRA_MED = "medId"
    const val EXTRA_TIME = "time"
    private const val NAG_MS = 2 * 60 * 1000L        // re-remind every 2 minutes
    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun today(): String = dayFmt.format(Calendar.getInstance().time)

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL) == null) {
            val ch = NotificationChannel(CHANNEL, "Medicine reminders", NotificationManager.IMPORTANCE_HIGH)
            ch.description = "Reminds you to take your medicine"
            ch.enableVibration(true)
            nm.createNotificationChannel(ch)
        }
    }

    private fun reqCode(medId: String, time: String) = (medId + "@" + time).hashCode()

    private fun alarmPI(ctx: Context, medId: String, time: String): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java)
            .putExtra(EXTRA_MED, medId).putExtra(EXTRA_TIME, time)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(ctx, reqCode(medId, time), i, flags)
    }

    private fun am(ctx: Context) = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun setExact(ctx: Context, at: Long, pi: PendingIntent) {
        try {
            am(ctx).setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (_: SecurityException) {
            am(ctx).set(AlarmManager.RTC_WAKEUP, at, pi)   // fallback if exact not allowed
        }
    }

    /** Next date-time for a given "HH:mm". If already past today, use tomorrow. */
    private fun nextTrigger(time: String, allowToday: Boolean): Long {
        val (h, m) = time.split(":").map { it.toInt() }
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (!allowToday || c.timeInMillis <= System.currentTimeMillis())
            c.add(Calendar.DAY_OF_MONTH, 1)
        return c.timeInMillis
    }

    /** (Re)schedule every dose to its next occurrence. Call on launch and on boot. */
    fun scheduleAll(ctx: Context) {
        ensureChannel(ctx)
        val meds = Store.medicines(ctx)
        for (med in meds) for (time in med.times) {
            val takenToday = med.takenDate == today() && med.doneTimes.contains(time)
            // if not taken today and the time already passed, remind now-ish (soon), else at the time
            val at = nextTrigger(time, allowToday = !takenToday)
            setExact(ctx, at, alarmPI(ctx, med.id, time))
        }
    }

    /** Called by AlarmReceiver when a dose alarm fires. */
    fun onAlarm(ctx: Context, medId: String, time: String) {
        val med = Store.medicines(ctx).firstOrNull { it.id == medId }
        if (med == null) return
        val takenToday = med.takenDate == today() && med.doneTimes.contains(time)
        if (takenToday) {                          // already taken -> just schedule tomorrow
            setExact(ctx, nextTrigger(time, allowToday = false), alarmPI(ctx, medId, time))
            return
        }
        // Show the loud full-screen reminder.
        val i = Intent(ctx, ReminderActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(EXTRA_MED, medId).putExtra(EXTRA_TIME, time)
            .putExtra("name", med.name)
        ctx.startActivity(i)
        Notifier.showReminder(ctx, med.name, medId, time)
        // Nag again soon if not confirmed.
        setExact(ctx, System.currentTimeMillis() + NAG_MS, alarmPI(ctx, medId, time))
    }

    /** Mark a dose taken: stop nagging, schedule for tomorrow. */
    fun markTaken(ctx: Context, medId: String, time: String) {
        val meds = Store.medicines(ctx)
        val idx = meds.indexOfFirst { it.id == medId }
        if (idx >= 0) {
            val m = meds[idx]
            val done = if (m.takenDate == today()) m.doneTimes.toMutableList() else mutableListOf()
            if (!done.contains(time)) done.add(time)
            meds[idx] = m.copy(takenDate = today(), doneTimes = done)
            Store.saveMedicines(ctx, meds)
        }
        // cancel current (nag) alarm and set tomorrow
        am(ctx).cancel(alarmPI(ctx, medId, time))
        setExact(ctx, nextTrigger(time, allowToday = false), alarmPI(ctx, medId, time))
        Notifier.cancel(ctx, medId, time)
    }

    /** Snooze a dose: remind again after [minutes]. */
    fun snooze(ctx: Context, medId: String, time: String, minutes: Int = 5) {
        Notifier.cancel(ctx, medId, time)
        setExact(ctx, System.currentTimeMillis() + minutes * 60_000L, alarmPI(ctx, medId, time))
    }

    /** True if this dose is done for today. */
    fun isTaken(med: Medicine, time: String) =
        med.takenDate == today() && med.doneTimes.contains(time)
}

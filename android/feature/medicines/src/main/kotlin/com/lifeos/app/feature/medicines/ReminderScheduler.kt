package com.lifeos.app.feature.medicines

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.lifeos.app.core.cognitive.AdaptiveParamsStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

private const val EXTRA_MEDICINE_ID = "medicine_id"
private const val EXTRA_TIME = "time"
private const val EXTRA_ATTEMPT = "attempt"
private const val FIRST_ATTEMPT = 1

/**
 * Deterministic scheduling (Architecture §2's "Deterministic Business
 * Logic" principle) — never an AI/runtime judgment call about when an
 * alarm fires. The nag cadence itself IS adjustable (Cognitive OS §14's
 * bounded [AdaptiveParamsStore]), but only within a hardcoded 1-5 minute
 * floor/ceiling that this class has no way to bypass — adaptation of *how
 * often* it nags is not the same as an AI deciding *whether* to nag at all.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val adaptiveParams: AdaptiveParamsStore,
) {
    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleAll(medicine: Medicine) {
        medicine.times.forEach { time ->
            val alreadyConfirmedToday = medicine.isConfirmed(time)
            schedule(medicine.id, time, allowToday = !alreadyConfirmedToday)
        }
    }

    fun cancelAll(medicine: Medicine) {
        medicine.times.forEach { time -> cancel(medicine.id, time) }
    }

    /** Re-fires soon — used both for the "not confirmed yet" nag and for snooze. [attempt] is the nag this will be. */
    fun nagAgain(
        medicineId: String,
        time: String,
        attempt: Int,
        delayMs: Long = adaptiveParams.nagInterval().minutes * 60_000L,
    ) {
        setExact(System.currentTimeMillis() + delayMs, pendingIntent(medicineId, time, attempt))
    }

    fun confirmedStopNagging(medicineId: String, time: String) {
        cancel(medicineId, time)
        schedule(medicineId, time, allowToday = false)
    }

    private fun schedule(medicineId: String, time: String, allowToday: Boolean) {
        setExact(nextTrigger(time, allowToday), pendingIntent(medicineId, time, FIRST_ATTEMPT))
    }

    private fun cancel(medicineId: String, time: String) {
        // The request code is attempt-independent (same medicine+time always
        // resolves to the same PendingIntent), so any attempt value cancels it.
        alarmManager.cancel(pendingIntent(medicineId, time, FIRST_ATTEMPT))
    }

    private fun nextTrigger(time: String, allowToday: Boolean): Long {
        val parsed = LocalTime.parse(time)
        val now = LocalDateTime.now()
        var candidate = LocalDateTime.of(LocalDate.now(), parsed)
        if (!allowToday || candidate.isBefore(now)) candidate = candidate.plusDays(1)
        return candidate.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private fun setExact(atMillis: Long, pendingIntent: PendingIntent) {
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
        } catch (e: SecurityException) {
            // Exact-alarm permission not granted — fall back to an inexact
            // alarm rather than losing the reminder entirely (Architecture
            // §16: fail gracefully, never silently drop a safety-critical event).
            alarmManager.set(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
        }
    }

    private fun pendingIntent(medicineId: String, time: String, attempt: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .putExtra(EXTRA_MEDICINE_ID, medicineId)
            .putExtra(EXTRA_TIME, time)
            .putExtra(EXTRA_ATTEMPT, attempt)
        val requestCode = (medicineId + time).hashCode()
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        fun medicineId(intent: Intent): String? = intent.getStringExtra(EXTRA_MEDICINE_ID)
        fun time(intent: Intent): String? = intent.getStringExtra(EXTRA_TIME)
        fun attempt(intent: Intent): Int = intent.getIntExtra(EXTRA_ATTEMPT, FIRST_ATTEMPT)
        fun putExtras(intent: Intent, medicineId: String, time: String): Intent =
            intent.putExtra(EXTRA_MEDICINE_ID, medicineId).putExtra(EXTRA_TIME, time)
    }
}

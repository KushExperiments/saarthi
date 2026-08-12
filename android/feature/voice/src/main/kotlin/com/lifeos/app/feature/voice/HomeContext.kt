package com.lifeos.app.feature.voice

import com.lifeos.app.feature.medicines.Medicine

/**
 * Pure helpers behind the Home screen's greeting and contextual card —
 * kept Android/Compose-free so they're directly unit-testable. Nothing
 * here invents a fact: the greeting falls back to a name-less line when
 * none is known, and the contextual card is simply absent when nothing is
 * actually due, matching the approved redesign's "presence alone is the
 * default" principle.
 */

/** "Good morning" / "Good afternoon" / "Good evening", by [hour] (0-23), with [name] if known. */
fun timeOfDayGreeting(hour: Int, name: String?): String {
    val timeOfDay = when {
        hour < 12 -> "morning"
        hour < 17 -> "afternoon"
        else -> "evening"
    }
    return if (name.isNullOrBlank()) "Good $timeOfDay." else "Good $timeOfDay, $name."
}

/**
 * The single next unconfirmed dose today, or null if nothing is due — the
 * Home screen shows a contextual line only when this is non-null, never a
 * stacked list of everything true today.
 */
fun nextDueDescription(medicines: List<Medicine>): String? {
    val next = medicines
        .flatMap { medicine -> medicine.times.map { time -> medicine to time } }
        .filterNot { (medicine, time) -> medicine.isConfirmed(time) }
        .minByOrNull { (_, time) -> time }
        ?: return null
    val (medicine, time) = next
    return "Your ${medicine.name} is due at ${formatTime12(time)}."
}

/** "08:00" -> "8:00 AM". */
fun formatTime12(hhmm: String): String {
    val parts = hhmm.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: return hhmm
    val minute = parts.getOrNull(1) ?: "00"
    val amPm = if (hour < 12) "AM" else "PM"
    val hour12 = (hour % 12).let { if (it == 0) 12 else it }
    return "$hour12:$minute $amPm"
}

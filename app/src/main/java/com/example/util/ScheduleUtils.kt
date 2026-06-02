package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Pure scheduling helpers, kept free of Android dependencies so they can be unit tested
 * on the host JVM.
 */
object ScheduleUtils {

    /**
     * Converts a human schedule string like "8:00 AM" or "11:30 PM" into minutes since
     * midnight (0..1439) for correct chronological ordering. Unparseable values sort last.
     */
    fun timeToMinutes(raw: String): Int {
        val text = raw.trim()
        if (text.isEmpty()) return Int.MAX_VALUE

        val patterns = listOf("h:mm a", "hh:mm a", "H:mm", "HH:mm")
        for (pattern in patterns) {
            try {
                val fmt = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
                val parsed = fmt.parse(text) ?: continue
                val cal = Calendar.getInstance().apply { time = parsed }
                return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            } catch (_: Exception) {
                // try next pattern
            }
        }
        return Int.MAX_VALUE
    }

    /** True if the two epoch-millis timestamps fall on the same calendar day. */
    fun isSameDay(a: Long, b: Long): Boolean {
        val calA = Calendar.getInstance().apply { timeInMillis = a }
        val calB = Calendar.getInstance().apply { timeInMillis = b }
        return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
            calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
    }

    /** True if the given timestamp is on today's calendar day. */
    fun isToday(timestamp: Long, now: Long = System.currentTimeMillis()): Boolean =
        isSameDay(timestamp, now)

    /** Formats a log timestamp as a short clock time, e.g. "8:05 AM". */
    fun formatClock(timestamp: Long): String =
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
}

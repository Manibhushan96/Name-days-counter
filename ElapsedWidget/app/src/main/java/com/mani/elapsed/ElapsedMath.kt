package com.mani.elapsed

import java.text.NumberFormat
import java.util.Locale

/** All the derived units for a span between two instants. */
data class Elapsed(
    val totalMs: Long,
    val seconds: Long,
    val minutes: Long,
    val hours: Long,
    val days: Long,
    val weeks: Long,
    val remDaysInWeek: Long,
    val clockHours: Long,      // 0..23  -> hour part of the running day
    val clockMinutes: Long,    // 0..59
    val clockSeconds: Long,    // 0..59
    val percentOfCommonYear: Double
) {
    /** Milliseconds elapsed since the last whole-day boundary. Drives the widget Chronometer. */
    val msIntoCurrentDay: Long
        get() = totalMs - days * 86_400_000L
}

object ElapsedMath {

    private val nf: NumberFormat = NumberFormat.getIntegerInstance(Locale.US)

    fun group(v: Long): String = nf.format(v)

    fun percent(v: Double): String = String.format(Locale.US, "%,.2f%%", v)

    fun of(startMs: Long, nowMs: Long): Elapsed {
        val total = (nowMs - startMs).coerceAtLeast(0L)
        val s = total / 1000L
        val m = s / 60L
        val h = m / 60L
        val d = h / 24L
        return Elapsed(
            totalMs = total,
            seconds = s,
            minutes = m,
            hours = h,
            days = d,
            weeks = d / 7L,
            remDaysInWeek = d % 7L,
            clockHours = h % 24L,
            clockMinutes = m % 60L,
            clockSeconds = s % 60L,
            percentOfCommonYear = d * 100.0 / 365.0
        )
    }

    /** "11,122 days 05:23:11" style line. */
    fun headline(e: Elapsed): String = String.format(
        Locale.US, "%s d  %02d:%02d:%02d",
        group(e.days), e.clockHours, e.clockMinutes, e.clockSeconds
    )
}

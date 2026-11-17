package com.focusguard.app

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class ScheduleBoundaryKind { START, END, CLOCK_CHANGE }

data class ScheduleBoundary(
    val kind: ScheduleBoundaryKind,
    val localDate: LocalDate,
    val minuteOfDay: Int,
    val zoneId: String,
    val at: Instant
) {
    val token: String get() = "${at.toEpochMilli()}_${kind.name}_${minuteOfDay}_$zoneId"
}

/** Calculates the next transition from local wall-clock settings, including DST gaps/overlaps. */
object ScheduleBoundaryCalculator {
    fun useExactAlarms(sdkInt: Int, exactAccessGranted: Boolean): Boolean =
        sdkInt < 31 || exactAccessGranted

    fun nextBoundary(
        now: Instant,
        zone: ZoneId,
        startMinute: Int,
        endMinute: Int
    ): ScheduleBoundary? {
        val start = startMinute.coerceIn(0, 1439)
        val end = endMinute.coerceIn(0, 1439)
        if (start == end) return null // Equal times intentionally mean an all-day window.

        val today = now.atZone(zone).toLocalDate()
        val dailyBoundaries = (0L..2L).flatMap { dayOffset ->
            val date = today.plusDays(dayOffset)
            listOf(
                ScheduleBoundaryKind.START to start,
                ScheduleBoundaryKind.END to end
            ).flatMap { (kind, minute) ->
                resolveLocalBoundaryInstants(date, minute, zone).map { instant ->
                    ScheduleBoundary(kind, date, minute, zone.id, instant)
                }
            }
        }

        val clockChangeBoundary = zone.rules.nextTransition(now)?.let { transition ->
            val localBefore = transition.instant.minusSeconds(1).atZone(zone).toLocalTime()
            val localAfter = transition.instant.atZone(zone).toLocalTime()
            val activeBefore = isActiveAt(localBefore, start, end)
            val activeAfter = isActiveAt(localAfter, start, end)
            if (activeBefore == activeAfter) null else ScheduleBoundary(
                kind = ScheduleBoundaryKind.CLOCK_CHANGE,
                localDate = transition.instant.atZone(zone).toLocalDate(),
                minuteOfDay = localAfter.hour * 60 + localAfter.minute,
                zoneId = zone.id,
                at = transition.instant
            )
        }

        return (dailyBoundaries + listOfNotNull(clockChangeBoundary)).filter { it.at.isAfter(now) }
            .minByOrNull { it.at }
    }

    private fun resolveLocalBoundaryInstants(date: LocalDate, minute: Int, zone: ZoneId): List<Instant> {
        val localDateTime = date.atTime(LocalTime.of(minute / 60, minute % 60))
        val offsets = zone.rules.getValidOffsets(localDateTime)
        if (offsets.isNotEmpty()) return offsets.map(localDateTime::toInstant)
        val transition = zone.rules.getTransition(localDateTime) ?: return emptyList()
        // The schedule policy compares wall-clock minutes, so a skipped boundary takes effect
        // at the first valid local minute after the DST gap.
        return listOf(transition.dateTimeAfter.atZone(zone).toInstant())
    }

    private fun isActiveAt(time: LocalTime, start: Int, end: Int): Boolean {
        val minute = time.hour * 60 + time.minute
        return when {
            start == end -> true
            start < end -> minute >= start && minute < end
            else -> minute >= start || minute < end
        }
    }
}

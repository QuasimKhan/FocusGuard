package com.focusguard.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ScheduleBoundaryCalculatorTest {
    @Test fun findsOvernightStartThenEndOnNextDate() {
        val zone = ZoneId.of("Asia/Kolkata")
        val start = ScheduleBoundaryCalculator.nextBoundary(
            Instant.parse("2026-11-17T15:00:00Z"), zone, 22 * 60 + 30, 6 * 60 + 15
        )
        assertEquals(ScheduleBoundaryKind.START, start?.kind)
        assertEquals(LocalDate.parse("2026-11-17"), start?.localDate)
        assertEquals(Instant.parse("2026-11-17T17:00:00Z"), start?.at)

        val end = ScheduleBoundaryCalculator.nextBoundary(
            Instant.parse("2026-11-17T18:00:00Z"), zone, 22 * 60 + 30, 6 * 60 + 15
        )
        assertEquals(ScheduleBoundaryKind.END, end?.kind)
        assertEquals(LocalDate.parse("2026-11-18"), end?.localDate)
        assertEquals(Instant.parse("2026-11-18T00:45:00Z"), end?.at)
    }

    @Test fun localTimesFollowNewZoneAfterTimezoneChange() {
        val now = Instant.parse("2026-11-17T12:00:00Z")
        val boundary = ScheduleBoundaryCalculator.nextBoundary(now, ZoneId.of("Asia/Kolkata"), 18 * 60 + 37, 21 * 60 + 12)
        assertEquals(Instant.parse("2026-11-17T13:07:00Z"), boundary?.at)
    }

    @Test fun nonexistentDstLocalTimeTakesEffectAtFirstValidMinute() {
        val zone = ZoneId.of("America/New_York")
        val boundary = ScheduleBoundaryCalculator.nextBoundary(
            Instant.parse("2026-03-08T06:59:00Z"), zone, 2 * 60 + 30, 4 * 60
        )
        assertEquals(ScheduleBoundaryKind.START, boundary?.kind)
        assertEquals(Instant.parse("2026-03-08T07:00:00Z"), boundary?.at)
    }

    @Test fun allDayWindowHasNoDailyTransitionAlarm() {
        assertNull(ScheduleBoundaryCalculator.nextBoundary(
            Instant.parse("2026-11-17T12:00:00Z"), ZoneId.of("UTC"), 8 * 60, 8 * 60
        ))
    }

    @Test fun deniedExactAlarmAccessSelectsFallbackAndLegacyAndroidRemainsExact() {
        assertEquals(false, ScheduleBoundaryCalculator.useExactAlarms(35, exactAccessGranted = false))
        assertEquals(true, ScheduleBoundaryCalculator.useExactAlarms(35, exactAccessGranted = true))
        assertEquals(true, ScheduleBoundaryCalculator.useExactAlarms(30, exactAccessGranted = false))
    }

    @Test fun fallBackClockChangeRechecksWhenItCrossesAStartBoundary() {
        val boundary = ScheduleBoundaryCalculator.nextBoundary(
            Instant.parse("2026-11-01T05:45:00Z"),
            ZoneId.of("America/New_York"),
            90,
            150
        )
        assertEquals(ScheduleBoundaryKind.CLOCK_CHANGE, boundary?.kind)
        assertEquals(Instant.parse("2026-11-01T06:00:00Z"), boundary?.at)
    }

    @Test fun repeatedLocalStartCanBeScheduledInTheSecondDstOccurrence() {
        val boundary = ScheduleBoundaryCalculator.nextBoundary(
            Instant.parse("2026-11-01T06:01:00Z"),
            ZoneId.of("America/New_York"),
            90,
            150
        )
        assertEquals(ScheduleBoundaryKind.START, boundary?.kind)
        assertEquals(Instant.parse("2026-11-01T06:30:00Z"), boundary?.at)
    }
}

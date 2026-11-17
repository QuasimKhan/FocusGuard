package com.focusguard.app

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class LockPolicyTest {
    private val packageName = "com.example.target"
    private val scheduled = setOf(packageName)

    private fun input(
        now: String = "2026-11-17T18:37:00",
        masterActive: Boolean = true,
        protectionLocked: Boolean = false,
        mode: Int = FocusPrefs.APP_MODE_OPEN,
        scheduleEnabled: Boolean = true,
        start: Int = 18 * 60 + 37,
        end: Int = 21 * 60 + 12,
        pass: Boolean = false,
        timeZone: ZoneId = ZoneId.of("UTC")
    ) = LockPolicyInput(
        packageName = packageName,
        now = LocalDateTime.parse(now),
        masterActive = masterActive,
        protectionLocked = protectionLocked,
        globalMode = mode,
        scheduleEnabled = scheduleEnabled,
        scheduledPackages = scheduled,
        scheduleStartMinute = start,
        scheduleEndMinute = end,
        temporaryPassActive = pass,
        timeZone = timeZone
    )

    @Test fun inactiveEnforcementAllows() {
        assertEquals(LockAction.ALLOW, LockPolicy.evaluate(input(masterActive = false)).action)
    }

    @Test fun protectionLockKeepsEnforcementActiveWhenMasterIsOff() {
        assertEquals(LockAction.BLOCK, LockPolicy.evaluate(input(masterActive = false, protectionLocked = true)).action)
    }

    @Test fun globalBlockIsIndependentOfSchedule() {
        val result = LockPolicy.evaluate(input(now = "2026-11-17T23:00:00", mode = FocusPrefs.APP_MODE_BLOCKED))
        assertEquals(LockReason.GLOBAL_BLOCK, result.reason)
    }

    @Test fun activeScheduleBlocksAtInclusiveStartAndReportsEnd() {
        val result = LockPolicy.evaluate(input())
        assertEquals(LockAction.BLOCK, result.action)
        assertEquals(LockReason.DAILY_AUTO_LOCK, result.reason)
        assertEquals(Instant.parse("2026-11-17T21:12:00Z"), result.endsAt)
    }

    @Test fun scheduleEndIsExclusive() {
        assertEquals(LockAction.ALLOW, LockPolicy.evaluate(input(now = "2026-11-17T21:12:00")).action)
    }

    @Test fun overnightScheduleReturnsNextLocalEnd() {
        val result = LockPolicy.evaluate(input(now = "2026-11-17T23:00:00", start = 22 * 60 + 30, end = 6 * 60 + 15))
        assertEquals(LockReason.DAILY_AUTO_LOCK, result.reason)
        assertEquals(Instant.parse("2026-11-18T06:15:00Z"), result.endsAt)
    }

    @Test fun overnightMorningEndIsExclusive() {
        val result = LockPolicy.evaluate(input(now = "2026-11-18T06:15:00", start = 22 * 60 + 30, end = 6 * 60 + 15))
        assertEquals(LockAction.ALLOW, result.action)
    }

    @Test fun equalTimesMeanAllDayWithoutFabricatedEnd() {
        val result = LockPolicy.evaluate(input(now = "2026-11-17T12:00:00", start = 8 * 60, end = 8 * 60))
        assertEquals(LockReason.DAILY_AUTO_LOCK, result.reason)
        assertEquals(null, result.endsAt)
    }

    @Test fun globalPauseCanBeBypassedByTemporaryPassButScheduleCannot() {
        val paused = LockPolicy.evaluate(input(now = "2026-11-17T23:00:00", mode = FocusPrefs.APP_MODE_PAUSE))
        assertEquals(LockAction.PAUSE, paused.action)
        assertEquals(LockAction.ALLOW, LockPolicy.evaluate(input(now = "2026-11-17T23:00:00", mode = FocusPrefs.APP_MODE_PAUSE, pass = true)).action)
        assertEquals(LockAction.BLOCK, LockPolicy.evaluate(input(mode = FocusPrefs.APP_MODE_PAUSE, pass = true)).action)
    }

    @Test fun minuteBoundariesUseLocalWallClock() {
        assertEquals(LockAction.ALLOW, LockPolicy.evaluate(input(now = "2026-11-17T18:36:59")).action)
        assertEquals(LockAction.BLOCK, LockPolicy.evaluate(input(now = "2026-11-17T18:37:59")).action)
    }

    @Test fun remainingTimeUsesActualDstTransitionInstant() {
        val zone = ZoneId.of("America/New_York")
        val result = LockPolicy.evaluate(
            input(
                now = "2026-11-01T01:45:00",
                start = 90,
                end = 150,
                timeZone = zone
            ).copy(nowInstant = Instant.parse("2026-11-01T05:45:00Z"))
        )
        assertEquals(Instant.parse("2026-11-01T06:00:00Z"), result.endsAt)
    }
}

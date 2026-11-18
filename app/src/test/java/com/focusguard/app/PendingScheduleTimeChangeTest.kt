package com.focusguard.app

import org.junit.Assert.assertEquals
import org.junit.Test

class PendingScheduleTimeChangeTest {
    @Test fun proposedBoundaryIsSavedOnlyAfterConfirmation() {
        var savedStart = 6 * 60
        var rescheduleCount = 0
        val change = PendingScheduleTimeChange(isStart = true, proposedMinutes = 18 * 60 + 37) { isStart, minutes ->
            if (isStart) savedStart = minutes
            rescheduleCount++
        }

        assertEquals(6 * 60, savedStart)
        assertEquals(0, rescheduleCount)

        change.confirm()

        assertEquals(18 * 60 + 37, savedStart)
        assertEquals(1, rescheduleCount)
    }

    @Test fun cancellingKeepsTheSavedBoundaryAndPendingAlarm() {
        var savedEnd = 22 * 60
        var rescheduleCount = 0
        val change = PendingScheduleTimeChange(isStart = false, proposedMinutes = 21 * 60 + 12) { isStart, minutes ->
            if (!isStart) savedEnd = minutes
            rescheduleCount++
        }

        change.cancel()
        change.confirm()

        assertEquals(22 * 60, savedEnd)
        assertEquals(0, rescheduleCount)
    }
}

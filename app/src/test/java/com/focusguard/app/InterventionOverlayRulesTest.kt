package com.focusguard.app

import org.junit.Assert.assertEquals
import org.junit.Test

class InterventionOverlayRulesTest {
    @Test fun proceedWaitsForTheFullMindfulPause() {
        assertEquals(false, InterventionOverlayRules.canProceed(LockAction.PAUSE, 14_999L, 15_000L))
        assertEquals(true, InterventionOverlayRules.canProceed(LockAction.PAUSE, 15_000L, 15_000L))
        assertEquals(false, InterventionOverlayRules.canProceed(LockAction.BLOCK, 99_000L, 15_000L))
        assertEquals(1L, InterventionOverlayRules.remainingPauseSeconds(14_001L, 15_000L))
    }

    @Test fun exitingSuppressesThatTargetUntilFocusLeavesIt() {
        val dismissed = "com.example.target"
        assertEquals(true, InterventionOverlayRules.isDismissedFor(dismissed, dismissed))
        assertEquals(dismissed, InterventionOverlayRules.clearDismissalAfterFocusChange(dismissed, dismissed))
        assertEquals(null, InterventionOverlayRules.clearDismissalAfterFocusChange(dismissed, "com.android.launcher"))
        assertEquals(false, InterventionOverlayRules.isDismissedFor(dismissed, null))
    }
}

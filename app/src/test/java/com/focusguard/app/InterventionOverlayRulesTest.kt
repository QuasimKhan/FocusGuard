package com.focusguard.app

import org.junit.Assert.assertEquals
import org.junit.Test

class InterventionOverlayRulesTest {
    @Test fun overlayYieldsToSystemUiOnlyWhileAnInterventionIsActive() {
        assertEquals(true, InterventionOverlayRules.shouldYieldToSystemUi("com.android.systemui", "target"))
        assertEquals(false, InterventionOverlayRules.shouldYieldToSystemUi("com.android.systemui", null))
        assertEquals(false, InterventionOverlayRules.shouldYieldToSystemUi("com.android.launcher", "target"))
    }

    @Test fun staleInterventionClearsOnlyForAConfirmedDifferentForegroundWindow() {
        assertEquals(true, InterventionOverlayRules.shouldClearForDifferentForeground(
            "target", "com.focusguard.app", "com.focusguard.app", "target"
        ))
        assertEquals(true, InterventionOverlayRules.shouldClearForDifferentForeground(
            "target", "com.android.launcher", "com.android.launcher", "target"
        ))
        assertEquals(false, InterventionOverlayRules.shouldClearForDifferentForeground(
            "target", "com.android.systemui", "com.android.systemui", "target"
        ))
        assertEquals(false, InterventionOverlayRules.shouldClearForDifferentForeground(
            "target", "com.focusguard.app", "com.android.systemui", "target"
        ))
        assertEquals(false, InterventionOverlayRules.shouldClearForDifferentForeground(
            "target", "target", "target", "target"
        ))
    }

    @Test fun interventionRestoresOnlyForForegroundTargetWithAnActiveRule() {
        val scheduleBlock = LockDecision(LockAction.BLOCK, LockReason.DAILY_AUTO_LOCK)
        val globalBlock = LockDecision(LockAction.BLOCK, LockReason.GLOBAL_BLOCK)
        val allowed = LockDecision(LockAction.ALLOW, LockReason.NONE)

        assertEquals(true, InterventionOverlayRules.shouldRestoreIntervention("target", "target", scheduleBlock))
        assertEquals(false, InterventionOverlayRules.shouldRestoreIntervention("target", "target", allowed))
        assertEquals(true, InterventionOverlayRules.shouldRestoreIntervention("target", "target", globalBlock))
        assertEquals(false, InterventionOverlayRules.shouldRestoreIntervention("target", "launcher", scheduleBlock))
    }

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

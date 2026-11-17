package com.focusguard.app

import org.junit.Assert.assertEquals
import org.junit.Test

class ForegroundEventGateTest {
    private fun accepts(
        event: String? = "com.whatsapp",
        root: String? = "com.whatsapp",
        focused: String? = "com.whatsapp",
        active: String? = "com.whatsapp",
        state: Boolean = true,
        content: Boolean = false,
        boundary: Boolean = false
    ) = ForegroundEventGate.accepts(
        eventPackage = event,
        rootPackage = root,
        focusedWindowPackage = focused,
        activeWindowPackage = active,
        focusGuardPackage = "com.focusguard.app",
        isWindowStateChange = state,
        isWindowContentChange = content,
        isBoundaryRecheck = boundary
    )

    @Test fun genuineForegroundWindowIsAccepted() {
        assertEquals(true, accepts())
        assertEquals(true, accepts(state = false, content = true))
    }

    @Test fun backgroundEventAndMismatchedRootAreIgnored() {
        assertEquals(false, accepts(event = "com.whatsapp", root = "com.android.launcher"))
        assertEquals(false, accepts(event = "com.whatsapp", root = "com.whatsapp", focused = "com.android.systemui"))
    }

    @Test fun systemShadeImeAndFocusGuardWindowsAreIgnored() {
        assertEquals(false, accepts(event = "com.android.systemui", root = "com.android.systemui", focused = "com.android.systemui"))
        assertEquals(false, accepts(event = "com.whatsapp", root = "com.whatsapp", focused = "com.google.android.inputmethod.latin"))
        assertEquals(false, accepts(event = "com.focusguard.app", root = "com.focusguard.app", focused = "com.focusguard.app"))
    }

    @Test fun notificationAndOtherNonWindowEventsAreIgnored() {
        assertEquals(false, accepts(state = false, content = false))
    }

    @Test fun boundaryRecheckRequiresMatchingForegroundWindowEvidence() {
        assertEquals(true, accepts(event = null, state = false, content = false, boundary = true))
        assertEquals(false, accepts(event = null, focused = null, active = null, state = false, content = false, boundary = true))
    }

    @Test fun whatsappBackgroundAndContentEventsCannotTriggerItsHardBlock() {
        val backgroundEventAccepted = ForegroundEventGate.accepts(
            eventPackage = "com.whatsapp",
            rootPackage = "com.android.launcher",
            focusedWindowPackage = "com.android.launcher",
            activeWindowPackage = "com.android.launcher",
            focusGuardPackage = "com.focusguard.app",
            isWindowStateChange = true,
            isWindowContentChange = false
        )
        assertEquals(false, backgroundEventAccepted)

        assertEquals(true, accepts(state = false, content = true))
        assertEquals(false, ForegroundEventGate.allowsAppRuleEvaluation(isWindowStateChange = false, isBoundaryRecheck = false))
        assertEquals(true, ForegroundEventGate.allowsAppRuleEvaluation(isWindowStateChange = true, isBoundaryRecheck = false))
    }
}

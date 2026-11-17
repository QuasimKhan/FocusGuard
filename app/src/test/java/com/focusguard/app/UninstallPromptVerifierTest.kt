package com.focusguard.app

import org.junit.Assert.assertEquals
import org.junit.Test

class UninstallPromptVerifierTest {
    private val pkg = "com.focusguard.app"

    @Test fun verifiesTargetedUninstallConfirmationOnly() {
        assertEquals(
            true,
            UninstallPromptVerifier.isVerifiedFocusGuardPrompt(
                "com.google.android.packageinstaller",
                "FocusGuard",
                pkg,
                listOf("Do you want to uninstall FocusGuard?", "Cancel", "Uninstall")
            )
        )
    }

    @Test fun appInfoPageAndOtherAppPromptFailOpen() {
        assertEquals(
            false,
            UninstallPromptVerifier.isVerifiedFocusGuardPrompt(
                "com.android.settings", "FocusGuard", pkg, listOf("FocusGuard", "Uninstall", "Force stop")
            )
        )
        assertEquals(
            false,
            UninstallPromptVerifier.isVerifiedFocusGuardPrompt(
                "com.google.android.packageinstaller",
                "FocusGuard",
                pkg,
                listOf("Do you want to uninstall WhatsApp?", "Cancel", "Uninstall")
            )
        )
    }

    @Test fun ambiguousOemPromptWithoutFocusGuardIdentityFailsOpen() {
        assertEquals(
            false,
            UninstallPromptVerifier.isVerifiedFocusGuardPrompt(
                "com.oem.installer", "FocusGuard", pkg, listOf("Do you want to uninstall this app?", "Cancel", "Uninstall")
            )
        )
    }
}

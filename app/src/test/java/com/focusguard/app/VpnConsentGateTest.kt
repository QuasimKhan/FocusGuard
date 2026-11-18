package com.focusguard.app

import org.junit.Assert.assertEquals
import org.junit.Test

class VpnConsentGateTest {
    @Test fun grantedConsentStartsTheVpnService() {
        var starts = 0

        VpnConsentGate.onAuthorizationResult(resultCode = 1, successResultCode = 1) { starts++ }

        assertEquals(1, starts)
    }

    @Test fun declinedConsentLeavesTheVpnServiceOff() {
        var starts = 0

        VpnConsentGate.onAuthorizationResult(resultCode = 0, successResultCode = 1) { starts++ }

        assertEquals(0, starts)
    }
}

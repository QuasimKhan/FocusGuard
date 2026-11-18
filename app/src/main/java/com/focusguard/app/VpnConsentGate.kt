package com.focusguard.app

/** Runs VPN startup only after Android reports that the user granted consent. */
internal object VpnConsentGate {
    fun onAuthorizationResult(resultCode: Int, successResultCode: Int, startService: () -> Unit) {
        if (resultCode == successResultCode) startService()
    }
}

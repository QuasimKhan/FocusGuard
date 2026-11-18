package com.focusguard.app

/** One-shot commit for a user action that must wait for explicit confirmation. */
internal class PendingUserAction(private val commitAction: () -> Unit) {
    private var resolved = false

    fun confirm() {
        if (resolved) return
        resolved = true
        commitAction()
    }

    fun cancel() {
        resolved = true
    }
}

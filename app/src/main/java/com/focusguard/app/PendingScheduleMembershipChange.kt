package com.focusguard.app

/** Defers schedule persistence and a live policy reevaluation until confirmation. */
internal class PendingScheduleMembershipChange(
    nextPackages: Set<String>,
    persist: (Set<String>) -> Unit,
    reevaluateForeground: () -> Unit
) {
    private val commit = PendingUserAction {
        persist(nextPackages.toSet())
        reevaluateForeground()
    }

    fun confirm() = commit.confirm()

    fun cancel() = commit.cancel()
}

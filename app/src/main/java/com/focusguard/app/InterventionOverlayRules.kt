package com.focusguard.app

/** Small overlay state rules kept pure so pause timing and dismissal behavior can be tested. */
object InterventionOverlayRules {
    fun isTransientSystemUiInterruption(eventPackage: String?, overlayTarget: String?): Boolean =
        overlayTarget != null && eventPackage == "com.android.systemui"

    @Deprecated("System UI interruptions no longer detach the intervention overlay")
    fun shouldYieldToSystemUi(eventPackage: String?, overlayTarget: String?): Boolean =
        isTransientSystemUiInterruption(eventPackage, overlayTarget)

    fun shouldClearForDifferentForeground(
        targetPackage: String?,
        foregroundPackage: String?,
        focusedWindowPackage: String?,
        activeWindowPackage: String?
    ): Boolean {
        if (targetPackage == null || foregroundPackage == null || foregroundPackage == targetPackage) return false
        if (isTransientSystemWindow(foregroundPackage)) return false
        val visibleWindowPackage = focusedWindowPackage ?: activeWindowPackage ?: return false
        return visibleWindowPackage == foregroundPackage
    }

    private fun isTransientSystemWindow(packageName: String): Boolean =
        packageName in setOf(
            "com.android.systemui",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller"
        ) || packageName.startsWith("com.android.inputmethod") ||
            packageName.startsWith("com.google.android.inputmethod") ||
            packageName.startsWith("com.samsung.android.honeyboard")

    fun shouldRestoreIntervention(
        targetPackage: String,
        foregroundPackage: String?,
        decision: LockDecision
    ): Boolean = targetPackage == foregroundPackage && decision.action != LockAction.ALLOW

    fun isDismissedFor(packageName: String, dismissedPackage: String?): Boolean =
        packageName == dismissedPackage

    fun clearDismissalAfterFocusChange(dismissedPackage: String?, foregroundPackage: String?): String? =
        dismissedPackage?.takeIf { it == foregroundPackage }

    fun canProceed(action: LockAction, nowElapsedMs: Long, pauseEndsElapsedMs: Long): Boolean =
        action == LockAction.PAUSE && nowElapsedMs >= pauseEndsElapsedMs

    fun remainingPauseSeconds(nowElapsedMs: Long, pauseEndsElapsedMs: Long): Long =
        ((pauseEndsElapsedMs - nowElapsedMs).coerceAtLeast(0L) + 999L) / 1000L
}

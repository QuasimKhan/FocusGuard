package com.focusguard.app

/** Small overlay state rules kept pure so pause timing and dismissal behavior can be tested. */
object InterventionOverlayRules {
    fun isDismissedFor(packageName: String, dismissedPackage: String?): Boolean =
        packageName == dismissedPackage

    fun clearDismissalAfterFocusChange(dismissedPackage: String?, foregroundPackage: String?): String? =
        dismissedPackage?.takeIf { it == foregroundPackage }

    fun canProceed(action: LockAction, nowElapsedMs: Long, pauseEndsElapsedMs: Long): Boolean =
        action == LockAction.PAUSE && nowElapsedMs >= pauseEndsElapsedMs

    fun remainingPauseSeconds(nowElapsedMs: Long, pauseEndsElapsedMs: Long): Long =
        ((pauseEndsElapsedMs - nowElapsedMs).coerceAtLeast(0L) + 999L) / 1000L
}

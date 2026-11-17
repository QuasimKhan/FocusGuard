package com.focusguard.app

/** Pure gate for Accessibility events; callers supply active and focused window package evidence. */
object ForegroundEventGate {
    private val ignoredPackages = setOf(
        "com.android.systemui",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.miui.home",
        "com.sec.android.app.launcher",
        "com.google.android.apps.nexuslauncher"
    )

    fun allowsAppRuleEvaluation(isWindowStateChange: Boolean, isBoundaryRecheck: Boolean): Boolean =
        isWindowStateChange || isBoundaryRecheck

    fun accepts(
        eventPackage: String?,
        rootPackage: String?,
        focusedWindowPackage: String?,
        activeWindowPackage: String?,
        focusGuardPackage: String,
        isWindowStateChange: Boolean,
        isWindowContentChange: Boolean,
        isBoundaryRecheck: Boolean = false
    ): Boolean {
        if (!isBoundaryRecheck && !isWindowStateChange && !isWindowContentChange) return false
        val root = rootPackage?.takeIf { it.isNotBlank() } ?: return false
        if (root == focusGuardPackage || isIgnored(root)) return false
        if (!isBoundaryRecheck && eventPackage != root) return false
        if (eventPackage != null && eventPackage != root) return false

        // A focused system dialog/IME above the app wins; don't fall back to an underlying active window.
        val foregroundWindowPackage = focusedWindowPackage ?: activeWindowPackage ?: return false
        return foregroundWindowPackage == root && !isIgnored(foregroundWindowPackage)
    }

    private fun isIgnored(packageName: String): Boolean =
        packageName in ignoredPackages ||
            packageName.startsWith("com.android.inputmethod") ||
            packageName.startsWith("com.google.android.inputmethod") ||
            packageName.startsWith("com.samsung.android.honeyboard")
}

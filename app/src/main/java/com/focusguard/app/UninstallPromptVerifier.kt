package com.focusguard.app

object UninstallPromptVerifier {
    private val installerPackages = setOf(
        "com.android.packageinstaller",
        "com.android.settings",
        "com.google.android.packageinstaller",
        "com.miui.packageinstaller",
        "com.samsung.android.packageinstaller",
        "com.oplus.appdetail",
        "com.coloros.safecenter",
        "com.iqoo.secure"
    )

    fun isVerifiedFocusGuardPrompt(
        installerPackage: String,
        focusGuardLabel: String,
        focusGuardPackage: String,
        visibleTexts: List<String>
    ): Boolean {
        if (installerPackage !in installerPackages) return false
        val texts = visibleTexts.map { it.trim().lowercase() }.filter { it.isNotBlank() }
        val label = focusGuardLabel.trim().lowercase()
        val targetIdentified = texts.any { it == focusGuardPackage.lowercase() } ||
            (label.isNotBlank() && texts.any { it == label || it.contains(label) })
        val uninstallAction = texts.any { it.contains("uninstall") }
        val confirmation = texts.any { text ->
            text.contains("do you want to uninstall") ||
                text.contains("uninstall this app") ||
                text.contains("uninstall app") ||
                text.contains("app will be removed") ||
                text.contains("remove this app")
        }
        return targetIdentified && uninstallAction && confirmation
    }
}

package com.focusguard.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class FocusAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var isConnected = false
    }

    private var lastShortExitTime = 0L
    private var lastAppBlockTime = 0L
    private var lastWebBlockTime = 0L
    private var lastSettingsBlockTime = 0L

    private val settingsPackages = setOf(
        "com.android.settings",
        "com.miui.securitycenter",
        "com.google.android.packageinstaller",
        "com.samsung.android.settings",
        "com.oplus.securitypermission",
        "com.coloros.safecenter",
        "com.iqoo.secure"
    )

    private val protectedSettingsKeywords = listOf(
        "focusguard",
        "private dns",
        "device admin",
        "set time automatically",
        "date & time"
    )

    private val youtubeShortIds = listOf(
        "com.google.android.youtube:id/reel_watch_fragment_root",
        "com.google.android.youtube:id/shorts_player_container",
        "com.google.android.youtube:id/reel_recycler"
    )

    private val instagramReelIds = listOf(
        "com.instagram.android:id/clips_viewer_view_pager",
        "com.instagram.android:id/clips_video_container"
    )

    private val browserUrlBarIds = mapOf(
        "com.android.chrome" to "com.android.chrome:id/url_bar",
        "com.brave.browser" to "com.brave.browser:id/url_bar",
        "org.mozilla.firefox" to "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
        "com.microsoft.emmx" to "com.microsoft.emmx:id/url_bar",
        "com.opera.browser" to "com.opera.browser:id/url_field",
        "com.sec.android.app.sbrowser" to "com.sec.android.app.sbrowser:id/location_bar_edit_text"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        isConnected = true
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isConnected = false
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg == packageName) return

        val now = SystemClock.elapsedRealtime()
        val rootNode = rootInActiveWindow ?: return

        // 0. Tamper-Resistance Settings Watchdog (runs whenever Strict Lock, Study Window, or Locked Pomodoro is active)
        if (settingsPackages.contains(pkg) && FocusPrefs.isProtectionLocked(this)) {
            if (now - lastSettingsBlockTime > 700L && containsProtectedSettingText(rootNode, 0)) {
                lastSettingsBlockTime = now
                performGlobalAction(GLOBAL_ACTION_BACK)
                performGlobalAction(GLOBAL_ACTION_HOME)
                launchIntervention(blockedTarget = "Settings Protected During Focus", frictionPkg = null)
                return
            }
        }

        if (!FocusPrefs.isEnforcementActive(this)) return

        // 1. Hard App Blocker
        if (FocusPrefs.getBlockedPackages(this).contains(pkg)) {
            if (now - lastAppBlockTime > 600L) {
                lastAppBlockTime = now
                performGlobalAction(GLOBAL_ACTION_HOME)
                launchIntervention(blockedTarget = pkg, frictionPkg = null)
            }
            return
        }

        // 2. Mindful 15-Second Friction Pause (for semi-useful apps like WhatsApp)
        if (FocusPrefs.getPausedPackages(this).contains(pkg) && !FocusPrefs.hasActivePass(pkg)) {
            if (now - lastAppBlockTime > 1200L) {
                lastAppBlockTime = now
                performGlobalAction(GLOBAL_ACTION_HOME)
                launchIntervention(blockedTarget = null, frictionPkg = pkg)
            }
            return
        }

        // 3. Browser URL Bar Guard (blocks Incognito & cached DNS bypasses)
        val urlBarId = browserUrlBarIds[pkg]
        if (urlBarId != null && now - lastWebBlockTime > 800L) {
            val blockedDomain = findBlockedDomainInUrlBar(rootNode, urlBarId)
            if (blockedDomain != null) {
                lastWebBlockTime = now
                performGlobalAction(GLOBAL_ACTION_BACK)
                performGlobalAction(GLOBAL_ACTION_HOME)
                launchIntervention(blockedTarget = blockedDomain, frictionPkg = null)
                return
            }
        }

        // 4. YouTube Shorts & Instagram Reels Surgeon
        if (!FocusPrefs.isShortsBlocked(this)) return
        if (now - lastShortExitTime < 900L) return

        val isShortFeed = when (pkg) {
            "com.google.android.youtube" -> hasAnyViewId(rootNode, youtubeShortIds)
            "com.instagram.android" -> hasAnyViewId(rootNode, instagramReelIds)
            else -> false
        }

        if (isShortFeed) {
            lastShortExitTime = now
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    private fun containsProtectedSettingText(node: AccessibilityNodeInfo?, depth: Int): Boolean {
        if (node == null || depth > 10) return false

        val text = node.text?.toString()?.lowercase() ?: ""
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        for (keyword in protectedSettingsKeywords) {
            if (text.contains(keyword) || desc.contains(keyword)) {
                return true
            }
        }

        val count = node.childCount
        for (i in 0 until count) {
            val child = node.getChild(i) ?: continue
            val matched = containsProtectedSettingText(child, depth + 1)
            @Suppress("DEPRECATION")
            child.recycle()
            if (matched) return true
        }
        return false
    }

    private fun findBlockedDomainInUrlBar(root: AccessibilityNodeInfo, urlBarId: String): String? {
        val nodes = root.findAccessibilityNodeInfosByViewId(urlBarId) ?: return null
        var matched: String? = null

        for (node in nodes) {
            if (!node.isFocused) {
                val rawText = node.text?.toString()?.trim()?.lowercase() ?: ""
                val host = rawText
                    .removePrefix("https://")
                    .removePrefix("http://")
                    .removePrefix("www.")
                    .substringBefore("/")
                    .substringBefore("?")
                    .substringBefore(":")
                    .trimEnd('.')

                if (host.contains(".") && !host.contains(" ") && DomainBlocklist.isBlocked(this, host)) {
                    matched = host
                }
            }
            @Suppress("DEPRECATION")
            node.recycle()
        }
        return matched
    }

    private fun launchIntervention(blockedTarget: String?, frictionPkg: String?) {
        val lockIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("blocked_pkg", blockedTarget)
            putExtra("friction_pkg", frictionPkg)
        }
        startActivity(lockIntent)
    }

    private fun hasAnyViewId(root: AccessibilityNodeInfo, ids: List<String>): Boolean {
        for (viewId in ids) {
            val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
            if (!nodes.isNullOrEmpty()) {
                for (node in nodes) {
                    @Suppress("DEPRECATION")
                    node.recycle()
                }
                return true
            }
        }
        return false
    }

    override fun onInterrupt() {
    }
}

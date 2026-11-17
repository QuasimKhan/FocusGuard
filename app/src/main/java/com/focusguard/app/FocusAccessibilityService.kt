package com.focusguard.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

class FocusAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var isConnected = false

        @Volatile
        private var connectedService: FocusAccessibilityService? = null

        fun requestActiveWindowReevaluation() {
            val service = connectedService ?: return
            service.mainHandler.post { service.reevaluateActiveWindow() }
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private var lastShortExitTime = 0L
    private var lastAppBlockTime = 0L
    private var lastWebBlockTime = 0L
    private var lastSettingsBlockTime = 0L
    private var dismissedTarget: String? = null
    private var overlayTarget: String? = null
    private var overlayDecision: LockDecision? = null
    private var overlayView: FrameLayout? = null
    private var overlayTitle: TextView? = null
    private var overlayTargetLabel: TextView? = null
    private var overlayReason: TextView? = null
    private var overlayRemaining: TextView? = null
    private var overlayProceed: Button? = null
    private var protectionNoticeView: FrameLayout? = null
    private var pauseEndsElapsedMs = 0L
    private val overlayTick = object : Runnable {
        override fun run() {
            if (overlayView == null) return
            val target = overlayTarget
            if (target != null) {
                val decision = lockDecision(target)
                if (decision.action == LockAction.ALLOW) {
                    removeInterventionOverlay()
                    return
                }
                overlayDecision = decision
            }
            updateOverlayText()
            mainHandler.postDelayed(this, 1000L)
        }
    }
    private var trackedPkg: String? = null
    private var trackedStartElapsed = 0L
    private var lastTrackedTickElapsed = 0L
    private var nextSessionThresholdMs = 15 * 60_000L

    private val ignoredUsagePackages = setOf(
        "com.android.systemui",
        "com.miui.home",
        "com.sec.android.app.launcher",
        "com.google.android.apps.nexuslauncher",
        "com.android.settings",
        "com.miui.securitycenter",
        "com.android.dialer",
        "com.google.android.dialer"
    )

    private val settingsPackages = setOf(
        "com.android.settings",
        "com.android.packageinstaller",
        "com.miui.securitycenter",
        "com.google.android.packageinstaller",
        "com.miui.packageinstaller",
        "com.samsung.android.packageinstaller",
        "com.samsung.android.settings",
        "com.oplus.securitypermission",
        "com.coloros.safecenter",
        "com.iqoo.secure"
    )

    private val protectedSettingsKeywords = listOf(
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
        connectedService = this
        mainHandler.post { reevaluateActiveWindow() }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isConnected = false
        if (connectedService === this) connectedService = null
        removeInterventionOverlay()
        removeProtectionNotice()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        isConnected = false
        if (connectedService === this) connectedService = null
        removeInterventionOverlay()
        removeProtectionNotice()
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        val eventType = event.eventType
        val isWindowStateChange = eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        val isWindowContentChange = eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        if (!isWindowStateChange && !isWindowContentChange) return

        val now = SystemClock.elapsedRealtime()
        val rootNode = rootInActiveWindow ?: return
        val rootPackage = rootNode.packageName?.toString()
        if (!isConfirmedForeground(pkg, rootNode, isWindowStateChange, isWindowContentChange)) {
            dismissedTarget = InterventionOverlayRules.clearDismissalAfterFocusChange(dismissedTarget, rootPackage)
            return
        }
        trackAppUsageAndMaybeNudge(pkg, now)
        processForegroundWindow(
            pkg,
            rootNode,
            now,
            allowAppRules = ForegroundEventGate.allowsAppRuleEvaluation(isWindowStateChange, isBoundaryRecheck = false)
        )
    }

    private fun reevaluateActiveWindow() {
        val visibleOverlayTarget = overlayTarget
        if (visibleOverlayTarget != null) {
            val decision = lockDecision(visibleOverlayTarget)
            if (decision.action == LockAction.ALLOW) removeInterventionOverlay()
            else showInterventionOverlay(visibleOverlayTarget, decision)
            return
        }
        val rootNode = rootInActiveWindow ?: return
        val pkg = rootNode.packageName?.toString() ?: return
        if (!isConfirmedForeground(pkg, rootNode, false, false, isBoundaryRecheck = true)) {
            dismissedTarget = InterventionOverlayRules.clearDismissalAfterFocusChange(dismissedTarget, pkg)
            return
        }
        processForegroundWindow(
            pkg,
            rootNode,
            SystemClock.elapsedRealtime(),
            allowAppRules = ForegroundEventGate.allowsAppRuleEvaluation(isWindowStateChange = false, isBoundaryRecheck = true)
        )
    }

    private fun isConfirmedForeground(
        eventPackage: String?,
        root: AccessibilityNodeInfo,
        isWindowStateChange: Boolean,
        isWindowContentChange: Boolean,
        isBoundaryRecheck: Boolean = false
    ): Boolean {
        val rootPackage = root.packageName?.toString()
        val windowPackages = foregroundWindowPackages()
        return ForegroundEventGate.accepts(
            eventPackage = eventPackage,
            rootPackage = rootPackage,
            focusedWindowPackage = windowPackages.first,
            activeWindowPackage = windowPackages.second,
            focusGuardPackage = packageName,
            isWindowStateChange = isWindowStateChange,
            isWindowContentChange = isWindowContentChange,
            isBoundaryRecheck = isBoundaryRecheck
        )
    }

    private fun foregroundWindowPackages(): Pair<String?, String?> {
        var focusedPackage: String? = null
        var activePackage: String? = null
        for (window in windows) {
            val root = window.root ?: continue
            val windowPackage = root.packageName?.toString()
            @Suppress("DEPRECATION")
            root.recycle()
            if (window.isFocused && focusedPackage == null) focusedPackage = windowPackage
            if (window.isActive && activePackage == null) activePackage = windowPackage
        }
        return focusedPackage to activePackage
    }

    private fun processForegroundWindow(
        pkg: String,
        rootNode: AccessibilityNodeInfo,
        now: Long,
        allowAppRules: Boolean
    ) {
        dismissedTarget = InterventionOverlayRules.clearDismissalAfterFocusChange(dismissedTarget, pkg)
        if (overlayTarget != null && overlayTarget != pkg) removeInterventionOverlay()

        // 0. Tamper-Resistance Settings Watchdog (runs whenever Strict Lock, Study Window, or Locked Pomodoro is active)
        if (settingsPackages.contains(pkg) && FocusPrefs.isProtectionLocked(this)) {
            if (isVerifiedUninstallPrompt(pkg, rootNode)) {
                if (now - lastSettingsBlockTime > 700L) {
                    lastSettingsBlockTime = now
                    if (!clickCancelAction(rootNode, 0)) performGlobalAction(GLOBAL_ACTION_BACK)
                    showProtectionNotice()
                }
                return
            }
            if (now - lastSettingsBlockTime > 700L && containsProtectedSettingText(rootNode, 0)) {
                lastSettingsBlockTime = now
                performGlobalAction(GLOBAL_ACTION_BACK)
                performGlobalAction(GLOBAL_ACTION_HOME)
                launchIntervention(blockedTarget = "Settings Protected During Focus", frictionPkg = null)
                return
            }
        }

        val decision = if (allowAppRules) lockDecision(pkg) else LockDecision(LockAction.ALLOW, LockReason.NONE)

        // 1. Hard App Blocker
        if (decision.action == LockAction.BLOCK) {
            if (!InterventionOverlayRules.isDismissedFor(pkg, dismissedTarget) && (overlayTarget == pkg || now - lastAppBlockTime > 600L)) {
                lastAppBlockTime = now
                showInterventionOverlay(pkg, decision)
            }
            return
        }

        // 2. Mindful 15-Second Friction Pause (for semi-useful apps like WhatsApp)
        if (decision.action == LockAction.PAUSE) {
            if (!InterventionOverlayRules.isDismissedFor(pkg, dismissedTarget) && (overlayTarget == pkg || now - lastAppBlockTime > 1200L)) {
                lastAppBlockTime = now
                showInterventionOverlay(pkg, decision)
            }
            return
        }

        if (overlayTarget == pkg) removeInterventionOverlay()

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

    private fun showInterventionOverlay(pkg: String, decision: LockDecision) {
        val currentView = overlayView
        if (currentView != null && overlayTarget == pkg) {
            val previousAction = overlayDecision?.action
            overlayDecision = decision
            if (previousAction != LockAction.PAUSE && decision.action == LockAction.PAUSE) {
                pauseEndsElapsedMs = SystemClock.elapsedRealtime() + 15_000L
            }
            updateOverlayText()
            return
        }

        removeInterventionOverlay()
        overlayTarget = pkg
        overlayDecision = decision
        pauseEndsElapsedMs = if (decision.action == LockAction.PAUSE) {
            SystemClock.elapsedRealtime() + 15_000L
        } else 0L

        val targetLabel = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg)
        val overlayBackgroundColor = getColor(R.color.focus_overlay_background)
        val overlaySurfaceColor = getColor(R.color.focus_overlay_surface)
        val overlaySurfaceVariantColor = getColor(R.color.focus_overlay_surface_variant)
        val overlayTextColor = getColor(R.color.focus_overlay_text)
        val overlayMutedColor = getColor(R.color.focus_overlay_muted)
        val overlayAccentColor = getColor(R.color.focus_overlay_accent)
        val overlayOnAccentColor = getColor(R.color.focus_overlay_on_accent)
        val overlayAccentContainerColor = getColor(R.color.focus_overlay_accent_container)
        val overlayOnAccentContainerColor = getColor(R.color.focus_overlay_on_accent_container)
        val overlayOutlineColor = getColor(R.color.focus_overlay_outline)
        val overlayTimerColor = getColor(R.color.focus_overlay_timer)
        val root = FrameLayout(this).apply {
            setBackgroundColor(overlayBackgroundColor)
            isFocusable = true
            isFocusableInTouchMode = true
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(22), dp(22), dp(22), dp(22))
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            background = overlayRoundedBackground(overlaySurfaceColor, overlayOutlineColor, 20)
        }
        val brandRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val brandMark = ImageView(this).apply {
            setImageResource(R.drawable.ic_launcher_foreground)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dp(5), dp(5), dp(5), dp(5))
            background = overlayRoundedBackground(overlayAccentContainerColor, overlayOutlineColor, 14)
            contentDescription = null
        }
        brandRow.addView(brandMark, LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginEnd = dp(10) })
        val brandName = TextView(this).apply {
            text = "FocusGuard"
            textSize = 17f
            letterSpacing = 0.02f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(overlayAccentColor)
            gravity = Gravity.CENTER_VERTICAL
        }
        brandRow.addView(brandName, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(44)))
        val title = TextView(this).apply {
            textSize = 27f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(overlayTextColor)
            gravity = Gravity.CENTER
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
        val target = TextView(this).apply {
            text = targetLabel
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(overlayOnAccentContainerColor)
            gravity = Gravity.CENTER_VERTICAL or Gravity.START
            maxLines = 2
            setPadding(0, dp(2), 0, dp(2))
        }
        val targetCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = overlayRoundedBackground(overlaySurfaceVariantColor, overlayOutlineColor, 14)
        }
        val targetIcon = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(dp(3), dp(3), dp(3), dp(3))
            background = overlayRoundedBackground(overlaySurfaceColor, overlayOutlineColor, 12)
            contentDescription = "$targetLabel icon"
            val installedIcon = runCatching { packageManager.getApplicationIcon(pkg) }.getOrNull()
            setImageDrawable(installedIcon ?: getDrawable(R.drawable.ic_launcher_foreground))
        }
        targetCard.addView(targetIcon, LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginEnd = dp(12) })
        targetCard.addView(target, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        val reason = TextView(this).apply {
            textSize = 15f
            setTextColor(overlayTextColor)
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        val remaining = TextView(this).apply {
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(overlayTimerColor)
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(6), dp(4), dp(8))
        }
        val witty = TextView(this).apply {
            text = "Phone rakh de bhai, abhi focus ka time hai!"
            textSize = 13f
            setTextColor(overlayMutedColor)
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(4), dp(4), dp(16))
        }
        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val exit = Button(this).apply {
            text = "Exit to Home"
            setTextColor(overlayTextColor)
            backgroundTintList = ColorStateList.valueOf(overlaySurfaceVariantColor)
            setOnClickListener {
                dismissedTarget = overlayTarget
                removeInterventionOverlay()
                performGlobalAction(GLOBAL_ACTION_HOME)
            }
        }
        val proceed = Button(this).apply {
            text = "Proceed (5m Pass)"
            setTextColor(overlayOnAccentColor)
            backgroundTintList = ColorStateList.valueOf(overlayAccentColor)
            visibility = if (decision.action == LockAction.PAUSE) View.VISIBLE else View.GONE
            isEnabled = false
            setOnClickListener {
                val targetPackage = overlayTarget ?: return@setOnClickListener
                val currentAction = overlayDecision?.action ?: decision.action
                if (!InterventionOverlayRules.canProceed(currentAction, SystemClock.elapsedRealtime(), pauseEndsElapsedMs)) return@setOnClickListener
                FocusPrefs.grantTemporaryPass(targetPackage)
                dismissedTarget = null
                removeInterventionOverlay()
                requestActiveWindowReevaluation()
            }
        }
        buttons.addView(exit, LinearLayout.LayoutParams(0, dp(52), 1f).apply { marginEnd = dp(6) })
        buttons.addView(proceed, LinearLayout.LayoutParams(0, dp(52), 1f).apply { marginStart = dp(6) })
        panel.addView(brandRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(12) })
        panel.addView(title, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(14) })
        panel.addView(targetCard, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(12) })
        panel.addView(reason, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(4) })
        panel.addView(remaining, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        panel.addView(witty, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        panel.addView(buttons, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        column.addView(panel, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        root.addView(column, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER))

        overlayTitle = title
        overlayTargetLabel = target
        overlayReason = reason
        overlayRemaining = remaining
        overlayProceed = proceed
        val window = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }
        try {
            window.addView(root, params)
            overlayView = root
            if (decision.action == LockAction.BLOCK) {
                FocusNotifier.sendAppBlockAlert(this, pkg, decision.reason)
            }
            updateOverlayText()
            mainHandler.removeCallbacks(overlayTick)
            mainHandler.postDelayed(overlayTick, 1000L)
        } catch (_: WindowManager.BadTokenException) {
            overlayTarget = null
            overlayDecision = null
            clearOverlayReferences()
        } catch (_: SecurityException) {
            overlayTarget = null
            overlayDecision = null
            clearOverlayReferences()
        }
    }

    private fun updateOverlayText() {
        val decision = overlayDecision ?: return
        val action = decision.action
        overlayTitle?.text = if (action == LockAction.PAUSE) "Mindful pause" else "Blocked"
        overlayReason?.text = when (decision.reason) {
            LockReason.DAILY_AUTO_LOCK -> "The Daily Auto-Lock Window is active."
            LockReason.GLOBAL_BLOCK -> "You blocked this app to protect your focus."
            LockReason.MINDFUL_PAUSE -> "Take a breath before opening this app."
            LockReason.NONE -> "FocusGuard is helping you stay on track."
        }
        overlayProceed?.visibility = if (action == LockAction.PAUSE) View.VISIBLE else View.GONE
        if (action == LockAction.PAUSE) {
            val seconds = InterventionOverlayRules.remainingPauseSeconds(SystemClock.elapsedRealtime(), pauseEndsElapsedMs)
            overlayRemaining?.text = if (seconds > 0) "Breathing pause: ${seconds}s" else "Your 5-minute pass is ready."
            overlayProceed?.isEnabled = InterventionOverlayRules.canProceed(action, SystemClock.elapsedRealtime(), pauseEndsElapsedMs)
        } else if (decision.endsAt != null) {
            val seconds = Duration.between(Instant.now(), decision.endsAt).seconds.coerceAtLeast(0L)
            val minutes = (seconds + 59) / 60
            overlayRemaining?.text = if (minutes == 0L) "Window ends now" else "Available again in about ${minutes} min"
        } else {
            overlayRemaining?.text = "No timed unlock is set for this block."
        }
    }

    private fun removeInterventionOverlay() {
        mainHandler.removeCallbacks(overlayTick)
        val view = overlayView
        overlayView = null
        if (view != null) {
            runCatching { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(view) }
        }
        overlayTarget = null
        overlayDecision = null
        pauseEndsElapsedMs = 0L
        clearOverlayReferences()
    }

    private fun clearOverlayReferences() {
        overlayTitle = null
        overlayTargetLabel = null
        overlayReason = null
        overlayRemaining = null
        overlayProceed = null
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun overlayRoundedBackground(fillColor: Int, outlineColor: Int, radiusDp: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp).toFloat()
            setColor(fillColor)
            setStroke(dp(1), outlineColor)
        }

    private fun lockDecision(pkg: String): LockDecision {
        val now = ZonedDateTime.now()
        return LockPolicy.evaluate(LockPolicyInput(
            packageName = pkg,
            now = now.toLocalDateTime(),
            masterActive = FocusPrefs.isMasterActive(this),
            protectionLocked = FocusPrefs.isProtectionLocked(this),
            globalMode = FocusPrefs.getAppMode(this, pkg),
            scheduleEnabled = FocusPrefs.isScheduleEnabled(this),
            scheduledPackages = FocusPrefs.getScheduledPackages(this),
            scheduleStartMinute = FocusPrefs.getScheduleStartMinutes(this),
            scheduleEndMinute = FocusPrefs.getScheduleEndMinutes(this),
            temporaryPassActive = FocusPrefs.hasActivePass(pkg),
            timeZone = now.zone,
            nowInstant = now.toInstant()
        ))
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

    private fun isVerifiedUninstallPrompt(pkg: String, root: AccessibilityNodeInfo): Boolean {
        val label = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault("FocusGuard")
        val visibleTexts = mutableListOf<String>()
        collectVisibleTexts(root, 0, visibleTexts)
        return UninstallPromptVerifier.isVerifiedFocusGuardPrompt(
            installerPackage = pkg,
            focusGuardLabel = label,
            focusGuardPackage = packageName,
            visibleTexts = visibleTexts
        )
    }

    private fun collectVisibleTexts(node: AccessibilityNodeInfo?, depth: Int, results: MutableList<String>) {
        if (node == null || depth > 12 || results.size >= 160) return
        node.text?.toString()?.let(results::add)
        node.contentDescription?.toString()?.let(results::add)
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            collectVisibleTexts(child, depth + 1, results)
            @Suppress("DEPRECATION")
            child.recycle()
        }
    }

    private fun clickCancelAction(node: AccessibilityNodeInfo?, depth: Int): Boolean {
        if (node == null || depth > 12) return false
        val text = node.text?.toString()?.trim()?.lowercase().orEmpty()
        val description = node.contentDescription?.toString()?.trim()?.lowercase().orEmpty()
        if ((text == "cancel" || description == "cancel" || text == "don't uninstall") && node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            val clicked = clickCancelAction(child, depth + 1)
            @Suppress("DEPRECATION")
            child.recycle()
            if (clicked) return true
        }
        return false
    }

    private fun showProtectionNotice() {
        removeProtectionNotice()
        val root = FrameLayout(this).apply { setBackgroundColor(Color.rgb(16, 22, 34)) }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(16))
        }
        val title = TextView(this).apply {
            text = "Uninstall paused"
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }
        val message = TextView(this).apply {
            text = "FocusGuard is protected during your active focus lock. App Info remains available; uninstall can be tried again when this lock ends."
            textSize = 14f
            setTextColor(Color.rgb(203, 213, 225))
            setPadding(0, dp(8), 0, dp(4))
        }
        val dismiss = Button(this).apply {
            text = "Got it"
            setOnClickListener { removeProtectionNotice() }
        }
        column.addView(title)
        column.addView(message)
        column.addView(dismiss)
        root.addView(column, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.TOP))
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP }
        try {
            (getSystemService(WINDOW_SERVICE) as WindowManager).addView(root, params)
            protectionNoticeView = root
        } catch (_: WindowManager.BadTokenException) {
            protectionNoticeView = null
        } catch (_: SecurityException) {
            protectionNoticeView = null
        }
    }

    private fun removeProtectionNotice() {
        val view = protectionNoticeView ?: return
        protectionNoticeView = null
        runCatching { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(view) }
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

    private fun trackAppUsageAndMaybeNudge(pkg: String, nowElapsed: Long) {
        if (!FocusPrefs.isComicNudgesEnabled(this)) return
        if (ignoredUsagePackages.contains(pkg)) {
            trackedPkg = null
            return
        }

        if (trackedPkg != pkg) {
            trackedPkg = pkg
            trackedStartElapsed = nowElapsed
            lastTrackedTickElapsed = nowElapsed
            // Randomize threshold between 12 and 20 minutes so nudges feel spontaneous
            nextSessionThresholdMs = (12..20).random() * 60_000L
            return
        }

        val delta = nowElapsed - lastTrackedTickElapsed
        if (delta in 1_000L..120_000L) {
            lastTrackedTickElapsed = nowElapsed
            val totalDailyMs = FocusPrefs.addDistractionUsageMs(this, delta)
            val continuousMs = nowElapsed - trackedStartElapsed

            if (continuousMs >= nextSessionThresholdMs && FocusPrefs.canSendComicNudgeNow(this)) {
                FocusNotifier.sendComicSessionNudge(this, (continuousMs / 60_000L).toInt())
                trackedStartElapsed = nowElapsed
            } else if (totalDailyMs >= 45 * 60_000L && FocusPrefs.canSendComicNudgeNow(this, 45 * 60_000L)) {
                FocusNotifier.sendHeavyDailyUsageNudge(this, (totalDailyMs / 60_000L).toInt())
            }
        } else if (delta > 120_000L) {
            // Screen was off or user left the app
            trackedStartElapsed = nowElapsed
            lastTrackedTickElapsed = nowElapsed
        }
    }

    override fun onInterrupt() {
    }
}

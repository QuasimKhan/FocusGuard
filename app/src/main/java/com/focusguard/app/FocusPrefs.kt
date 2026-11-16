package com.focusguard.app

import android.content.Context
import android.os.SystemClock
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.ConcurrentHashMap

object FocusPrefs {
    private const val PREF_NAME = "focus_guard_prefs"
    private const val KEY_BLOCKED_PACKAGES = "blocked_packages"
    private const val KEY_PAUSED_PACKAGES = "paused_packages"
    private const val KEY_BLOCKED_DOMAINS = "blocked_domains"
    private const val KEY_BLOCK_SHORTS = "block_shorts"
    private const val KEY_MASTER_ACTIVE = "master_active"
    private const val KEY_VPN_ENABLED = "vpn_enabled"

    // Strict Lock & Cooldown Keys
    private const val KEY_LOCK_STATE = "lock_state"
    private const val KEY_COOLDOWN_DURATION_MS = "cooldown_duration_ms"
    private const val KEY_UNLOCK_WALL_MS = "unlock_wall_ms"
    private const val KEY_UNLOCK_START_ELAPSED_MS = "unlock_start_elapsed_ms"

    // Daily Study Schedule Keys
    private const val KEY_SCHEDULE_ENABLED = "schedule_enabled"
    private const val KEY_SCHEDULE_START_MIN = "schedule_start_min"
    private const val KEY_SCHEDULE_END_MIN = "schedule_end_min"

    // Pomodoro & Daily Stats Keys
    private const val KEY_POMO_END_ELAPSED_MS = "pomo_end_elapsed_ms"
    private const val KEY_POMO_PLANNED_MINS = "pomo_planned_mins"
    private const val KEY_POMO_LOCK_ENABLED = "pomo_lock_enabled"
    private const val KEY_STATS_DATE = "stats_date"
    private const val KEY_TODAY_FOCUSED_MINS = "today_focused_mins"
    private const val KEY_TODAY_SESSIONS = "today_sessions"
    private const val KEY_COMIC_NUDGES_ENABLED = "comic_nudges_enabled"
    private const val KEY_SCREEN_USAGE_DATE = "screen_usage_date"
    private const val KEY_TODAY_USAGE_MS = "today_usage_ms"
    private const val KEY_LAST_NUDGE_ELAPSED_MS = "last_nudge_elapsed_ms"

    const val LOCK_UNLOCKED = 0
    const val LOCK_ENGAGED = 1
    const val LOCK_COOLING_DOWN = 2

    const val APP_MODE_OPEN = 0
    const val APP_MODE_PAUSE = 1
    const val APP_MODE_BLOCKED = 2

    // In-memory 5-minute passes after completing the 15-second breathing pause
    private val temporaryPasses = ConcurrentHashMap<String, Long>()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isMasterActive(context: Context): Boolean =
        prefs(context).getBoolean(KEY_MASTER_ACTIVE, true)

    fun setMasterActive(context: Context, active: Boolean) {
        if (!active && isProtectionLocked(context)) return
        prefs(context).edit().putBoolean(KEY_MASTER_ACTIVE, active).apply()
    }

    fun isEnforcementActive(context: Context): Boolean =
        isMasterActive(context) || isProtectionLocked(context)

    fun isShortsBlocked(context: Context): Boolean =
        prefs(context).getBoolean(KEY_BLOCK_SHORTS, true)

    fun setShortsBlocked(context: Context, blocked: Boolean) {
        if (!blocked && isProtectionLocked(context)) return
        prefs(context).edit().putBoolean(KEY_BLOCK_SHORTS, blocked).apply()
    }

    fun isVpnEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_VPN_ENABLED, false)

    fun setVpnEnabled(context: Context, enabled: Boolean) {
        if (!enabled && isProtectionLocked(context)) return
        prefs(context).edit().putBoolean(KEY_VPN_ENABLED, enabled).apply()
    }

    // --- 3-Mode App Control (Open = 0, 15s Pause = 1, Blocked = 2) ---

    fun getBlockedPackages(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_BLOCKED_PACKAGES, emptySet()) ?: emptySet()

    fun getPausedPackages(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_PAUSED_PACKAGES, emptySet()) ?: emptySet()

    fun getAppMode(context: Context, packageName: String): Int {
        return when {
            getBlockedPackages(context).contains(packageName) -> APP_MODE_BLOCKED
            getPausedPackages(context).contains(packageName) -> APP_MODE_PAUSE
            else -> APP_MODE_OPEN
        }
    }

    fun setAppMode(context: Context, packageName: String, targetMode: Int) {
        val currentMode = getAppMode(context, packageName)
        // While locked, allow tightening restrictions (Open -> Pause -> Blocked) but forbid loosening
        if (isProtectionLocked(context) && targetMode < currentMode) return

        val blocked = getBlockedPackages(context).toMutableSet()
        val paused = getPausedPackages(context).toMutableSet()
        blocked.remove(packageName)
        paused.remove(packageName)

        when (targetMode) {
            APP_MODE_PAUSE -> paused.add(packageName)
            APP_MODE_BLOCKED -> blocked.add(packageName)
        }

        prefs(context).edit()
            .putStringSet(KEY_BLOCKED_PACKAGES, blocked)
            .putStringSet(KEY_PAUSED_PACKAGES, paused)
            .apply()
    }

    fun grantTemporaryPass(packageName: String, durationMs: Long = 5 * 60_000L) {
        temporaryPasses[packageName] = SystemClock.elapsedRealtime() + durationMs
    }

    fun hasActivePass(packageName: String): Boolean {
        val expiry = temporaryPasses[packageName] ?: return false
        if (SystemClock.elapsedRealtime() < expiry) return true
        temporaryPasses.remove(packageName)
        return false
    }

    // --- Custom Domain Rules ---

    fun getCustomBlockedDomains(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_BLOCKED_DOMAINS, emptySet()) ?: emptySet()

    fun addBlockedDomain(context: Context, rawDomain: String) {
        val clean = rawDomain.trim()
            .lowercase()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .substringBefore("/")
            .trimEnd('.')
        if (clean.isBlank() || (!clean.contains(".") && !clean.contains("*"))) return

        val current = getCustomBlockedDomains(context).toMutableSet()
        current.add(clean)
        prefs(context).edit().putStringSet(KEY_BLOCKED_DOMAINS, current).apply()
    }

    fun removeBlockedDomain(context: Context, domain: String) {
        if (isProtectionLocked(context)) return
        val current = getCustomBlockedDomains(context).toMutableSet()
        current.remove(domain)
        prefs(context).edit().putStringSet(KEY_BLOCKED_DOMAINS, current).apply()
    }

    // --- Daily Study Window ---

    fun isScheduleEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SCHEDULE_ENABLED, false)

    fun setScheduleEnabled(context: Context, enabled: Boolean) {
        if (!enabled && isProtectionLocked(context)) return
        prefs(context).edit().putBoolean(KEY_SCHEDULE_ENABLED, enabled).apply()
    }

    fun getScheduleStartMinutes(context: Context): Int =
        prefs(context).getInt(KEY_SCHEDULE_START_MIN, 6 * 60) // Default 06:00

    fun getScheduleEndMinutes(context: Context): Int =
        prefs(context).getInt(KEY_SCHEDULE_END_MIN, 22 * 60) // Default 22:00

    fun adjustScheduleMinutes(context: Context, isStart: Boolean, deltaMinutes: Int) {
        if (isProtectionLocked(context)) return
        val key = if (isStart) KEY_SCHEDULE_START_MIN else KEY_SCHEDULE_END_MIN
        val current = prefs(context).getInt(key, if (isStart) 360 else 1320)
        val updated = ((current + deltaMinutes) % 1440 + 1440) % 1440
        prefs(context).edit().putInt(key, updated).apply()
    }

    fun isStudyWindowCurrentlyActive(context: Context): Boolean {
        if (!isScheduleEnabled(context)) return false
        val start = getScheduleStartMinutes(context)
        val end = getScheduleEndMinutes(context)
        val now = LocalTime.now()
        val nowMins = now.hour * 60 + now.minute

        return when {
            start == end -> true
            start < end -> nowMins in start until end
            else -> nowMins >= start || nowMins < end
        }
    }

    // --- Pomodoro Deep-Work Timer ---

    fun isPomodoroLockEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_POMO_LOCK_ENABLED, true)

    fun setPomodoroLockEnabled(context: Context, enabled: Boolean) {
        if (!enabled && isPomodoroLockCurrentlyActive(context)) return
        prefs(context).edit().putBoolean(KEY_POMO_LOCK_ENABLED, enabled).apply()
    }

    fun startPomodoro(context: Context, minutes: Int) {
        val endElapsed = SystemClock.elapsedRealtime() + minutes * 60_000L
        prefs(context).edit()
            .putLong(KEY_POMO_END_ELAPSED_MS, endElapsed)
            .putInt(KEY_POMO_PLANNED_MINS, minutes)
            .putBoolean(KEY_MASTER_ACTIVE, true)
            .apply()
    }

    fun stopPomodoro(context: Context) {
        if (isPomodoroLockCurrentlyActive(context)) return
        prefs(context).edit().putLong(KEY_POMO_END_ELAPSED_MS, 0L).apply()
    }

    fun getPomodoroRemainingMs(context: Context): Long {
        val p = prefs(context)
        val endElapsed = p.getLong(KEY_POMO_END_ELAPSED_MS, 0L)
        if (endElapsed <= 0L) return 0L

        val now = SystemClock.elapsedRealtime()
        val remaining = endElapsed - now
        if (remaining <= 0L) {
            val planned = p.getInt(KEY_POMO_PLANNED_MINS, 25)
            recordCompletedSession(context, planned)
            p.edit().putLong(KEY_POMO_END_ELAPSED_MS, 0L).apply()
            return 0L
        }
        return remaining
    }

    fun isPomodoroRunning(context: Context): Boolean =
        getPomodoroRemainingMs(context) > 0L

    fun isPomodoroLockCurrentlyActive(context: Context): Boolean =
        isPomodoroLockEnabled(context) && isPomodoroRunning(context)

    private fun recordCompletedSession(context: Context, minutes: Int) {
        val p = prefs(context)
        val today = LocalDate.now().toString()
        val savedDate = p.getString(KEY_STATS_DATE, today)
        val prevMins = if (savedDate == today) p.getInt(KEY_TODAY_FOCUSED_MINS, 0) else 0
        val prevSessions = if (savedDate == today) p.getInt(KEY_TODAY_SESSIONS, 0) else 0

        p.edit()
            .putString(KEY_STATS_DATE, today)
            .putInt(KEY_TODAY_FOCUSED_MINS, prevMins + minutes)
            .putInt(KEY_TODAY_SESSIONS, prevSessions + 1)
            .apply()
    }

    fun getTodayFocusedMinutes(context: Context): Int {
        val p = prefs(context)
        val today = LocalDate.now().toString()
        return if (p.getString(KEY_STATS_DATE, today) == today) {
            p.getInt(KEY_TODAY_FOCUSED_MINS, 0)
        } else 0
    }

    fun getTodaySessions(context: Context): Int {
        val p = prefs(context)
        val today = LocalDate.now().toString()
        return if (p.getString(KEY_STATS_DATE, today) == today) {
            p.getInt(KEY_TODAY_SESSIONS, 0)
        } else 0
    }

    // --- Anti-Cheat Strict Lock & Cooldown State Machine ---

    fun engageStrictLock(context: Context, cooldownMinutes: Int) {
        val durationMs = cooldownMinutes * 60_000L
        prefs(context).edit()
            .putInt(KEY_LOCK_STATE, LOCK_ENGAGED)
            .putLong(KEY_COOLDOWN_DURATION_MS, durationMs)
            .putLong(KEY_UNLOCK_WALL_MS, 0L)
            .putLong(KEY_UNLOCK_START_ELAPSED_MS, 0L)
            .putBoolean(KEY_MASTER_ACTIVE, true)
            .apply()
    }

    fun requestUnlock(context: Context) {
        val p = prefs(context)
        if (p.getInt(KEY_LOCK_STATE, LOCK_UNLOCKED) != LOCK_ENGAGED) return
        val durationMs = p.getLong(KEY_COOLDOWN_DURATION_MS, 60 * 60_000L)
        p.edit()
            .putInt(KEY_LOCK_STATE, LOCK_COOLING_DOWN)
            .putLong(KEY_UNLOCK_WALL_MS, System.currentTimeMillis() + durationMs)
            .putLong(KEY_UNLOCK_START_ELAPSED_MS, SystemClock.elapsedRealtime())
            .apply()
    }

    fun cancelUnlockRequest(context: Context) {
        val p = prefs(context)
        if (p.getInt(KEY_LOCK_STATE, LOCK_UNLOCKED) != LOCK_COOLING_DOWN) return
        p.edit()
            .putInt(KEY_LOCK_STATE, LOCK_ENGAGED)
            .putLong(KEY_UNLOCK_WALL_MS, 0L)
            .putLong(KEY_UNLOCK_START_ELAPSED_MS, 0L)
            .apply()
    }

    fun onDeviceReboot(context: Context) {
        val p = prefs(context)
        if (p.getInt(KEY_LOCK_STATE, LOCK_UNLOCKED) == LOCK_COOLING_DOWN) {
            val wallRemaining = (p.getLong(KEY_UNLOCK_WALL_MS, 0L) - System.currentTimeMillis())
                .coerceAtLeast(0L)
            p.edit()
                .putLong(KEY_COOLDOWN_DURATION_MS, wallRemaining)
                .putLong(KEY_UNLOCK_START_ELAPSED_MS, SystemClock.elapsedRealtime())
                .apply()
        }
        p.edit().putLong(KEY_POMO_END_ELAPSED_MS, 0L).apply()
    }

    fun getRemainingCooldownMs(context: Context): Long {
        val p = prefs(context)
        if (p.getInt(KEY_LOCK_STATE, LOCK_UNLOCKED) != LOCK_COOLING_DOWN) return 0L

        val durationMs = p.getLong(KEY_COOLDOWN_DURATION_MS, 0L)
        val unlockWallMs = p.getLong(KEY_UNLOCK_WALL_MS, 0L)
        val startElapsedMs = p.getLong(KEY_UNLOCK_START_ELAPSED_MS, 0L)

        val wallRemaining = (unlockWallMs - System.currentTimeMillis()).coerceAtLeast(0L)
        val nowElapsed = SystemClock.elapsedRealtime()
        val monotonicRemaining = if (nowElapsed >= startElapsedMs) {
            (durationMs - (nowElapsed - startElapsedMs)).coerceAtLeast(0L)
        } else {
            wallRemaining
        }

        val remaining = maxOf(wallRemaining, monotonicRemaining)
        if (remaining <= 0L) {
            p.edit()
                .putInt(KEY_LOCK_STATE, LOCK_UNLOCKED)
                .putLong(KEY_UNLOCK_WALL_MS, 0L)
                .putLong(KEY_UNLOCK_START_ELAPSED_MS, 0L)
                .apply()
            return 0L
        }
        return remaining
    }

    fun getLockState(context: Context): Int {
        getRemainingCooldownMs(context)
        return prefs(context).getInt(KEY_LOCK_STATE, LOCK_UNLOCKED)
    }

    fun isStrictLockActive(context: Context): Boolean =
        getLockState(context) != LOCK_UNLOCKED

    fun isProtectionLocked(context: Context): Boolean =
        isStrictLockActive(context) ||
            isStudyWindowCurrentlyActive(context) ||
            isPomodoroLockCurrentlyActive(context)

    // --- Comic Focus Nudges & Usage Tracking ---

    fun isComicNudgesEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_COMIC_NUDGES_ENABLED, true)

    fun setComicNudgesEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_COMIC_NUDGES_ENABLED, enabled).apply()
    }

    fun addDistractionUsageMs(context: Context, deltaMs: Long): Long {
        val p = prefs(context)
        val today = LocalDate.now().toString()
        val savedDate = p.getString(KEY_SCREEN_USAGE_DATE, today)
        val currentTotal = if (savedDate == today) p.getLong(KEY_TODAY_USAGE_MS, 0L) else 0L
        val updated = currentTotal + deltaMs
        p.edit()
            .putString(KEY_SCREEN_USAGE_DATE, today)
            .putLong(KEY_TODAY_USAGE_MS, updated)
            .apply()
        return updated
    }

    fun canSendComicNudgeNow(context: Context, minCooldownMs: Long = 20 * 60_000L): Boolean {
        val p = prefs(context)
        val last = p.getLong(KEY_LAST_NUDGE_ELAPSED_MS, 0L)
        val now = SystemClock.elapsedRealtime()
        if (last > 0L && now >= last && (now - last) < minCooldownMs) return false
        p.edit().putLong(KEY_LAST_NUDGE_ELAPSED_MS, now).apply()
        return true
    }

    fun getConfiguredCooldownMinutes(context: Context): Int =
        (prefs(context).getLong(KEY_COOLDOWN_DURATION_MS, 60 * 60_000L) / 60_000L).toInt()
}

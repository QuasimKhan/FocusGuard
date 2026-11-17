package com.focusguard.app

import android.content.Context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FocusPrefsScheduleMigrationTest {
    private lateinit var context: Context
    private val preferences by lazy { context.getSharedPreferences("focus_guard_prefs", Context.MODE_PRIVATE) }

    @Before fun setUp() {
        context = RuntimeEnvironment.getApplication()
        preferences.edit().clear().commit()
    }

    @After fun tearDown() {
        preferences.edit().clear().commit()
    }

    @Test fun upgradeDefaultsScheduleSelectionWithoutChangingLegacyRulesOrTimes() {
        val blocked = setOf("com.example.blocked")
        val paused = setOf("com.example.paused")
        preferences.edit()
            .putStringSet("blocked_packages", blocked)
            .putStringSet("paused_packages", paused)
            .putBoolean("schedule_enabled", true)
            .putInt("schedule_start_min", 6 * 60 + 41)
            .putInt("schedule_end_min", 21 * 60 + 7)
            .commit()

        assertTrue(FocusPrefs.getScheduledPackages(context).isEmpty())
        assertEquals(FocusPrefs.APP_MODE_BLOCKED, FocusPrefs.getAppMode(context, "com.example.blocked"))
        assertEquals(FocusPrefs.APP_MODE_PAUSE, FocusPrefs.getAppMode(context, "com.example.paused"))
        assertEquals(FocusPrefs.APP_MODE_OPEN, FocusPrefs.getAppMode(context, "com.example.open"))
        assertTrue(FocusPrefs.isScheduleEnabled(context))
        assertEquals(6 * 60 + 41, FocusPrefs.getScheduleStartMinutes(context))
        assertEquals(21 * 60 + 7, FocusPrefs.getScheduleEndMinutes(context))
    }

    @Test fun scheduledMembershipPersistsWithoutChangingGlobalMode() {
        val packageName = "com.example.target"
        FocusPrefs.setAppMode(context, packageName, FocusPrefs.APP_MODE_PAUSE)
        FocusPrefs.setScheduledPackages(context, setOf(packageName))

        assertEquals(setOf(packageName), FocusPrefs.getScheduledPackages(context))
        assertEquals(FocusPrefs.APP_MODE_PAUSE, FocusPrefs.getAppMode(context, packageName))
    }

    @Test fun scheduleTimeCanBeSavedAtMinuteResolution() {
        FocusPrefs.setScheduleTimeMinutes(context, isStart = true, minutes = 18 * 60 + 37)
        assertEquals(18 * 60 + 37, FocusPrefs.getScheduleStartMinutes(context))
    }

    @Test fun boundaryTokenIsClaimedOnlyOnce() {
        assertEquals(true, FocusPrefs.claimScheduleBoundary(context, "2026-11-17_START_1117_Asia/Kolkata"))
        assertEquals(false, FocusPrefs.claimScheduleBoundary(context, "2026-11-17_START_1117_Asia/Kolkata"))
        assertEquals(true, FocusPrefs.claimScheduleBoundary(context, "2026-11-17_END_1260_Asia/Kolkata"))
    }

    @Test fun repeatedAppBlockAttemptsAreRateLimited() {
        val packageName = "com.example.blocked"
        assertEquals(true, FocusPrefs.claimAppBlockNotification(context, packageName))
        assertEquals(false, FocusPrefs.claimAppBlockNotification(context, packageName))
        assertEquals(true, FocusPrefs.claimAppBlockNotification(context, packageName, cooldownMs = 0L))
    }
}

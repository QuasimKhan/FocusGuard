package com.focusguard.app

import android.app.NotificationManager
import android.content.Context
import android.Manifest
import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FocusNotificationTest {
    @Test fun requiredFriendlyHinglishExamplesAreAvailable() {
        assertEquals(true, FocusNotificationCopy.scheduleStart.contains("Phone rakh de bhai, abhi focus ka time hai!"))
        assertEquals(true, FocusNotificationCopy.appBlocked.contains("Bas kar pagle, kaam pe dhyan de!"))
    }

    @Test fun appIconFallsBackToFocusGuardIconWhenTargetIsUnavailable() {
        val context: Context = RuntimeEnvironment.getApplication()
        assertNotNull(FocusNotifier.loadLargeIcon(context, "com.example.not.installed"))
    }

    @Test fun channelsKeepLegacyNudgeAndUpdateIdsAndRespectDisabledNudgeChannel() {
        val context: Context = RuntimeEnvironment.getApplication()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        FocusNotifier.initializeChannels(context)

        val nudge = manager.getNotificationChannel("focusguard_comic_nudges")
        val update = manager.getNotificationChannel("focusguard_updates")
        assertNotNull(nudge)
        assertNotNull(update)
        nudge.setImportance(NotificationManager.IMPORTANCE_NONE)
        manager.createNotificationChannel(nudge)

        FocusNotifier.initializeChannels(context)
        assertEquals(NotificationManager.IMPORTANCE_NONE, manager.getNotificationChannel("focusguard_comic_nudges").importance)
        assertNotNull(manager.getNotificationChannel("focusguard_schedule_transitions"))
        assertNotNull(manager.getNotificationChannel("focusguard_app_block_alerts"))
        assertFalse(FocusNotifier.readiness(context).nudgeAlertsAvailable)

        val locked = LockPolicy.evaluate(
            LockPolicyInput(
                packageName = "com.example.locked",
                now = java.time.LocalDateTime.parse("2026-11-17T12:00:00"),
                masterActive = true,
                protectionLocked = false,
                globalMode = FocusPrefs.APP_MODE_BLOCKED,
                scheduleEnabled = false,
                scheduledPackages = emptySet(),
                scheduleStartMinute = 360,
                scheduleEndMinute = 1320,
                temporaryPassActive = false
            )
        )
        assertEquals(LockAction.BLOCK, locked.action)
    }

    @Test fun api33PermissionReadinessRefreshesWhenPermissionChanges() {
        val context: Context = RuntimeEnvironment.getApplication()
        assertFalse(FocusNotifier.readiness(context).permissionGranted)
        shadowOf(context.applicationContext as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertTrue(FocusNotifier.readiness(context).permissionGranted)
        shadowOf(context.applicationContext as Application).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        assertFalse(FocusNotifier.readiness(context).permissionGranted)
    }
}

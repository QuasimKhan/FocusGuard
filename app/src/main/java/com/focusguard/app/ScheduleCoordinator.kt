package com.focusguard.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.Instant
import java.time.ZoneId

object ScheduleCoordinator {
    private const val REQUEST_CODE = 4301
    const val ACTION_BOUNDARY = "com.focusguard.app.action.SCHEDULE_BOUNDARY"
    const val EXTRA_BOUNDARY_TOKEN = "schedule_boundary_token"
    const val EXTRA_BOUNDARY_KIND = "schedule_boundary_kind"

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarms.canScheduleExactAlarms()
    }

    fun reschedule(context: Context, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = boundaryPendingIntent(context)
        alarmManager.cancel(pendingIntent)

        if (!FocusPrefs.isScheduleEnabled(context) || FocusPrefs.getScheduledPackages(context).isEmpty()) return
        val boundary = ScheduleBoundaryCalculator.nextBoundary(
            now = now,
            zone = zone,
            startMinute = FocusPrefs.getScheduleStartMinutes(context),
            endMinute = FocusPrefs.getScheduleEndMinutes(context)
        ) ?: return

        val intent = Intent(context, ScheduleBoundaryReceiver::class.java)
            .setAction(ACTION_BOUNDARY)
            .putExtra(EXTRA_BOUNDARY_TOKEN, boundary.token)
            .putExtra(EXTRA_BOUNDARY_KIND, boundary.kind.name)
        val receiverPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAtMillis = boundary.at.toEpochMilli()
        val exactAccessGranted = canScheduleExact(context)
        val exactRequested = ScheduleBoundaryCalculator.useExactAlarms(Build.VERSION.SDK_INT, exactAccessGranted)
        if (exactRequested) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, receiverPendingIntent)
                return
            } catch (_: SecurityException) {
                // Permission may be revoked between the check and the call; preserve a best-effort alert.
            }
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, receiverPendingIntent)
    }

    fun onBoundaryDelivered(context: Context, token: String?, kind: String?) {
        if (token.isNullOrBlank() || kind.isNullOrBlank() || !FocusPrefs.claimScheduleBoundary(context, token)) return
        val boundaryKind = runCatching { ScheduleBoundaryKind.valueOf(kind) }.getOrNull() ?: return
        val windowActive = FocusPrefs.isStudyWindowCurrentlyActive(context)
        val shouldNotify = when (boundaryKind) {
            ScheduleBoundaryKind.START -> windowActive
            ScheduleBoundaryKind.END -> !windowActive
            ScheduleBoundaryKind.CLOCK_CHANGE -> false
        }
        if (shouldNotify) FocusNotifier.sendScheduleTransition(context, boundaryKind)
        // Recompute from current preferences and local time; stale alarms cannot shift the schedule.
        reschedule(context)
        FocusAccessibilityService.requestActiveWindowReevaluation()
    }

    private fun boundaryPendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, ScheduleBoundaryReceiver::class.java).setAction(ACTION_BOUNDARY),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    ) ?: PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, ScheduleBoundaryReceiver::class.java).setAction(ACTION_BOUNDARY),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

package com.focusguard.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import kotlin.random.Random

data class NotificationReadiness(
    val appEnabled: Boolean,
    val permissionGranted: Boolean,
    val scheduleChannelEnabled: Boolean,
    val blockChannelEnabled: Boolean,
    val nudgeChannelEnabled: Boolean,
    val updateChannelEnabled: Boolean
) {
    private val systemEnabled get() = appEnabled && permissionGranted
    val scheduleAlertsAvailable get() = systemEnabled && scheduleChannelEnabled
    val appBlockAlertsAvailable get() = systemEnabled && blockChannelEnabled
    val nudgeAlertsAvailable get() = systemEnabled && nudgeChannelEnabled
    val canPost get() = systemEnabled
}

object FocusNotifier {
    private const val CHANNEL_NUDGE_ID = "focusguard_comic_nudges"
    private const val CHANNEL_UPDATE_ID = "focusguard_updates"
    private const val CHANNEL_SCHEDULE_ID = "focusguard_schedule_transitions"
    private const val CHANNEL_BLOCK_ID = "focusguard_app_block_alerts"
    private const val NOTIF_ID_NUDGE = 202
    private const val NOTIF_ID_UPDATE = 203
    private const val NOTIF_ID_SCHEDULE = 204


    private val sessionNudges = listOf(
        "Plot Twist: Your Goals Miss You" to "You've been scrolling for %d mins straight. Your textbook just filed a missing person report.",
        "Breaking News from Reality" to "%d mins on this app and zero chapters finished. Bold strategy, let's see if it pays off.",
        "Achievement Unlocked: Professional Scroller" to "You survived %d mins here! Unfortunately, exams and deadlines don't accept screen-time points.",
        "Your Future Self Called..." to "They asked why you spent %d mins here instead of building something legendary. I didn't snitch, go study.",
        "Thumb Workout Complete!" to "%d mins of cardio for your thumb. Now give your brain 25 minutes of actual work.",
        "Are You Waiting for a Sign?" to "This notification is it. Close this app (%d mins used) and knock out one real task."
    )

    private val heavyUsageNudges = listOf(
        "Screen Time Alert: Legendary Status" to "You've clocked %d mins on non-work apps today. Even your phone battery is judging you.",
        "Friendly Reality Check" to "%d mins of screen time today. Imagine if even half of that went into your main goal!",
        "Houston, We Have a Scroll Problem" to "%d mins on distracting apps today. Time to switch to Study OS before the day disappears."
    )

    fun readiness(context: Context): NotificationReadiness {
        val manager = context.getSystemService(NotificationManager::class.java)
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return NotificationReadiness(
            appEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled(),
            permissionGranted = permissionGranted,
            scheduleChannelEnabled = isChannelEnabled(manager, CHANNEL_SCHEDULE_ID),
            blockChannelEnabled = isChannelEnabled(manager, CHANNEL_BLOCK_ID),
            nudgeChannelEnabled = isChannelEnabled(manager, CHANNEL_NUDGE_ID),
            updateChannelEnabled = isChannelEnabled(manager, CHANNEL_UPDATE_ID)
        )
    }

    private fun canNotify(context: Context): Boolean = readiness(context).canPost

    private fun isChannelEnabled(manager: NotificationManager?, channelId: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val channel = manager?.getNotificationChannel(channelId) ?: return true
        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    fun initializeChannels(context: Context) = ensureChannels(context)

    private fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val nudgeChannel = NotificationChannel(
            CHANNEL_NUDGE_ID,
            "Comic Focus Nudges",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Witty reminders when spending too much time on distracting apps"
        }
        val updateChannel = NotificationChannel(
            CHANNEL_UPDATE_ID,
            "App Updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications when a new FocusGuard release is published on GitHub"
        }
        val scheduleChannel = NotificationChannel(
            CHANNEL_SCHEDULE_ID,
            "Daily Auto-Lock",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Start and end alerts for the Daily Auto-Lock Window"
        }
        val blockChannel = NotificationChannel(
            CHANNEL_BLOCK_ID,
            "App Block Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when a selected app is blocked by an active focus rule"
        }
        manager.createNotificationChannel(nudgeChannel)
        manager.createNotificationChannel(updateChannel)
        manager.createNotificationChannel(scheduleChannel)
        manager.createNotificationChannel(blockChannel)
    }

    fun sendComicSessionNudge(context: Context, continuousMinutes: Int) {
        if (!canNotify(context)) return
        ensureChannels(context)

        val (title, template) = sessionNudges[Random.nextInt(sessionNudges.size)]
        val body = String.format(template, continuousMinutes)
        postNotification(context, CHANNEL_NUDGE_ID, NOTIF_ID_NUDGE, title, body, null)
    }

    fun sendHeavyDailyUsageNudge(context: Context, totalDailyMinutes: Int) {
        if (!canNotify(context)) return
        ensureChannels(context)

        val (title, template) = heavyUsageNudges[Random.nextInt(heavyUsageNudges.size)]
        val body = String.format(template, totalDailyMinutes)
        postNotification(context, CHANNEL_NUDGE_ID, NOTIF_ID_NUDGE, title, body, null)
    }

    fun sendUpdateNotification(context: Context, update: ReleaseUpdate) {
        if (!canNotify(context)) return
        ensureChannels(context)

        val downloadIntent = Intent(Intent.ACTION_VIEW, Uri.parse(update.downloadUrl))
        postNotification(
            context = context,
            channelId = CHANNEL_UPDATE_ID,
            notifId = NOTIF_ID_UPDATE,
            title = "FocusGuard ${update.latestTag} is Live!",
            body = "Tap to download the latest features & upgrades directly from GitHub.",
            customIntent = downloadIntent
        )
    }

    fun sendScheduleTransition(context: Context, kind: ScheduleBoundaryKind) {
        if (!canNotify(context)) return
        ensureChannels(context)
        val starting = kind == ScheduleBoundaryKind.START
        val body = (if (starting) FocusNotificationCopy.scheduleStart else FocusNotificationCopy.scheduleEnd)
            .randomOrNull() ?: return
        postNotification(
            context = context,
            channelId = CHANNEL_SCHEDULE_ID,
            notifId = NOTIF_ID_SCHEDULE,
            title = if (starting) "Daily Auto-Lock started" else "Daily Auto-Lock ended",
            body = body,
            customIntent = null
        )
    }

    fun sendAppBlockAlert(context: Context, packageName: String, reason: LockReason) {
        if (!canNotify(context) || !FocusPrefs.claimAppBlockNotification(context, packageName)) return
        ensureChannels(context)
        val label = runCatching {
            context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
        val reasonText = when (reason) {
            LockReason.DAILY_AUTO_LOCK -> "Daily Auto-Lock Window active"
            LockReason.GLOBAL_BLOCK -> "Your app block is active"
            LockReason.MINDFUL_PAUSE -> "Take a short mindful pause"
            LockReason.NONE -> "Your focus rule is active"
        }
        val body = "${FocusNotificationCopy.appBlocked.random()} $reasonText."
        postNotification(
            context = context,
            channelId = CHANNEL_BLOCK_ID,
            notifId = appNotificationId(packageName),
            title = "$label is taking a focus break",
            body = body,
            customIntent = null,
            largeIcon = loadLargeIcon(context, packageName)
        )
    }

    private fun postNotification(
        context: Context,
        channelId: String,
        notifId: Int,
        title: String,
        body: String,
        customIntent: Intent?,
        largeIcon: Bitmap? = null
    ) {
        val targetIntent = customIntent ?: Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            targetIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = android.app.Notification.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_focusguard)
            .setContentTitle(title)
            .setContentText(body)
            .setLargeIcon(largeIcon)
            .setStyle(android.app.Notification.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.notify(notifId, notification)
    }

    internal fun loadLargeIcon(context: Context, targetPackage: String): Bitmap? {
        val drawable = runCatching { context.packageManager.getApplicationIcon(targetPackage) }.getOrNull()
            ?: runCatching { context.packageManager.getApplicationIcon(context.packageName) }.getOrNull()
            ?: runCatching { context.getDrawable(R.mipmap.ic_launcher) }.getOrNull()
            ?: runCatching { context.getDrawable(R.drawable.ic_stat_focusguard) }.getOrNull()
            ?: return null
        val width = drawable.intrinsicWidth.takeIf { it > 0 }?.coerceAtMost(256) ?: 96
        val height = drawable.intrinsicHeight.takeIf { it > 0 }?.coerceAtMost(256) ?: 96
        return runCatching {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, width, height)
                drawable.draw(canvas)
            }
        }.getOrNull()
    }

    private fun appNotificationId(packageName: String): Int =
        10_000 + (packageName.hashCode() and 0x3fff_ffff)
}

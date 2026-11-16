package com.focusguard.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import kotlin.random.Random

object FocusNotifier {
    private const val CHANNEL_NUDGE_ID = "focusguard_comic_nudges"
    private const val CHANNEL_UPDATE_ID = "focusguard_updates"
    private const val NOTIF_ID_NUDGE = 202
    private const val NOTIF_ID_UPDATE = 203

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

    private fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

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
        manager.createNotificationChannel(nudgeChannel)
        manager.createNotificationChannel(updateChannel)
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

    private fun postNotification(
        context: Context,
        channelId: String,
        notifId: Int,
        title: String,
        body: String,
        customIntent: Intent?
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
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(android.app.Notification.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.notify(notifId, notification)
    }
}

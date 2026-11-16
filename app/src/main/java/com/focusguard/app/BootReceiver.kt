package com.focusguard.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            FocusPrefs.onDeviceReboot(context)
            if (FocusPrefs.isVpnEnabled(context) && VpnService.prepare(context) == null) {
                context.startForegroundService(Intent(context, DnsVpnService::class.java))
            }
        }
    }
}

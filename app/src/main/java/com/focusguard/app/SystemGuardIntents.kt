package com.focusguard.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

object SystemGuardIntents {

    fun isBatteryUnrestricted(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun openUnrestrictedBatterySettings(context: Context) {
        val pkgUri = Uri.parse("package:${context.packageName}")
        val directPrompt =
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkgUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
        try {
            if (directPrompt.resolveActivity(context.packageManager) != null) {
                context.startActivity(directPrompt)
                return
            }
        } catch (_: Exception) {}

        try {
            context.startActivity(
                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
            )
        } catch (_: Exception) {
            openAppInfo(context)
        }
    }

    fun supportsOemAutoStart(context: Context): Boolean {
        return getOemAutoStartIntents().any { intent ->
            context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) !=
                    null
        }
    }

    fun openAutoStartSettings(context: Context) {
        for (intent in getOemAutoStartIntents()) {
            try {
                if (context.packageManager.resolveActivity(
                                intent,
                                PackageManager.MATCH_DEFAULT_ONLY
                        ) != null
                ) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return
                }
            } catch (_: Exception) {}
        }
        openAppInfo(context)
    }

    fun openAppInfo(context: Context) {
        try {
            context.startActivity(
                    Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:${context.packageName}")
                            )
                            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            )
        } catch (_: Exception) {}
    }

    private fun getOemAutoStartIntents(): List<Intent> {
        val brand = "${Build.BRAND} ${Build.MANUFACTURER}".lowercase()
        val list = mutableListOf<Intent>()

        if ("xiaomi" in brand || "redmi" in brand || "poco" in brand) {
            list +=
                    Intent().setComponent(
                                    ComponentName(
                                            "com.miui.securitycenter",
                                            "com.miui.permcenter.autostart.AutoStartManagementActivity"
                                    )
                            )
        }
        if ("oppo" in brand || "realme" in brand) {
            list +=
                    Intent().setComponent(
                                    ComponentName(
                                            "com.coloros.safecenter",
                                            "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                                    )
                            )
            list +=
                    Intent().setComponent(
                                    ComponentName(
                                            "com.oplus.safecenter",
                                            "com.oplus.safecenter.permission.startup.StartupAppListActivity"
                                    )
                            )
        }
        if ("vivo" in brand || "iqoo" in brand) {
            list +=
                    Intent().setComponent(
                                    ComponentName(
                                            "com.vivo.permissionmanager",
                                            "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                                    )
                            )
            list +=
                    Intent().setComponent(
                                    ComponentName(
                                            "com.iqoo.secure",
                                            "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                                    )
                            )
        }
        if ("oneplus" in brand) {
            list +=
                    Intent().setComponent(
                                    ComponentName(
                                            "com.oneplus.security",
                                            "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                                    )
                            )
        }
        if ("tecno" in brand || "infinix" in brand || "itel" in brand) {
            list +=
                    Intent().setComponent(
                                    ComponentName(
                                            "com.transsion.phonemaster",
                                            "com.cyin.himgr.autostart.AutoStartActivity"
                                    )
                            )
        }
        return list
    }
}

package com.focusguard.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseUpdate(
    val latestTag: String,
    val releaseNotes: String,
    val downloadUrl: String
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val update: ReleaseUpdate) : UpdateCheckResult()
    data class UpToDate(val currentVersion: String) : UpdateCheckResult()
    data class Error(val reason: String) : UpdateCheckResult()
}

object UpdateChecker {
    private const val REPO_BASE = "https://github.com/quasimkhan/FocusGuard"
    private const val LATEST_RELEASE_URL = "$REPO_BASE/releases/latest"
    private const val PREF_NAME = "focus_guard_updates"
    private const val KEY_LAST_CHECK_MS = "last_update_check_ms"
    private const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L

    fun getInstalledVersion(context: Context): String {
        return try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    fun shouldAutoCheck(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val last = prefs.getLong(KEY_LAST_CHECK_MS, 0L)
        return System.currentTimeMillis() - last >= CHECK_INTERVAL_MS
    }

    suspend fun checkForUpdate(context: Context, force: Boolean = false): UpdateCheckResult {
        if (!force && !shouldAutoCheck(context)) {
            return UpdateCheckResult.UpToDate(getInstalledVersion(context))
        }

        return withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(LATEST_RELEASE_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    instanceFollowRedirects = true
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("User-Agent", "FocusGuard-Android")
                }

                val code = conn.responseCode
                if (code == 404) {
                    markChecked(context)
                    return@withContext UpdateCheckResult.Error("No public release found (check repo visibility)")
                }
                if (code != 200) {
                    return@withContext UpdateCheckResult.Error("GitHub returned HTTP $code")
                }

                val resolvedUrl = conn.url.toString()
                val tagName = resolvedUrl
                    .substringAfter("/releases/tag/", "")
                    .substringBefore("?")
                    .substringBefore("#")
                    .trim()

                markChecked(context)
                val installed = getInstalledVersion(context)

                if (tagName.isNotBlank() && isNewerVersion(tagName, installed)) {
                    val apkDownloadUrl = "$REPO_BASE/releases/download/$tagName/FocusGuard-$tagName-Quasim.apk"
                    UpdateCheckResult.UpdateAvailable(
                        ReleaseUpdate(
                            latestTag = tagName,
                            releaseNotes = "Tap Download to install FocusGuard $tagName directly from GitHub.",
                            downloadUrl = apkDownloadUrl
                        )
                    )
                } else {
                    UpdateCheckResult.UpToDate(installed)
                }
            } catch (_: Exception) {
                UpdateCheckResult.Error("Unable to reach GitHub (check internet connection)")
            } finally {
                conn?.disconnect()
            }
        }
    }

    private fun markChecked(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_CHECK_MS, System.currentTimeMillis())
            .apply()
    }

    private fun isNewerVersion(remoteTag: String, localVersion: String): Boolean {
        val remoteParts = remoteTag.trimStart('v', 'V')
            .substringBefore('-')
            .split(".")
            .map { it.toIntOrNull() ?: 0 }
        val localParts = localVersion.trimStart('v', 'V')
            .substringBefore('-')
            .split(".")
            .map { it.toIntOrNull() ?: 0 }

        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }
}

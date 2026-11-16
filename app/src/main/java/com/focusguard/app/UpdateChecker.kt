package com.focusguard.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
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
    private const val LATEST_RELEASE_API =
        "https://api.github.com/repos/quasimkhan/FocusGuard/releases/latest"
    private const val RELEASES_PAGE_URL =
        "https://github.com/quasimkhan/FocusGuard/releases/latest"
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
                val url = URL(LATEST_RELEASE_API)
                conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "FocusGuard-Android")
                }

                val code = conn.responseCode
                if (code == 404) {
                    markChecked(context)
                    return@withContext UpdateCheckResult.UpToDate(getInstalledVersion(context))
                }
                if (code != 200) {
                    return@withContext UpdateCheckResult.Error("GitHub returned HTTP $code")
                }

                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val tagName = json.optString("tag_name", "").trim()
                val notes = json.optString("body", "New improvements and fixes available.")
                    .trim()
                    .take(220)
                val htmlUrl = json.optString("html_url", RELEASES_PAGE_URL)

                var apkUrl = htmlUrl
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val name = asset.optString("name", "")
                        val dl = asset.optString("browser_download_url", "")
                        if (name.endsWith(".apk", ignoreCase = true) && dl.isNotBlank()) {
                            apkUrl = dl
                            break
                        }
                    }
                }

                markChecked(context)
                val installed = getInstalledVersion(context)
                if (tagName.isNotBlank() && isNewerVersion(tagName, installed)) {
                    UpdateCheckResult.UpdateAvailable(
                        ReleaseUpdate(
                            latestTag = tagName,
                            releaseNotes = notes.ifBlank { "New FocusGuard release available on GitHub." },
                            downloadUrl = apkUrl
                        )
                    )
                } else {
                    UpdateCheckResult.UpToDate(installed)
                }
            } catch (_: Exception) {
                UpdateCheckResult.Error("Unable to reach GitHub (offline or rate-limited)")
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

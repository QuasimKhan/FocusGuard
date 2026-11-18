package com.focusguard.app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppEntry(
    val label: String,
    val packageName: String
)

/** Loads app metadata off the UI thread and keeps it for the lifetime of the process. */
object LaunchableAppInventory {
    @Volatile
    private var inventory: List<AppEntry>? = null

    fun cachedApps(): List<AppEntry>? = inventory

    suspend fun load(context: Context): List<AppEntry> {
        inventory?.let { return it }
        return withContext(Dispatchers.IO) {
            inventory?.let { return@withContext it }
            synchronized(this@LaunchableAppInventory) {
                inventory?.let { return@synchronized it }

                val appContext = context.applicationContext
                val packageManager = appContext.packageManager
                val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                val apps = packageManager.queryIntentActivities(launchIntent, PackageManager.MATCH_ALL)
                    .mapNotNull { info ->
                        val packageName = info.activityInfo?.packageName ?: return@mapNotNull null
                        if (packageName == appContext.packageName) return@mapNotNull null
                        AppEntry(info.loadLabel(packageManager).toString(), packageName)
                    }
                    .distinctBy(AppEntry::packageName)
                    .sortedBy { it.label.lowercase() }
                inventory = apps
                apps
            }
        }
    }
}

/** Small bitmap cache so lazy app rows do icon work only when they enter the viewport. */
object AppIconBitmapCache {
    private val bitmaps = object : LruCache<String, Bitmap>(4 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = (value.byteCount / 1024).coerceAtLeast(1)
    }

    fun cached(packageName: String): Bitmap? = bitmaps.get(packageName)

    suspend fun load(context: Context, packageName: String): Bitmap? {
        cached(packageName)?.let { return it }
        return withContext(Dispatchers.IO) {
            cached(packageName) ?: runCatching {
                context.applicationContext.packageManager
                    .getApplicationIcon(packageName)
                    .toBitmap(width = 96, height = 96)
                    .also { bitmaps.put(packageName, it) }
            }.getOrNull()
        }
    }
}

package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.Log
import java.util.Locale

data class AppLaunchResult(
    val success: Boolean,
    val appName: String,
    val packageName: String? = null,
    val message: String
)

object AppLauncherHelper {

    private const val TAG = "AppLauncherHelper"

    // Common system packages mapping for quick resolution
    private val wellKnownPackages = mapOf(
        "whatsapp" to listOf("com.whatsapp", "com.whatsapp.w4b"),
        "youtube" to listOf("com.google.android.youtube"),
        "instagram" to listOf("com.instagram.android"),
        "chrome" to listOf("com.android.chrome"),
        "browser" to listOf("com.android.chrome", "org.mozilla.firefox"),
        "camera" to listOf("com.android.camera", "com.google.android.GoogleCamera"),
        "photos" to listOf("com.google.android.apps.photos"),
        "gallery" to listOf("com.android.gallery3d", "com.google.android.apps.photos"),
        "calculator" to listOf("com.google.android.calculator", "com.android.calculator2"),
        "clock" to listOf("com.google.android.deskclock", "com.android.deskclock"),
        "alarm" to listOf("com.google.android.deskclock", "com.android.deskclock"),
        "maps" to listOf("com.google.android.apps.maps"),
        "spotify" to listOf("com.spotify.music"),
        "gmail" to listOf("com.google.android.gm"),
        "settings" to listOf("com.android.settings"),
        "files" to listOf("com.google.android.apps.nbu.files", "com.android.documentsui"),
        "calendar" to listOf("com.google.android.calendar")
    )

    /**
     * Finds and launches an installed application by its display label or package name.
     */
    fun launchAppByName(context: Context, rawQuery: String): AppLaunchResult {
        val pm = context.packageManager
        val query = cleanQuery(rawQuery)

        if (query.isBlank()) {
            return AppLaunchResult(false, rawQuery, null, "App name cannot be empty")
        }

        // 1. Check dynamically installed launcher applications via PackageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveList: List<ResolveInfo> = pm.queryIntentActivities(launcherIntent, 0)

        // Exact label match (case-insensitive)
        var matchedResolveInfo = resolveList.find { resolve ->
            val label = resolve.loadLabel(pm).toString().trim()
            label.equals(query, ignoreCase = true)
        }

        // Substring / StartsWith match
        if (matchedResolveInfo == null) {
            matchedResolveInfo = resolveList.find { resolve ->
                val label = resolve.loadLabel(pm).toString().trim().lowercase(Locale.ROOT)
                label.contains(query) || query.contains(label)
            }
        }

        if (matchedResolveInfo != null) {
            val pkg = matchedResolveInfo.activityInfo.packageName
            val label = matchedResolveInfo.loadLabel(pm).toString()
            return launchPackage(context, pm, pkg, label)
        }

        // 2. Check well-known package alias dictionary
        for ((alias, candidates) in wellKnownPackages) {
            if (query.contains(alias) || alias.contains(query)) {
                for (cand in candidates) {
                    val launchIntent = pm.getLaunchIntentForPackage(cand)
                    if (launchIntent != null) {
                        return launchPackage(context, pm, cand, alias.replaceFirstChar { it.titlecase(Locale.ROOT) })
                    }
                }
            }
        }

        // 3. Fallback direct package launch attempt
        val directIntent = pm.getLaunchIntentForPackage(query)
        if (directIntent != null) {
            return launchPackage(context, pm, query, query)
        }

        Log.w(TAG, "Application not found on device for query: '$rawQuery'")
        return AppLaunchResult(
            success = false,
            appName = rawQuery,
            packageName = null,
            message = "Could not find app '$rawQuery' installed on device."
        )
    }

    private fun launchPackage(
        context: Context,
        pm: PackageManager,
        packageName: String,
        appLabel: String
    ): AppLaunchResult {
        val intent = pm.getLaunchIntentForPackage(packageName)
        return if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Log.d(TAG, "Successfully launched app: $appLabel ($packageName)")
            AppLaunchResult(
                success = true,
                appName = appLabel,
                packageName = packageName,
                message = "Launched $appLabel"
            )
        } else {
            AppLaunchResult(
                success = false,
                appName = appLabel,
                packageName = packageName,
                message = "Unable to launch $appLabel (intent unavailable)"
            )
        }
    }

    private fun cleanQuery(text: String): String {
        return text.lowercase(Locale.ROOT)
            .replace("kholo", "")
            .replace("open", "")
            .replace("chalao", "")
            .replace("start", "")
            .replace("app", "")
            .replace("application", "")
            .replace("wala", "")
            .replace("ka", "")
            .trim()
    }
}

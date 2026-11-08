package com.anacatalina.androidiconsadmin.platform

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

data class InstalledAppInfo(
    val packageName: String,
    val appLabel: String,
    val isSystemApp: Boolean,
    val hasAdaptiveIcon: Boolean
)

object AppDiscoveryService {

    fun getInstalledLaunchableApps(
        context: Context,
        includeSystemApps: Boolean = false
    ): List<InstalledAppInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }

        val results = mutableListOf<InstalledAppInfo>()
        val seenPackages = mutableSetOf<String>()

        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName || !seenPackages.add(pkg)) {
                continue
            }

            val appInfo = info.activityInfo.applicationInfo
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (!includeSystemApps && isSystem) {
                continue
            }

            val label = try {
                info.loadLabel(pm).toString()
            } catch (_: Exception) {
                pkg
            }

            val hasAdaptive = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val icon = pm.getApplicationIcon(pkg)
                    icon is android.graphics.drawable.AdaptiveIconDrawable
                } else false
            } catch (_: Exception) {
                false
            }

            results.add(
                InstalledAppInfo(
                    packageName = pkg,
                    appLabel = label,
                    isSystemApp = isSystem,
                    hasAdaptiveIcon = hasAdaptive
                )
            )
        }

        return results.sortedBy { it.appLabel.lowercase() }
    }
}

package com.anacatalina.androidiconsadmin.platform

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

object ShortcutDeliveryService {

    fun createShortcutId(packageName: String): String = "shortcut_$packageName"

    fun requestPinShortcut(
        context: Context,
        packageName: String,
        appLabel: String,
        iconBitmap: Bitmap
    ): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            return false
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(Intent.ACTION_MAIN).apply {
                setPackage(packageName)
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

        val shortcutInfo = ShortcutInfoCompat.Builder(context, createShortcutId(packageName))
            .setShortLabel(appLabel)
            .setIcon(IconCompat.createWithBitmap(iconBitmap))
            .setIntent(launchIntent)
            .build()

        return ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, null)
    }

    fun updateShortcutIconSilently(
        context: Context,
        packageName: String,
        appLabel: String,
        iconBitmap: Bitmap
    ): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        val shortcutInfo = ShortcutInfoCompat.Builder(context, createShortcutId(packageName))
            .setShortLabel(appLabel)
            .setIcon(IconCompat.createWithBitmap(iconBitmap))
            .setIntent(launchIntent)
            .build()

        return ShortcutManagerCompat.updateShortcuts(context, listOf(shortcutInfo))
    }

    fun updateBatchShortcutsSilently(
        context: Context,
        updates: List<Triple<String, String, Bitmap>> // packageName, appLabel, iconBitmap
    ): Boolean {
        val shortcutInfos = updates.mapNotNull { (pkg, label, bmp) ->
            val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return@mapNotNull null
            ShortcutInfoCompat.Builder(context, createShortcutId(pkg))
                .setShortLabel(label)
                .setIcon(IconCompat.createWithBitmap(bmp))
                .setIntent(intent)
                .build()
        }
        if (shortcutInfos.isEmpty()) return true
        return ShortcutManagerCompat.updateShortcuts(context, shortcutInfos)
    }

    fun disableShortcuts(
        context: Context,
        packageNames: List<String>,
        disabledMessage: String = "Icon reset in AndroidIconsAdmin"
    ) {
        val ids = packageNames.map { createShortcutId(it) }
        ShortcutManagerCompat.disableShortcuts(context, ids, disabledMessage)
    }
}

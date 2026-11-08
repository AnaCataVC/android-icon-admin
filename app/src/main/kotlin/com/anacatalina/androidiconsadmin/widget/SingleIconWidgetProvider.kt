package com.anacatalina.androidiconsadmin.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.anacatalina.androidiconsadmin.R
import com.anacatalina.androidiconsadmin.core.history.OriginalSnapshotStore
import com.anacatalina.androidiconsadmin.core.settings.AppSettingsService
import com.anacatalina.androidiconsadmin.core.styling.IconStyle
import com.anacatalina.androidiconsadmin.core.styling.IconStyleRenderer
import com.anacatalina.androidiconsadmin.core.styling.StyleStore
import com.anacatalina.androidiconsadmin.platform.AdaptiveLayerExtractor
import com.anacatalina.androidiconsadmin.platform.BitmapCanvasBridge
import java.io.File

class SingleIconWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    companion object {
        private const val PREFS_NAME = "SingleIconWidgetPrefs"
        private const val PREF_PREFIX_KEY = "appwidget_pkg_"

        fun saveWidgetPackage(context: Context, appWidgetId: Int, packageName: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_PREFIX_KEY + appWidgetId, packageName)
                .apply()
        }

        fun getWidgetPackage(context: Context, appWidgetId: Int): String? {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(PREF_PREFIX_KEY + appWidgetId, null)
        }

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            try {
                val packageName = getWidgetPackage(context, appWidgetId) ?: return

                val views = RemoteViews(context.packageName, R.layout.widget_single_icon)

                // Setup launch intent
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        appWidgetId,
                        launchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_icon_image, pendingIntent)
                }

                // Render current styled or original icon
                val settingsService = AppSettingsService(File(context.filesDir, "settings.json"))
                val settings = settingsService.loadSettings()
                val styleStore = StyleStore(File(context.filesDir, "styles"))
                val activeStyle = styleStore.get(settings.activeStyleId) ?: IconStyle.createDefault()

                val snapshotStore = OriginalSnapshotStore(File(context.filesDir, "snapshots"))
                val originalCanvas = snapshotStore.getSnapshot(packageName)
                    ?: AdaptiveLayerExtractor.extractIcon(context, packageName, activeStyle.extractionMode)

                if (originalCanvas != null) {
                    val styledCanvas = IconStyleRenderer.render(originalCanvas, activeStyle, canvasSize = 192)
                    val styledBitmap = BitmapCanvasBridge.toBitmap(styledCanvas)
                    views.setImageViewBitmap(R.id.widget_icon_image, styledBitmap)
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (_: Throwable) {
                // Safeguard against missing drawables or widget update errors
            }
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, SingleIconWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (id in allWidgetIds) {
                updateWidget(context, appWidgetManager, id)
            }
        }
    }
}

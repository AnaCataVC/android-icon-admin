package com.anacatalina.androidiconsadmin.platform

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import com.anacatalina.androidiconsadmin.core.styling.ExtractionMode
import com.anacatalina.androidiconsadmin.core.styling.IconNormalizer
import com.anacatalina.androidiconsadmin.core.styling.RgbaCanvas

object AdaptiveLayerExtractor {

    fun extractIcon(
        context: Context,
        packageName: String,
        mode: ExtractionMode = ExtractionMode.SMART_FOREGROUND,
        targetSize: Int = 256
    ): RgbaCanvas? {
        val pm = context.packageManager
        val rawDrawable = try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val res = pm.getResourcesForApplication(appInfo)
            if (appInfo.icon != 0) {
                res.getDrawableForDensity(appInfo.icon, context.resources.displayMetrics.densityDpi, null)
            } else {
                pm.getApplicationIcon(packageName)
            }
        } catch (_: Exception) {
            try {
                pm.getApplicationIcon(packageName)
            } catch (_: Exception) {
                null
            }
        } ?: return null

        return extractFromDrawable(rawDrawable, mode, targetSize)
    }

    fun extractFromDrawable(
        drawable: Drawable,
        mode: ExtractionMode,
        targetSize: Int = 256
    ): RgbaCanvas {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
            // Check monochrome first if requested
            if (mode == ExtractionMode.MONOCHROME_PREFERRED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val mono = drawable.monochrome
                if (mono != null) {
                    val monoBitmap = renderDrawableToBitmap(mono, targetSize, targetSize)
                    return BitmapCanvasBridge.toRgbaCanvas(monoBitmap)
                }
            }

            if (mode == ExtractionMode.SMART_FOREGROUND) {
                val fg = drawable.foreground
                if (fg != null) {
                    val fgBitmap = renderDrawableToBitmap(fg, targetSize, targetSize)
                    val fgCanvas = BitmapCanvasBridge.toRgbaCanvas(fgBitmap)

                    // Check if foreground has full-bleed opaque edges (e.g. background baked into foreground)
                    val isFullBleed = IconNormalizer.hasFullBleedOpaqueEdges(fgCanvas)
                    if (!isFullBleed) {
                        return fgCanvas
                    }
                }
            }

            // Fallback to full composite
            val fullBitmap = renderDrawableToBitmap(drawable, targetSize, targetSize)
            return BitmapCanvasBridge.toRgbaCanvas(fullBitmap)
        }

        // Standard legacy drawable
        val bitmap = renderDrawableToBitmap(drawable, targetSize, targetSize)
        return BitmapCanvasBridge.toRgbaCanvas(bitmap)
    }

    private fun renderDrawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return Bitmap.createScaledBitmap(drawable.bitmap, width, height, true)
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        return bitmap
    }
}

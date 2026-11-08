package com.anacatalina.androidiconsadmin.platform

import android.graphics.Bitmap
import com.anacatalina.androidiconsadmin.core.styling.RgbaCanvas
import java.nio.ByteBuffer

object BitmapCanvasBridge {
    fun toRgbaCanvas(bitmap: Bitmap): RgbaCanvas {
        val safeBitmap = if (bitmap.config != Bitmap.Config.ARGB_8888) {
            val copied = try { bitmap.copy(Bitmap.Config.ARGB_8888, false) } catch (_: Exception) { null }
            if (copied != null) {
                copied
            } else {
                val swBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(swBitmap)
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                swBitmap
            }
        } else {
            bitmap
        }
        val width = safeBitmap.width
        val height = safeBitmap.height
        val pixels = IntArray(width * height)
        safeBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        return RgbaCanvas.fromArgbArray(pixels, width, height)
    }

    fun toBitmap(canvas: RgbaCanvas): Bitmap {
        val bitmap = Bitmap.createBitmap(canvas.width, canvas.height, Bitmap.Config.ARGB_8888)
        val argb = IntArray(canvas.width * canvas.height)
        for (i in argb.indices) {
            val offset = i * 4
            val r = canvas.pixels[offset].toInt() and 0xFF
            val g = canvas.pixels[offset + 1].toInt() and 0xFF
            val b = canvas.pixels[offset + 2].toInt() and 0xFF
            val a = canvas.pixels[offset + 3].toInt() and 0xFF
            argb[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        bitmap.setPixels(argb, 0, canvas.width, 0, 0, canvas.width, canvas.height)
        return bitmap
    }
}

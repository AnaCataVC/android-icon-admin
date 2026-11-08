package com.anacatalina.androidiconsadmin.core.styling

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class RgbaCanvas(
    val width: Int,
    val height: Int,
    val pixels: ByteArray = ByteArray(width * height * 4)
) {
    init {
        require(width > 0) { "Width must be positive" }
        require(height > 0) { "Height must be positive" }
        require(pixels.size == width * height * 4) { "Pixel array size must match width * height * 4" }
    }

    data class Pixel(val r: Int, val g: Int, val b: Int, val a: Int)

    fun getPixel(x: Int, y: Int): Pixel {
        require(x in 0 until width && y in 0 until height) { "Coordinates out of bounds: ($x, $y)" }
        val offset = (y * width + x) * 4
        return Pixel(
            r = pixels[offset].toInt() and 0xFF,
            g = pixels[offset + 1].toInt() and 0xFF,
            b = pixels[offset + 2].toInt() and 0xFF,
            a = pixels[offset + 3].toInt() and 0xFF
        )
    }

    fun setPixel(x: Int, y: Int, r: Int, g: Int, b: Int, a: Int) {
        if (x !in 0 until width || y !in 0 until height) return
        val offset = (y * width + x) * 4
        pixels[offset] = r.coerceIn(0, 255).toByte()
        pixels[offset + 1] = g.coerceIn(0, 255).toByte()
        pixels[offset + 2] = b.coerceIn(0, 255).toByte()
        pixels[offset + 3] = a.coerceIn(0, 255).toByte()
    }

    fun clone(): RgbaCanvas {
        val copy = ByteArray(pixels.size)
        System.arraycopy(pixels, 0, copy, 0, pixels.size)
        return RgbaCanvas(width, height, copy)
    }

    fun toPngByteArray(): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val offset = (y * width + x) * 4
                val r = pixels[offset].toInt() and 0xFF
                val g = pixels[offset + 1].toInt() and 0xFF
                val b = pixels[offset + 2].toInt() and 0xFF
                val a = pixels[offset + 3].toInt() and 0xFF
                val argb = (a shl 24) or (r shl 16) or (g shl 8) or b
                image.setRGB(x, y, argb)
            }
        }
        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "PNG", baos)
        return baos.toByteArray()
    }

    companion object {
        fun fromPngByteArray(bytes: ByteArray): RgbaCanvas {
            val image = ImageIO.read(ByteArrayInputStream(bytes))
                ?: throw IllegalArgumentException("Could not decode image from PNG byte array")
            val w = image.width
            val h = image.height
            val canvas = RgbaCanvas(w, h)
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val argb = image.getRGB(x, y)
                    val a = (argb ushr 24) and 0xFF
                    val r = (argb ushr 16) and 0xFF
                    val g = (argb ushr 8) and 0xFF
                    val b = argb and 0xFF
                    canvas.setPixel(x, y, r, g, b, a)
                }
            }
            return canvas
        }

        fun fromArgbArray(argb: IntArray, width: Int, height: Int): RgbaCanvas {
            require(argb.size == width * height)
            val canvas = RgbaCanvas(width, height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val color = argb[y * width + x]
                    val a = (color ushr 24) and 0xFF
                    val r = (color ushr 16) and 0xFF
                    val g = (color ushr 8) and 0xFF
                    val b = color and 0xFF
                    canvas.setPixel(x, y, r, g, b, a)
                }
            }
            return canvas
        }
    }
}

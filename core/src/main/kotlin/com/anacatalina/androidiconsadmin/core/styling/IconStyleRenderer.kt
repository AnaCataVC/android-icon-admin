package com.anacatalina.androidiconsadmin.core.styling

import java.security.MessageDigest
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

object IconStyleRenderer {
    const val DEFAULT_CANVAS_SIZE = 256
    const val RENDERER_VERSION = 1

    fun applyTreatment(source: RgbaCanvas, treatment: TreatmentSpec): RgbaCanvas {
        val result = source.clone()

        val sat = treatment.saturation
        val bri = treatment.brightness
        val con = treatment.contrast
        val factor = (1.0 + con) / (1.0 - con * 0.99)

        val tint = parseColorRgb(treatment.tintColor)

        for (i in result.pixels.indices step 4) {
            val alpha = result.pixels[i + 3].toInt() and 0xFF
            if (alpha == 0) continue

            var r = (result.pixels[i].toInt() and 0xFF).toDouble()
            var g = (result.pixels[i + 1].toInt() and 0xFF).toDouble()
            var b = (result.pixels[i + 2].toInt() and 0xFF).toDouble()

            // 1. Saturation (Rec.709 luminance)
            if (sat != 0.0) {
                val y = 0.2126 * r + 0.7152 * g + 0.0722 * b
                r = y + (r - y) * (1.0 + sat)
                g = y + (g - y) * (1.0 + sat)
                b = y + (b - y) * (1.0 + sat)
            }

            // 2. Brightness
            if (bri != 0.0) {
                val offset = bri * 255.0
                r += offset
                g += offset
                b += offset
            }

            // 3. Contrast
            if (con != 0.0) {
                r = ((r / 255.0 - 0.5) * factor + 0.5) * 255.0
                g = ((g / 255.0 - 0.5) * factor + 0.5) * 255.0
                b = ((b / 255.0 - 0.5) * factor + 0.5) * 255.0
            }

            // 4. Optional tint (gentle 15% blend)
            if (tint != null) {
                r = r * 0.85 + tint.r * 0.15
                g = g * 0.85 + tint.g * 0.15
                b = b * 0.85 + tint.b * 0.15
            }

            result.pixels[i] = r.roundToInt().coerceIn(0, 255).toByte()
            result.pixels[i + 1] = g.roundToInt().coerceIn(0, 255).toByte()
            result.pixels[i + 2] = b.roundToInt().coerceIn(0, 255).toByte()
        }

        return result
    }

    fun renderContainer(size: Int, container: ContainerSpec): RgbaCanvas {
        require(size > 0) { "Size must be positive" }

        val bg = parseColorRgba(container.backgroundColor)
        val canvas = RgbaCanvas(size, size)

        val cx = (size - 1) / 2.0
        val cy = (size - 1) / 2.0
        val r = size / 2.0
        val cornerRadius = size * 0.225

        // Subpixel sampling 4x4 grid (16 samples per pixel)
        val samples = 4
        val step = 1.0 / samples
        val sampleWeight = 1.0 / (samples * samples)

        for (y in 0 until size) {
            for (x in 0 until size) {
                var insideCount = 0

                for (sy in 0 until samples) {
                    val py = y + (sy + 0.5) * step

                    for (sx in 0 until samples) {
                        val px = x + (sx + 0.5) * step

                        val inside = when (container.shape) {
                            ContainerShape.SQUIRCLE -> isInsideSquircle(px, py, cx, cy, r, exponent = 4.5)
                            ContainerShape.ONE_UI_SQUIRCLE -> isInsideSquircle(px, py, cx, cy, r, exponent = 3.6)
                            ContainerShape.ROUNDED_SQUARE -> isInsideRoundedSquare(px, py, size, cornerRadius)
                            ContainerShape.CIRCLE -> isInsideCircle(px, py, cx, cy, r)
                            ContainerShape.TEARDROP -> isInsideTeardrop(px, py, cx, cy, r, size)
                        }

                        if (inside) insideCount++
                    }
                }

                if (insideCount > 0) {
                    val coverage = insideCount * sampleWeight
                    val finalAlpha = (bg.a * coverage).roundToInt().coerceIn(0, 255)
                    canvas.setPixel(x, y, bg.r, bg.g, bg.b, finalAlpha)
                }
            }
        }

        return canvas
    }

    fun composite(background: RgbaCanvas, foreground: RgbaCanvas): RgbaCanvas {
        require(background.width == foreground.width && background.height == foreground.height) {
            "Canvas dimensions must match for composite"
        }

        val result = RgbaCanvas(background.width, background.height)

        for (i in result.pixels.indices step 4) {
            val fr = foreground.pixels[i].toInt() and 0xFF
            val fg = foreground.pixels[i + 1].toInt() and 0xFF
            val fb = foreground.pixels[i + 2].toInt() and 0xFF
            val fa = (foreground.pixels[i + 3].toInt() and 0xFF) / 255.0

            val br = background.pixels[i].toInt() and 0xFF
            val bg = background.pixels[i + 1].toInt() and 0xFF
            val bb = background.pixels[i + 2].toInt() and 0xFF
            val ba = (background.pixels[i + 3].toInt() and 0xFF) / 255.0

            val outAlpha = fa + ba * (1.0 - fa)

            if (outAlpha > 0.0) {
                val outR = (fr * fa + br * ba * (1.0 - fa)) / outAlpha
                val outG = (fg * fa + bg * ba * (1.0 - fa)) / outAlpha
                val outB = (fb * fa + bb * ba * (1.0 - fa)) / outAlpha

                result.pixels[i] = outR.roundToInt().coerceIn(0, 255).toByte()
                result.pixels[i + 1] = outG.roundToInt().coerceIn(0, 255).toByte()
                result.pixels[i + 2] = outB.roundToInt().coerceIn(0, 255).toByte()
                result.pixels[i + 3] = (outAlpha * 255.0).roundToInt().coerceIn(0, 255).toByte()
            }
        }

        return result
    }

    fun render(
        source: RgbaCanvas,
        style: IconStyle,
        canvasSize: Int = DEFAULT_CANVAS_SIZE
    ): RgbaCanvas {
        // 1. Normalize
        val normalized = IconNormalizer.normalize(source, canvasSize, style.container.padding)

        // 2. Treatment
        val treated = applyTreatment(normalized, style.treatment)

        // 3. Container
        val container = renderContainer(canvasSize, style.container)

        // 4. Composite
        return composite(container, treated)
    }

    fun computeCacheKey(sourceBytes: ByteArray, style: IconStyle): String {
        val md = MessageDigest.getInstance("SHA-256")
        val sourceHash = md.digest(sourceBytes).joinToString("") { "%02x".format(it) }
        val styleJson = style.toJson()
        val combined = "$sourceHash|$styleJson|v$RENDERER_VERSION"
        val finalHash = md.digest(combined.toByteArray(Charsets.UTF_8))
        return finalHash.joinToString("") { "%02x".format(it) }
    }

    private fun isInsideSquircle(px: Double, py: Double, cx: Double, cy: Double, r: Double, exponent: Double): Boolean {
        val dx = abs((px - cx) / r)
        val dy = abs((py - cy) / r)
        return dx.pow(exponent) + dy.pow(exponent) <= 1.0
    }

    private fun isInsideCircle(px: Double, py: Double, cx: Double, cy: Double, r: Double): Boolean {
        val dx = px - cx
        val dy = py - cy
        return (dx * dx + dy * dy) <= (r * r)
    }

    private fun isInsideRoundedSquare(px: Double, py: Double, size: Int, cornerRadius: Double): Boolean {
        if (px < 0 || px >= size || py < 0 || py >= size) return false
        val min = cornerRadius
        val max = size - cornerRadius

        if (px in min..max) return true
        if (py in min..max) return true

        val cornerX = if (px < min) min else max
        val cornerY = if (py < min) min else max

        val dx = px - cornerX
        val dy = py - cornerY
        return (dx * dx + dy * dy) <= (cornerRadius * cornerRadius)
    }

    private fun isInsideTeardrop(px: Double, py: Double, cx: Double, cy: Double, r: Double, size: Int): Boolean {
        // Circle base with top-right rounded corner
        if (px >= cx && py <= cy) {
            // Top-right corner squared off slightly or corner-rounded
            return px < size && py >= 0
        }
        val dx = px - cx
        val dy = py - cy
        return (dx * dx + dy * dy) <= (r * r)
    }

    data class RgbColor(val r: Int, val g: Int, val b: Int)
    data class RgbaColor(val r: Int, val g: Int, val b: Int, val a: Int)

    fun parseColorRgba(hex: String): RgbaColor {
        val clean = hex.trim().removePrefix("#")
        return try {
            when (clean.length) {
                6 -> {
                    val r = clean.substring(0, 2).toInt(16)
                    val g = clean.substring(2, 4).toInt(16)
                    val b = clean.substring(4, 6).toInt(16)
                    RgbaColor(r, g, b, 255)
                }
                8 -> {
                    val a = clean.substring(0, 2).toInt(16)
                    val r = clean.substring(2, 4).toInt(16)
                    val g = clean.substring(4, 6).toInt(16)
                    val b = clean.substring(6, 8).toInt(16)
                    RgbaColor(r, g, b, a)
                }
                else -> RgbaColor(230, 219, 250, 255)
            }
        } catch (_: Exception) {
            RgbaColor(230, 219, 250, 255)
        }
    }

    fun parseColorRgb(hex: String?): RgbColor? {
        if (hex.isNullOrBlank()) return null
        val clean = hex.trim().removePrefix("#")
        return try {
            if (clean.length == 6) {
                val r = clean.substring(0, 2).toInt(16)
                val g = clean.substring(2, 4).toInt(16)
                val b = clean.substring(4, 6).toInt(16)
                RgbColor(r, g, b)
            } else null
        } catch (_: Exception) {
            null
        }
    }
}

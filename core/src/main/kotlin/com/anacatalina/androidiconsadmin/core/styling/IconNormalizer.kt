package com.anacatalina.androidiconsadmin.core.styling

import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

data class EffectiveBounds(
    val minX: Int,
    val minY: Int,
    val maxX: Int,
    val maxY: Int
)

object IconNormalizer {
    const val ALPHA_THRESHOLD = 16

    fun getEffectiveBounds(source: RgbaCanvas): EffectiveBounds {
        var minX = source.width
        var minY = source.height
        var maxX = -1
        var maxY = -1

        for (y in 0 until source.height) {
            val rowOffset = y * source.width * 4
            for (x in 0 until source.width) {
                val alpha = source.pixels[rowOffset + x * 4 + 3].toInt() and 0xFF
                if (alpha >= ALPHA_THRESHOLD) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (maxX < 0 || maxY < 0) {
            return EffectiveBounds(0, 0, source.width - 1, source.height - 1)
        }

        return EffectiveBounds(minX, minY, maxX, maxY)
    }

    fun hasFullBleedOpaqueEdges(source: RgbaCanvas, insetPercent: Double = 0.05): Boolean {
        val insetX = (source.width * insetPercent).toInt().coerceAtLeast(1)
        val insetY = (source.height * insetPercent).toInt().coerceAtLeast(1)

        var opaqueEdgeCount = 0
        var totalEdgeSamples = 0

        // Top and bottom rows
        for (x in insetX until (source.width - insetX) step 2) {
            val topAlpha = source.pixels[insetY * source.width * 4 + x * 4 + 3].toInt() and 0xFF
            val bottomAlpha = source.pixels[(source.height - 1 - insetY) * source.width * 4 + x * 4 + 3].toInt() and 0xFF
            if (topAlpha >= 240) opaqueEdgeCount++
            if (bottomAlpha >= 240) opaqueEdgeCount++
            totalEdgeSamples += 2
        }

        // Left and right columns
        for (y in insetY until (source.height - insetY) step 2) {
            val leftAlpha = source.pixels[y * source.width * 4 + insetX * 4 + 3].toInt() and 0xFF
            val rightAlpha = source.pixels[y * source.width * 4 + (source.width - 1 - insetX) * 4 + 3].toInt() and 0xFF
            if (leftAlpha >= 240) opaqueEdgeCount++
            if (rightAlpha >= 240) opaqueEdgeCount++
            totalEdgeSamples += 2
        }

        if (totalEdgeSamples == 0) return false
        return (opaqueEdgeCount.toDouble() / totalEdgeSamples) > 0.85
    }

    fun normalize(
        source: RgbaCanvas,
        targetSize: Int = 256,
        padding: Double = 0.16
    ): RgbaCanvas {
        require(targetSize > 0) { "Target size must be positive" }
        require(padding in 0.0..0.40) { "Padding must be between 0.0 and 0.40" }

        val bounds = getEffectiveBounds(source)
        val cropWidth = bounds.maxX - bounds.minX + 1
        val cropHeight = bounds.maxY - bounds.minY + 1

        val innerSize = targetSize * (1.0 - 2.0 * padding)
        val scale = min(innerSize / cropWidth, innerSize / cropHeight)

        val dstWidth = (cropWidth * scale).roundToInt().coerceIn(1, targetSize)
        val dstHeight = (cropHeight * scale).roundToInt().coerceIn(1, targetSize)

        val dstX = (targetSize - dstWidth) / 2
        val dstY = (targetSize - dstHeight) / 2

        val target = RgbaCanvas(targetSize, targetSize)

        // Bilinear resampling from crop rect into target canvas
        for (dy in 0 until dstHeight) {
            val srcY = if (dstHeight == 1) bounds.minY.toDouble() else {
                bounds.minY + (dy.toDouble() / (dstHeight - 1)) * (cropHeight - 1)
            }
            val y0 = floor(srcY).toInt()
            val y1 = min(y0 + 1, bounds.maxY)
            val fy = srcY - y0

            for (dx in 0 until dstWidth) {
                val srcX = if (dstWidth == 1) bounds.minX.toDouble() else {
                    bounds.minX + (dx.toDouble() / (dstWidth - 1)) * (cropWidth - 1)
                }
                val x0 = floor(srcX).toInt()
                val x1 = min(x0 + 1, bounds.maxX)
                val fx = srcX - x0

                val p00 = source.getPixel(x0, y0)
                val p10 = source.getPixel(x1, y0)
                val p01 = source.getPixel(x0, y1)
                val p11 = source.getPixel(x1, y1)

                val r = interpolate(p00.r, p10.r, p01.r, p11.r, fx, fy)
                val g = interpolate(p00.g, p10.g, p01.g, p11.g, fx, fy)
                val b = interpolate(p00.b, p10.b, p01.b, p11.b, fx, fy)
                val a = interpolate(p00.a, p10.a, p01.a, p11.a, fx, fy)

                target.setPixel(dstX + dx, dstY + dy, r, g, b, a)
            }
        }

        return target
    }

    private fun interpolate(v00: Int, v10: Int, v01: Int, v11: Int, fx: Double, fy: Double): Int {
        val top = v00 * (1.0 - fx) + v10 * fx
        val bottom = v01 * (1.0 - fx) + v11 * fx
        val result = top * (1.0 - fy) + bottom * fy
        return result.roundToInt().coerceIn(0, 255)
    }
}

package com.anacatalina.androidiconsadmin.core.styling

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class StylingTests {

    @Test
    fun `RgbaCanvas creation and pixel manipulation`() {
        val canvas = RgbaCanvas(4, 4)
        assertEquals(4, canvas.width)
        assertEquals(4, canvas.height)

        canvas.setPixel(1, 1, 255, 100, 50, 200)
        val p = canvas.getPixel(1, 1)
        assertEquals(255, p.r)
        assertEquals(100, p.g)
        assertEquals(50, p.b)
        assertEquals(200, p.a)
    }

    @Test
    fun `IconNormalizer correctly detects bounds and normalizes`() {
        val source = RgbaCanvas(10, 10)
        // Draw an isolated 4x4 square in the center
        for (y in 3..6) {
            for (x in 3..6) {
                source.setPixel(x, y, 200, 50, 50, 255)
            }
        }

        val bounds = IconNormalizer.getEffectiveBounds(source)
        assertEquals(3, bounds.minX)
        assertEquals(3, bounds.minY)
        assertEquals(6, bounds.maxX)
        assertEquals(6, bounds.maxY)

        val normalized = IconNormalizer.normalize(source, targetSize = 20, padding = 0.10)
        assertEquals(20, normalized.width)
        assertEquals(20, normalized.height)

        // Center pixel of normalized should be opaque
        val center = normalized.getPixel(10, 10)
        assertTrue(center.a > 200)
    }

    @Test
    fun `IconStyleRenderer deterministic rendering and cache key`() {
        val source = RgbaCanvas(32, 32)
        for (y in 8..24) {
            for (x in 8..24) {
                source.setPixel(x, y, 100, 150, 200, 255)
            }
        }

        val style = IconStyle.createDefault()
        val rendered1 = IconStyleRenderer.render(source, style, canvasSize = 64)
        val rendered2 = IconStyleRenderer.render(source, style, canvasSize = 64)

        assertEquals(64, rendered1.width)
        assertEquals(64, rendered1.height)
        assertArrayEquals(rendered1.pixels, rendered2.pixels, "Rendering must be 100% deterministic")

        val key1 = IconStyleRenderer.computeCacheKey(source.pixels, style)
        val key2 = IconStyleRenderer.computeCacheKey(source.pixels, style)
        assertEquals(key1, key2)
    }

    @Test
    fun `StyleStore atomic save and retrieval`(@TempDir tempDir: File) {
        val store = StyleStore(tempDir)
        val defaultStyles = store.getAll()
        assertTrue(defaultStyles.isNotEmpty())

        val custom = IconStyle(
            id = "custom-test-style",
            name = "Custom Test",
            container = ContainerSpec(ContainerShape.ONE_UI_SQUIRCLE, "#123456", 0.15),
            treatment = TreatmentSpec(0.0, 0.0, 0.0, null)
        )
        store.save(custom)

        val retrieved = store.get("custom-test-style")
        assertNotNull(retrieved)
        assertEquals("Custom Test", retrieved?.name)
        assertEquals(ContainerShape.ONE_UI_SQUIRCLE, retrieved?.container?.shape)
    }

    @Test
    fun `StyleStore preserves standard presets after saving custom or modified styles`(@TempDir tempDir: File) {
        val store = StyleStore(tempDir)
        val initialPresets = store.getAll()
        val standardCount = IconStyle.getStandardPresets().size
        assertEquals(standardCount, initialPresets.size)

        // 1. Save custom style
        val custom = IconStyle(
            id = "custom-new-preset",
            name = "My Custom Preset",
            container = ContainerSpec(ContainerShape.CIRCLE, "#FFAA00", 0.16),
            treatment = TreatmentSpec(0.0, 0.0, 0.0, null)
        )
        store.save(custom)

        val afterCustomSave = store.getAll()
        assertEquals(standardCount + 1, afterCustomSave.size, "Custom style must be added without erasing standard presets")
        assertTrue(afterCustomSave.any { it.id == "style-soft-lavender" })
        assertTrue(afterCustomSave.any { it.id == "custom-new-preset" })

        // 2. Modify and save an existing standard preset
        val modifiedLavender = IconStyle.createDefault("Customized Soft Lavender").copy(
            container = ContainerSpec(ContainerShape.TEARDROP, "#112233", 0.20)
        )
        store.save(modifiedLavender)

        val afterModifySave = store.getAll()
        assertEquals(standardCount + 1, afterModifySave.size, "Modifying an existing preset must override in-place")
        val updatedLavender = store.get("style-soft-lavender")
        assertNotNull(updatedLavender)
        assertEquals("Customized Soft Lavender", updatedLavender?.name)
        assertEquals(ContainerShape.TEARDROP, updatedLavender?.container?.shape)
        assertEquals("#112233", updatedLavender?.container?.backgroundColor)
    }

    @Test
    fun `IconStyleRenderer composite strictly masks foreground outside container shape`() {
        // Create fully opaque 64x64 white foreground
        val foreground = RgbaCanvas(64, 64)
        for (y in 0 until 64) {
            for (x in 0 until 64) {
                foreground.setPixel(x, y, 255, 255, 255, 255)
            }
        }

        // Render circular container (corners are outside circle)
        val circleContainer = ContainerSpec(
            shape = ContainerShape.CIRCLE,
            backgroundColor = "#000000",
            padding = 0.0
        )
        val containerCanvas = IconStyleRenderer.renderContainer(64, circleContainer)

        val composited = IconStyleRenderer.composite(containerCanvas, foreground)

        // Corner (0,0) is outside circle: must have alpha == 0
        val cornerPixel = composited.getPixel(0, 0)
        assertEquals(0, cornerPixel.a, "Pixels outside the container shape must be completely transparent")

        // Center (32, 32) is inside circle: must be opaque
        val centerPixel = composited.getPixel(32, 32)
        assertEquals(255, centerPixel.a, "Pixels inside the container shape must be fully opaque")
    }
}

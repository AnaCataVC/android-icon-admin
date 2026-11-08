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
}

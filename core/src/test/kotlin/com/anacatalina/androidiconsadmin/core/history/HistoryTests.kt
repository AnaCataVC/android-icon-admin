package com.anacatalina.androidiconsadmin.core.history

import com.anacatalina.androidiconsadmin.core.styling.RgbaCanvas
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class HistoryTests {

    @Test
    fun `OriginalSnapshotStore persists original icon safely`(@TempDir tempDir: File) {
        val store = OriginalSnapshotStore(tempDir)
        assertFalse(store.hasSnapshot("com.example.app"))

        val canvas = RgbaCanvas(16, 16)
        canvas.setPixel(0, 0, 128, 64, 32, 255)
        store.saveSnapshot("com.example.app", canvas)

        assertTrue(store.hasSnapshot("com.example.app"))
        val restored = store.getSnapshot("com.example.app")
        assertNotNull(restored)
        val p = restored?.getPixel(0, 0)
        assertEquals(128, p?.r)
        assertEquals(64, p?.g)
        assertEquals(32, p?.b)
    }

    @Test
    fun `OriginalSnapshotStore supports extraction mode variants and base fallback`(@TempDir tempDir: File) {
        val store = OriginalSnapshotStore(tempDir)
        val baseCanvas = RgbaCanvas(8, 8)
        baseCanvas.setPixel(0, 0, 10, 20, 30, 255)
        store.saveSnapshot("com.test.app", baseCanvas)

        val monoCanvas = RgbaCanvas(8, 8)
        monoCanvas.setPixel(0, 0, 200, 200, 200, 255)
        store.saveSnapshot("com.test.app", monoCanvas, "MONOCHROME_PREFERRED")

        assertTrue(store.hasSnapshot("com.test.app", "MONOCHROME_PREFERRED"))
        val monoRestored = store.getSnapshot("com.test.app", "MONOCHROME_PREFERRED")
        assertNotNull(monoRestored)
        assertEquals(200, monoRestored?.getPixel(0, 0)?.r)

        // Requesting an uncached mode falls back to base snapshot
        val smartRestored = store.getSnapshot("com.test.app", "SMART_FOREGROUND")
        assertNotNull(smartRestored)
        assertEquals(10, smartRestored?.getPixel(0, 0)?.r)
    }

    @Test
    fun `AppIconUndoStore records and pops batches atomically`(@TempDir tempDir: File) {
        val file = File(tempDir, "undo.json")
        val store = AppIconUndoStore(file)
        assertTrue(store.getHistory().isEmpty())

        val batch1 = AppBatchRecord(
            batchId = "b1",
            timestampMillis = 1000L,
            description = "Applied Lavender to 2 apps",
            changes = listOf(
                AppIconChangeEntry("com.app.a", null, "style-soft-lavender", true),
                AppIconChangeEntry("com.app.b", null, "style-soft-lavender", true)
            )
        )
        store.recordBatch(batch1)

        assertEquals(1, store.getHistory().size)
        val popped = store.popLatestBatch()
        assertEquals("b1", popped?.batchId)
        assertTrue(store.getHistory().isEmpty())
    }
}

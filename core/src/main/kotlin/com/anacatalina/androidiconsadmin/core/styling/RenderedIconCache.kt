package com.anacatalina.androidiconsadmin.core.styling

import java.io.File

class RenderedIconCache(
    private val cacheDirectory: File
) {
    init {
        cacheDirectory.mkdirs()
    }

    fun get(cacheKey: String): RgbaCanvas? {
        val file = File(cacheDirectory, "$cacheKey.png")
        if (!file.exists()) return null
        return try {
            RgbaCanvas.fromPngByteArray(file.readBytes())
        } catch (_: Exception) {
            null
        }
    }

    fun put(cacheKey: String, canvas: RgbaCanvas): File {
        val file = File(cacheDirectory, "$cacheKey.png")
        val temp = File(cacheDirectory, "$cacheKey.tmp")
        val bytes = canvas.toPngByteArray()
        temp.writeBytes(bytes)
        if (file.exists()) file.delete()
        temp.renameTo(file)
        return file
    }

    fun clear() {
        cacheDirectory.listFiles()?.forEach { it.delete() }
    }
}

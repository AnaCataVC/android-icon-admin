package com.anacatalina.androidiconsadmin.core.history

import com.anacatalina.androidiconsadmin.core.styling.RgbaCanvas
import java.io.File

class OriginalSnapshotStore(
    private val snapshotsDirectory: File
) {
    init {
        snapshotsDirectory.mkdirs()
    }

    fun hasSnapshot(packageName: String, mode: String? = null): Boolean {
        val key = if (mode.isNullOrBlank()) packageName else "${packageName}_$mode"
        return getSnapshotFile(key).exists() || (mode != null && getSnapshotFile(packageName).exists())
    }

    fun saveSnapshot(packageName: String, canvas: RgbaCanvas, mode: String? = null) {
        val key = if (mode.isNullOrBlank()) packageName else "${packageName}_$mode"
        val file = getSnapshotFile(key)
        val safeName = key.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val temp = File(snapshotsDirectory, "$safeName.tmp")
        try {
            temp.writeBytes(canvas.toPngByteArray())
            if (!temp.renameTo(file)) {
                temp.copyTo(file, overwrite = true)
                temp.delete()
            }
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    fun getSnapshot(packageName: String, mode: String? = null): RgbaCanvas? {
        val key = if (mode.isNullOrBlank()) packageName else "${packageName}_$mode"
        val file = getSnapshotFile(key)
        if (file.exists()) {
            val canvas = try {
                RgbaCanvas.fromPngByteArray(file.readBytes())
            } catch (_: Exception) {
                null
            }
            if (canvas != null) return canvas
        }

        // Fallback to base snapshot if mode-specific snapshot does not exist yet
        if (!mode.isNullOrBlank()) {
            val baseFile = getSnapshotFile(packageName)
            if (baseFile.exists()) {
                return try {
                    RgbaCanvas.fromPngByteArray(baseFile.readBytes())
                } catch (_: Exception) {
                    null
                }
            }
        }
        return null
    }

    fun deleteSnapshot(packageName: String): Boolean {
        val file = getSnapshotFile(packageName)
        return if (file.exists()) file.delete() else false
    }

    private fun getSnapshotFile(packageName: String): File {
        val safeName = packageName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(snapshotsDirectory, "$safeName.png")
    }
}

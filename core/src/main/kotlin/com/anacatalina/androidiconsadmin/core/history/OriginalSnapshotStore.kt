package com.anacatalina.androidiconsadmin.core.history

import com.anacatalina.androidiconsadmin.core.styling.RgbaCanvas
import java.io.File

class OriginalSnapshotStore(
    private val snapshotsDirectory: File
) {
    init {
        snapshotsDirectory.mkdirs()
    }

    fun hasSnapshot(packageName: String): Boolean {
        return getSnapshotFile(packageName).exists()
    }

    fun saveSnapshot(packageName: String, canvas: RgbaCanvas) {
        val file = getSnapshotFile(packageName)
        if (!file.exists()) {
            val temp = File(snapshotsDirectory, "$packageName.tmp")
            temp.writeBytes(canvas.toPngByteArray())
            temp.renameTo(file)
        }
    }

    fun getSnapshot(packageName: String): RgbaCanvas? {
        val file = getSnapshotFile(packageName)
        if (!file.exists()) return null
        return try {
            RgbaCanvas.fromPngByteArray(file.readBytes())
        } catch (_: Exception) {
            null
        }
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

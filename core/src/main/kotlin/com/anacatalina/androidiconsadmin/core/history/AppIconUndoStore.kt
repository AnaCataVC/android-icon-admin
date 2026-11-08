package com.anacatalina.androidiconsadmin.core.history

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class AppIconUndoStore(
    private val storageFile: File,
    private val maxBatches: Int = 50
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun getHistory(): List<AppBatchRecord> {
        if (!storageFile.exists()) return emptyList()
        return try {
            json.decodeFromString<List<AppBatchRecord>>(storageFile.readText())
        } catch (_: Exception) {
            val bak = File(storageFile.parentFile, "${storageFile.name}.bak")
            if (bak.exists()) {
                try {
                    json.decodeFromString<List<AppBatchRecord>>(bak.readText())
                } catch (_: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }

    fun recordBatch(record: AppBatchRecord) {
        val current = getHistory().toMutableList()
        current.add(0, record)
        if (current.size > maxBatches) {
            current.removeAt(current.size - 1)
        }
        saveInternal(current)
    }

    fun popLatestBatch(): AppBatchRecord? {
        val current = getHistory().toMutableList()
        if (current.isEmpty()) return null
        val latest = current.removeAt(0)
        saveInternal(current)
        return latest
    }

    fun clear() {
        saveInternal(emptyList())
    }

    private fun saveInternal(records: List<AppBatchRecord>) {
        val parent = storageFile.parentFile
        if (parent != null && !parent.exists()) parent.mkdirs()

        val temp = File(parent, "${storageFile.name}.${UUID.randomUUID()}.tmp")
        try {
            temp.writeText(json.encodeToString(records))
            if (storageFile.exists()) {
                val bak = File(parent, "${storageFile.name}.bak")
                if (bak.exists()) bak.delete()
                storageFile.renameTo(bak)
            }
            if (!temp.renameTo(storageFile)) {
                temp.copyTo(storageFile, overwrite = true)
                temp.delete()
            }
        } finally {
            if (temp.exists()) temp.delete()
        }
    }
}

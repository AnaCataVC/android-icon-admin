package com.anacatalina.androidiconsadmin.core.rules

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

class RuleStore(
    private val storageFile: File
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun loadRules(): List<AppRule> {
        if (!storageFile.exists()) return emptyList()
        return try {
            json.decodeFromString<List<AppRule>>(storageFile.readText())
        } catch (_: Exception) {
            val bak = File(storageFile.parentFile, "${storageFile.name}.bak")
            if (bak.exists()) {
                try {
                    json.decodeFromString<List<AppRule>>(bak.readText())
                } catch (_: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }

    fun saveRules(rules: List<AppRule>) {
        val parent = storageFile.parentFile
        if (parent != null && !parent.exists()) parent.mkdirs()

        val temp = File(parent, "${storageFile.name}.${UUID.randomUUID()}.tmp")
        try {
            temp.writeText(json.encodeToString(rules))
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

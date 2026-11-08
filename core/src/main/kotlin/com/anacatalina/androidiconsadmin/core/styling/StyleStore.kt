package com.anacatalina.androidiconsadmin.core.styling

import java.io.File
import java.util.UUID

class StyleStore(
    private val stylesDirectory: File
) {
    init {
        stylesDirectory.mkdirs()
    }

    fun save(style: IconStyle) {
        require(style.id.isNotBlank()) { "Style id cannot be empty" }
        val file = File(stylesDirectory, "${style.id}.json")
        val temp = File(stylesDirectory, "${style.id}.${UUID.randomUUID()}.tmp")

        try {
            temp.writeText(style.toJson())
            if (file.exists()) {
                val bak = File(stylesDirectory, "${style.id}.bak")
                if (bak.exists()) bak.delete()
                file.renameTo(bak)
            }
            if (!temp.renameTo(file)) {
                temp.copyTo(file, overwrite = true)
                temp.delete()
            }
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    fun get(id: String): IconStyle? {
        val file = File(stylesDirectory, "$id.json")
        if (!file.exists()) return null
        return try {
            IconStyle.fromJson(file.readText())
        } catch (_: Exception) {
            null
        }
    }

    fun getAll(): List<IconStyle> {
        val files = stylesDirectory.listFiles { f -> f.extension == "json" }
        val standardPresets = IconStyle.getStandardPresets()
        val stylesMap = LinkedHashMap<String, IconStyle>()
        for (preset in standardPresets) {
            stylesMap[preset.id] = preset
        }

        if (!files.isNullOrEmpty()) {
            for (f in files) {
                try {
                    val saved = IconStyle.fromJson(f.readText())
                    stylesMap[saved.id] = saved
                } catch (_: Exception) {
                    // Ignore corrupted individual files
                }
            }
        }

        return stylesMap.values.toList()
    }

    fun delete(id: String): Boolean {
        val file = File(stylesDirectory, "$id.json")
        return if (file.exists()) file.delete() else false
    }
}

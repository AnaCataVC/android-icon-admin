package com.anacatalina.androidiconsadmin.core.settings

import com.anacatalina.androidiconsadmin.core.styling.ExtractionMode
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

@Serializable
data class AppSettings(
    val activeStyleId: String = "style-soft-lavender",
    val defaultExtractionMode: ExtractionMode = ExtractionMode.SMART_FOREGROUND,
    val defaultCanvasSize: Int = 256,
    val exportSize: Int = 512,
    val showSystemApps: Boolean = false,
    val autoSyncShortcutsOnApply: Boolean = true,
    val autoSyncWidgetsOnApply: Boolean = true
)

class AppSettingsService(
    private val settingsFile: File
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun loadSettings(): AppSettings {
        if (!settingsFile.exists()) return AppSettings()
        return try {
            json.decodeFromString<AppSettings>(settingsFile.readText())
        } catch (_: Exception) {
            val bak = File(settingsFile.parentFile, "${settingsFile.name}.bak")
            if (bak.exists()) {
                try {
                    json.decodeFromString<AppSettings>(bak.readText())
                } catch (_: Exception) {
                    AppSettings()
                }
            } else {
                AppSettings()
            }
        }
    }

    fun saveSettings(settings: AppSettings) {
        val parent = settingsFile.parentFile
        if (parent != null && !parent.exists()) parent.mkdirs()

        val temp = File(parent, "${settingsFile.name}.${UUID.randomUUID()}.tmp")
        try {
            temp.writeText(json.encodeToString(settings))
            if (settingsFile.exists()) {
                val bak = File(parent, "${settingsFile.name}.bak")
                if (bak.exists()) bak.delete()
                settingsFile.renameTo(bak)
            }
            if (!temp.renameTo(settingsFile)) {
                temp.copyTo(settingsFile, overwrite = true)
                temp.delete()
            }
        } finally {
            if (temp.exists()) temp.delete()
        }
    }
}

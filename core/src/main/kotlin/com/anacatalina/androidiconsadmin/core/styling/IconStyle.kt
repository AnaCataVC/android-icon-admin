package com.anacatalina.androidiconsadmin.core.styling

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class IconStyle(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val id: String,
    val name: String,
    val container: ContainerSpec,
    val treatment: TreatmentSpec,
    val extractionMode: ExtractionMode = ExtractionMode.SMART_FOREGROUND
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1

        val jsonSerializer = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        fun createDefault(name: String = "Soft Lavender"): IconStyle = IconStyle(
            id = "style-soft-lavender",
            name = name,
            container = ContainerSpec(
                shape = ContainerShape.SQUIRCLE,
                backgroundColor = "#E6DBFA",
                padding = 0.16
            ),
            treatment = TreatmentSpec(
                saturation = -0.15,
                brightness = 0.05,
                contrast = 0.0,
                tintColor = null
            ),
            extractionMode = ExtractionMode.SMART_FOREGROUND
        )

        fun createOneUiSquirclePreset(name: String = "One UI Lavender"): IconStyle = IconStyle(
            id = "style-one-ui-lavender",
            name = name,
            container = ContainerSpec(
                shape = ContainerShape.ONE_UI_SQUIRCLE,
                backgroundColor = "#EDE7F6",
                padding = 0.15
            ),
            treatment = TreatmentSpec(
                saturation = -0.10,
                brightness = 0.04,
                contrast = 0.02,
                tintColor = null
            ),
            extractionMode = ExtractionMode.SMART_FOREGROUND
        )

        fun createMidnightSlatePreset(): IconStyle = IconStyle(
            id = "style-midnight-slate",
            name = "Midnight Slate",
            container = ContainerSpec(
                shape = ContainerShape.SQUIRCLE,
                backgroundColor = "#1E222B",
                padding = 0.16
            ),
            treatment = TreatmentSpec(
                saturation = -0.20,
                brightness = -0.05,
                contrast = 0.10,
                tintColor = "#8EA3C0"
            ),
            extractionMode = ExtractionMode.SMART_FOREGROUND
        )

        fun createForestSagePreset(): IconStyle = IconStyle(
            id = "style-forest-sage",
            name = "Forest Sage",
            container = ContainerSpec(
                shape = ContainerShape.ROUNDED_SQUARE,
                backgroundColor = "#D5E2D4",
                padding = 0.16
            ),
            treatment = TreatmentSpec(
                saturation = -0.15,
                brightness = 0.03,
                contrast = 0.0,
                tintColor = "#466B50"
            ),
            extractionMode = ExtractionMode.SMART_FOREGROUND
        )

        fun createWarmTerracottaPreset(): IconStyle = IconStyle(
            id = "style-warm-terracotta",
            name = "Warm Terracotta",
            container = ContainerSpec(
                shape = ContainerShape.CIRCLE,
                backgroundColor = "#F7DDD4",
                padding = 0.16
            ),
            treatment = TreatmentSpec(
                saturation = -0.10,
                brightness = 0.04,
                contrast = 0.0,
                tintColor = "#B85D3B"
            ),
            extractionMode = ExtractionMode.SMART_FOREGROUND
        )

        fun createMonochromeMaterialPreset(): IconStyle = IconStyle(
            id = "style-monochrome-material",
            name = "Monochrome Material",
            container = ContainerSpec(
                shape = ContainerShape.ONE_UI_SQUIRCLE,
                backgroundColor = "#E8EAED",
                padding = 0.18
            ),
            treatment = TreatmentSpec(
                saturation = -1.0,
                brightness = 0.05,
                contrast = 0.15,
                tintColor = "#3C4043"
            ),
            extractionMode = ExtractionMode.MONOCHROME_PREFERRED
        )

        fun getStandardPresets(): List<IconStyle> = listOf(
            createDefault(),
            createOneUiSquirclePreset(),
            createMidnightSlatePreset(),
            createForestSagePreset(),
            createWarmTerracottaPreset(),
            createMonochromeMaterialPreset()
        )

        fun fromJson(json: String): IconStyle = jsonSerializer.decodeFromString(json)
    }

    fun toJson(): String = jsonSerializer.encodeToString(this)
}

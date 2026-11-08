package com.anacatalina.androidiconsadmin.core.styling

import kotlinx.serialization.Serializable

@Serializable
data class TreatmentSpec(
    val saturation: Double = 0.0,
    val brightness: Double = 0.0,
    val contrast: Double = 0.0,
    val tintColor: String? = null
) {
    init {
        require(saturation in -1.0..1.0) { "Saturation must be between -1.0 and 1.0" }
        require(brightness in -1.0..1.0) { "Brightness must be between -1.0 and 1.0" }
        require(contrast in -1.0..1.0) { "Contrast must be between -1.0 and 1.0" }
    }
}

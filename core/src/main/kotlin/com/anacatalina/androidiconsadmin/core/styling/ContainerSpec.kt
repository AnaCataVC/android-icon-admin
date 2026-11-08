package com.anacatalina.androidiconsadmin.core.styling

import kotlinx.serialization.Serializable

@Serializable
data class ContainerSpec(
    val shape: ContainerShape = ContainerShape.SQUIRCLE,
    val backgroundColor: String = "#E6DBFA",
    val padding: Double = 0.16
) {
    init {
        require(padding in 0.0..0.40) { "Padding must be between 0.0 and 0.40" }
        require(backgroundColor.isNotBlank()) { "BackgroundColor cannot be blank" }
    }
}

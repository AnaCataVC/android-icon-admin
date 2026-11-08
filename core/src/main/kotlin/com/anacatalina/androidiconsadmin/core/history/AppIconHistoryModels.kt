package com.anacatalina.androidiconsadmin.core.history

import kotlinx.serialization.Serializable

@Serializable
data class AppIconChangeEntry(
    val packageName: String,
    val previousStyleId: String?,
    val newStyleId: String?,
    val isCustomized: Boolean
)

@Serializable
data class AppBatchRecord(
    val batchId: String,
    val timestampMillis: Long,
    val description: String,
    val changes: List<AppIconChangeEntry>
)

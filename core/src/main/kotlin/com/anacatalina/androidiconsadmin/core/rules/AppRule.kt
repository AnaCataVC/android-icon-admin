package com.anacatalina.androidiconsadmin.core.rules

import kotlinx.serialization.Serializable

@Serializable
data class AppRule(
    val id: String,
    val name: String,
    val targetField: String = "packageName", // "packageName" or "appLabel"
    val condition: RuleCondition,
    val pattern: String,
    val targetStyleId: String,
    val priority: Int = 100,
    val isEnabled: Boolean = true
) {
    init {
        require(id.isNotBlank()) { "Rule ID cannot be blank" }
        require(pattern.isNotBlank()) { "Pattern cannot be blank" }
        require(targetStyleId.isNotBlank()) { "TargetStyleId cannot be blank" }
    }
}

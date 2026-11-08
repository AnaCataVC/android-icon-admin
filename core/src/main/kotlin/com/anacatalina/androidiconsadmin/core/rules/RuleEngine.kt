package com.anacatalina.androidiconsadmin.core.rules

import java.util.concurrent.CancellationException
import java.util.regex.Pattern

object RuleEngine {
    private const val REGEX_TIMEOUT_MS = 250L

    fun matches(rule: AppRule, packageName: String, appLabel: String): Boolean {
        if (!rule.isEnabled) return false

        val subject = if (rule.targetField.equals("appLabel", ignoreCase = true)) {
            appLabel
        } else {
            packageName
        }

        return when (rule.condition) {
            RuleCondition.CONTAINS -> subject.contains(rule.pattern, ignoreCase = true)
            RuleCondition.STARTS_WITH -> subject.startsWith(rule.pattern, ignoreCase = true)
            RuleCondition.ENDS_WITH -> subject.endsWith(rule.pattern, ignoreCase = true)
            RuleCondition.EQUALS -> subject.equals(rule.pattern, ignoreCase = true)
            RuleCondition.REGEX -> matchesRegexWithTimeout(rule.pattern, subject)
        }
    }

    fun findFirstMatchingRule(rules: List<AppRule>, packageName: String, appLabel: String): AppRule? {
        val sorted = rules.filter { it.isEnabled }.sortedBy { it.priority }
        for (rule in sorted) {
            if (matches(rule, packageName, appLabel)) {
                return rule
            }
        }
        return null
    }

    private fun matchesRegexWithTimeout(regexPattern: String, input: CharSequence): Boolean {
        return try {
            val pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(TimeoutCharSequence(input, REGEX_TIMEOUT_MS))
            matcher.find()
        } catch (_: Exception) {
            false
        }
    }

    private class TimeoutCharSequence(
        private val inner: CharSequence,
        private val timeoutMs: Long
    ) : CharSequence {
        private val startTime = System.currentTimeMillis()

        override val length: Int
            get() = inner.length

        override fun get(index: Int): Char {
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                throw CancellationException("Regex execution timed out after $timeoutMs ms")
            }
            return inner[index]
        }

        override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
            return TimeoutCharSequence(inner.subSequence(startIndex, endIndex), timeoutMs)
        }

        override fun toString(): String = inner.toString()
    }
}

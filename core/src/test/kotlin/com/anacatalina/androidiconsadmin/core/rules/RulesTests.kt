package com.anacatalina.androidiconsadmin.core.rules

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class RulesTests {

    @Test
    fun `RuleEngine matches contains, starts_with and regex`() {
        val rule1 = AppRule(
            id = "r1",
            name = "Google apps",
            targetField = "packageName",
            condition = RuleCondition.STARTS_WITH,
            pattern = "com.google",
            targetStyleId = "style-soft-lavender"
        )

        assertTrue(RuleEngine.matches(rule1, "com.google.android.apps.photos", "Photos"))
        assertFalse(RuleEngine.matches(rule1, "com.whatsapp", "WhatsApp"))

        val rule2 = AppRule(
            id = "r2",
            name = "Social regex",
            targetField = "packageName",
            condition = RuleCondition.REGEX,
            pattern = "(whatsapp|telegram|instagram)",
            targetStyleId = "style-midnight-slate"
        )

        assertTrue(RuleEngine.matches(rule2, "com.whatsapp", "WhatsApp"))
        assertTrue(RuleEngine.matches(rule2, "org.telegram.messenger", "Telegram"))
        assertFalse(RuleEngine.matches(rule2, "com.android.settings", "Settings"))
    }

    @Test
    fun `RuleEngine priority resolution`() {
        val lowPriority = AppRule("r-low", "Low", "packageName", RuleCondition.CONTAINS, "google", "style-low", priority = 100)
        val highPriority = AppRule("r-high", "High", "packageName", RuleCondition.CONTAINS, "photos", "style-high", priority = 10)

        val match = RuleEngine.findFirstMatchingRule(listOf(lowPriority, highPriority), "com.google.android.apps.photos", "Photos")
        assertNotNull(match)
        assertEquals("r-high", match?.id)
    }

    @Test
    fun `RuleStore atomic save and persistence`(@TempDir tempDir: File) {
        val file = File(tempDir, "rules.json")
        val store = RuleStore(file)
        assertTrue(store.loadRules().isEmpty())

        val rule = AppRule("r1", "Test", "packageName", RuleCondition.EQUALS, "com.test", "style-default")
        store.saveRules(listOf(rule))

        val loaded = store.loadRules()
        assertEquals(1, loaded.size)
        assertEquals("r1", loaded[0].id)
    }
}

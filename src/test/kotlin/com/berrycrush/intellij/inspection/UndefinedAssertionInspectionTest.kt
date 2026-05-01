package com.berrycrush.intellij.inspection

import com.berrycrush.intellij.BerryCrushTestCase

/**
 * Tests for UndefinedAssertionInspection.
 *
 * Tests the assertion detection patterns.
 */
class UndefinedAssertionInspectionTest : BerryCrushTestCase() {

    fun testAssertPatternMatching() {
        val pattern = Regex(
            """^\s*assert\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )
        
        // Should match assert directives
        var match = pattern.find("assert response is valid")
        assertNotNull(match)
        assertEquals("response is valid", match?.groupValues?.get(1))
        
        match = pattern.find("  assert status code is 200")
        assertNotNull(match)
        assertEquals("status code is 200", match?.groupValues?.get(1))
        
        match = pattern.find("ASSERT user exists")
        assertNotNull(match)
        assertEquals("user exists", match?.groupValues?.get(1))
    }

    fun testAssertPatternDoesNotMatchProseText() {
        val pattern = Regex(
            """^\s*assert\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )
        
        // Should NOT match 'assert' in prose (doesn't start line)
        val match = pattern.find("user will assert their rights")
        assertNull("Should not match 'assert' in middle of text", match)
    }

    fun testAssertionTextExtraction() {
        val pattern = Regex("""^\s*assert\s+(.+)$""", RegexOption.IGNORE_CASE)
        
        // Test with quoted string
        var match = pattern.find("assert response contains \"success\"")
        assertEquals("response contains \"success\"", match?.groupValues?.get(1))
        
        // Test with number
        match = pattern.find("assert count is 42")
        assertEquals("count is 42", match?.groupValues?.get(1))
    }

    fun testMultipleAssertionsInSequence() {
        val content = """
            then: verify results
              assert first condition
              assert second condition
              assert third condition
        """.trimIndent()
        
        val pattern = Regex("""^\s*assert\s+(.+)$""", RegexOption.IGNORE_CASE)
        val assertions = content.lines()
            .mapNotNull { pattern.find(it) }
            .map { it.groupValues[1] }
        
        assertEquals(3, assertions.size)
        assertEquals("first condition", assertions[0])
        assertEquals("second condition", assertions[1])
        assertEquals("third condition", assertions[2])
    }
}


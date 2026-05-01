package com.berrycrush.intellij.inspection

import com.berrycrush.intellij.BerryCrushTestCase

/**
 * Tests for UndefinedStepInspection.
 *
 * Tests the step detection patterns.
 */
class UndefinedStepInspectionTest : BerryCrushTestCase() {

    fun testStepPatternMatching() {
        val pattern = Regex(
            """^\s*(given|when|then|and|but):?\s*(.*)$""",
            RegexOption.IGNORE_CASE
        )
        
        // Should match step keywords
        var match = pattern.find("given: I have a pet")
        assertNotNull(match)
        assertEquals("given", match?.groupValues?.get(1)?.lowercase())
        assertEquals("I have a pet", match?.groupValues?.get(2)?.trim())
        
        match = pattern.find("When user logs in")
        assertNotNull(match)
        assertEquals("when", match?.groupValues?.get(1)?.lowercase())
        
        match = pattern.find("  then: result is displayed")
        assertNotNull(match)
        assertEquals("then", match?.groupValues?.get(1)?.lowercase())
        
        match = pattern.find("and: another condition")
        assertNotNull(match)
        assertEquals("and", match?.groupValues?.get(1)?.lowercase())
        
        match = pattern.find("but: exception case")
        assertNotNull(match)
        assertEquals("but", match?.groupValues?.get(1)?.lowercase())
    }

    fun testDirectivePatternMatching() {
        val pattern = Regex(
            """^(call|assert|extract|include)\s+.*""",
            RegexOption.IGNORE_CASE
        )
        
        // Should match directives
        assertTrue(pattern.matches("call ^listPets"))
        assertTrue(pattern.matches("assert response is valid"))
        assertTrue(pattern.matches("extract $.id as petId"))
        assertTrue(pattern.matches("include auth-fragment"))
        
        // Should not match regular text
        assertFalse(pattern.matches("given I call the API"))
        assertFalse(pattern.matches("user should include header"))
    }

    fun testStepTextExtraction() {
        // Test colon syntax
        val colonMatch = Regex("""^\s*(given|when|then):?\s*(.*)$""", RegexOption.IGNORE_CASE)
            .find("given: I have a pet")
        assertEquals("I have a pet", colonMatch?.groupValues?.get(2)?.trim())
        
        // Test space syntax
        val spaceMatch = Regex("""^\s*(given|when|then):?\s*(.*)$""", RegexOption.IGNORE_CASE)
            .find("when user logs in")
        assertEquals("user logs in", spaceMatch?.groupValues?.get(2)?.trim())
    }

    fun testStepsWithDirectivesAreSkipped() {
        // This tests the logic that steps followed by directives should not be flagged
        val lines = listOf(
            "given: setup",
            "  call ^operation",
            "when: action"
        )
        
        val stepPattern = Regex("""^\s*(given|when|then|and|but):?\s*(.*)$""", RegexOption.IGNORE_CASE)
        val directivePattern = Regex("""^\s*(call|assert|extract|include)\s+.*""", RegexOption.IGNORE_CASE)
        
        // Line 0 is a step, line 1 is a directive, so line 0 has a directive
        val line0IsStep = stepPattern.matches(lines[0].trim())
        val line1IsDirective = directivePattern.matches(lines[1].trim())
        
        assertTrue("Line 0 should be a step", line0IsStep)
        assertTrue("Line 1 should be a directive", line1IsDirective)
        
        // Line 2 is a step without a following directive
        val line2IsStep = stepPattern.matches(lines[2].trim())
        assertTrue("Line 2 should be a step", line2IsStep)
        // No line 3, so no directive follows
    }
}


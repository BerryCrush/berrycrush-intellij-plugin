package com.berrycrush.intellij.index

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Unit tests for IncludeUsageIndex.
 *
 * Tests the include directive regex pattern matching.
 * The pattern only matches "include" at the start of a line (with optional whitespace).
 */
class IncludeUsageIndexTest {

    // Test the regex pattern directly since integration testing requires more setup
    // Pattern matches "include fragmentName" at the start of a line only
    private val includePattern = Regex(
        """^\s*include\s+(\^?[a-zA-Z_][a-zA-Z0-9_.\-]*)""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)
    )

    @Test
    fun `pattern matches basic include`() {
        val content = "include login-steps"
        val matches = includePattern.findAll(content).toList()
        assertEquals(1, matches.size)
        assertEquals("login-steps", matches[0].groupValues[1])
    }

    @Test
    fun `pattern matches include with caret`() {
        val content = "include ^operation-fragment"
        val matches = includePattern.findAll(content).toList()
        assertEquals(1, matches.size)
        assertEquals("^operation-fragment", matches[0].groupValues[1])
    }

    @Test
    fun `pattern matches multiple includes`() {
        val content = """
            include setup-steps
            include auth-steps
            include cleanup-steps
        """.trimIndent()
        val matches = includePattern.findAll(content).toList()
        assertEquals(3, matches.size)
        assertEquals("setup-steps", matches[0].groupValues[1])
        assertEquals("auth-steps", matches[1].groupValues[1])
        assertEquals("cleanup-steps", matches[2].groupValues[1])
    }

    @Test
    fun `pattern is case insensitive`() {
        val content1 = "INCLUDE uppercase-fragment"
        val content2 = "Include mixedCase-fragment"
        
        val matches1 = includePattern.findAll(content1).toList()
        val matches2 = includePattern.findAll(content2).toList()
        
        assertEquals("uppercase-fragment", matches1[0].groupValues[1])
        assertEquals("mixedCase-fragment", matches2[0].groupValues[1])
    }

    @Test
    fun `pattern matches fragment name with dots`() {
        val content = "include api.v1.steps"
        val matches = includePattern.findAll(content).toList()
        assertEquals("api.v1.steps", matches[0].groupValues[1])
    }

    @Test
    fun `pattern matches fragment name with dashes`() {
        val content = "include my-custom-fragment"
        val matches = includePattern.findAll(content).toList()
        assertEquals("my-custom-fragment", matches[0].groupValues[1])
    }

    @Test
    fun `pattern matches fragment name with underscores`() {
        val content = "include my_custom_fragment"
        val matches = includePattern.findAll(content).toList()
        assertEquals("my_custom_fragment", matches[0].groupValues[1])
    }

    @Test
    fun `pattern requires whitespace after include`() {
        val content = "includenospace"
        val matches = includePattern.findAll(content).toList()
        assertEquals(0, matches.size)
    }

    @Test
    fun `pattern does not match include in middle of line`() {
        val content = "Given I include the header"
        val matches = includePattern.findAll(content).toList()
        // Should not match because "include" is not at the start of the line
        assertEquals(0, matches.size)
    }

    @Test
    fun `pattern matches in multiline content`() {
        val content = """
            Scenario: Test
            include login-steps
            Given user is logged in
        """.trimIndent()
        val matches = includePattern.findAll(content).toList()
        assertEquals(1, matches.size)
        assertEquals("login-steps", matches[0].groupValues[1])
    }

    @Test
    fun `pattern matches with leading whitespace`() {
        val content = "    include indented-fragment"
        val matches = includePattern.findAll(content).toList()
        assertEquals(1, matches.size)
        assertEquals("indented-fragment", matches[0].groupValues[1])
    }

    @Test
    fun `pattern does not match includes in step text`() {
        val content = """
            Given the system includes a header
            When I include the data
            Then it includes the result
        """.trimIndent()
        val matches = includePattern.findAll(content).toList()
        // None should match because "include" is in the middle of step text
        assertEquals(0, matches.size)
    }
}

package com.berrycrush.intellij.reference

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for BerryCrushFragmentReference content parsing logic.
 */
class BerryCrushFragmentReferenceTest {

    // Replicate the fragment name extraction logic for testing
    // Note: Uses case-insensitive matching for "fragment"
    private fun extractFragmentNameFromContent(text: String): String? {
        val fragmentMatch = Regex("^\\s*fragment:\\s*(\\S+)", setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE)).find(text)
        return fragmentMatch?.groupValues?.get(1)
    }

    @Test
    fun `extracts fragment name from beginning of file`() {
        val content = """
            fragment: login-steps
            Given user is on login page
            When user enters credentials
        """.trimIndent()

        assertEquals("login-steps", extractFragmentNameFromContent(content))
    }

    @Test
    fun `extracts fragment name with capitalized keyword`() {
        val content = """
            Fragment: auth-flow
            Given user has valid token
        """.trimIndent()

        assertEquals("auth-flow", extractFragmentNameFromContent(content))
    }

    @Test
    fun `extracts fragment name with extra whitespace`() {
        val content = """
            fragment:    my-fragment
            Given test step
        """.trimIndent()

        assertEquals("my-fragment", extractFragmentNameFromContent(content))
    }

    @Test
    fun `extracts fragment name with leading whitespace`() {
        val content = """
              fragment: indented-fragment
            Given test step
        """.trimIndent()

        assertEquals("indented-fragment", extractFragmentNameFromContent(content))
    }

    @Test
    fun `returns null when no fragment directive`() {
        val content = """
            # This is a comment
            Given some step
            Then some assertion
        """.trimIndent()

        assertNull(extractFragmentNameFromContent(content))
    }

    @Test
    fun `extracts first fragment when multiple present`() {
        val content = """
            fragment: first-fragment
            Given step one
            
            fragment: second-fragment
            Given step two
        """.trimIndent()

        // The regex finds the first match
        assertEquals("first-fragment", extractFragmentNameFromContent(content))
    }

    @Test
    fun `handles fragment name with dots`() {
        val content = """
            fragment: api.v1.steps
            Given test step
        """.trimIndent()

        assertEquals("api.v1.steps", extractFragmentNameFromContent(content))
    }

    @Test
    fun `handles fragment name with underscores`() {
        val content = """
            fragment: my_custom_fragment
            Given test step
        """.trimIndent()

        assertEquals("my_custom_fragment", extractFragmentNameFromContent(content))
    }

    @Test
    fun `handles fragment name with numbers`() {
        val content = """
            fragment: step123
            Given test step
        """.trimIndent()

        assertEquals("step123", extractFragmentNameFromContent(content))
    }

    @Test
    fun `returns null for empty content`() {
        assertNull(extractFragmentNameFromContent(""))
    }
}

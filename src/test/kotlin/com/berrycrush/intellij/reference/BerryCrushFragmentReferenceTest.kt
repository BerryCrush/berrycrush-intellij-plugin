package com.berrycrush.intellij.reference

import com.berrycrush.intellij.navigation.BerryCrushLineMarkerProvider
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for BerryCrushFragmentReference content parsing logic.
 */
class BerryCrushFragmentReferenceTest {

    @Test
    fun `extracts fragment name from beginning of file`() {
        val content = """
            fragment: login-steps
            given user is on login page
            when user enters credentials
        """.trimIndent()

        assertEquals("login-steps", BerryCrushLineMarkerProvider.extractFragmentName(content))
    }

    @Test
    fun `extracts fragment name with lowercase keyword`() {
        val content = """
            fragment: auth-flow
            given user has valid token
        """.trimIndent()

        assertEquals("auth-flow", BerryCrushLineMarkerProvider.extractFragmentName(content))
    }

    @Test
    fun `extracts fragment name with extra whitespace`() {
        val content = """
            fragment:    my-fragment
            given test step
        """.trimIndent()

        assertEquals("my-fragment", BerryCrushLineMarkerProvider.extractFragmentName(content))
    }

    @Test
    fun `extracts fragment name with leading whitespace`() {
        val content = """
              fragment: indented-fragment
            given test step
        """.trimIndent()

        assertEquals("indented-fragment", BerryCrushLineMarkerProvider.extractFragmentName(content))
    }

    @Test
    fun `returns null when no fragment directive`() {
        val content = """
            # This is a comment
            given some step
            then some assertion
        """.trimIndent()

        assertNull(BerryCrushLineMarkerProvider.extractFragmentName(content))
    }

    @Test
    fun `extracts first fragment when multiple present`() {
        val content = """
            fragment: first-fragment
            given step one
            
            fragment: second-fragment
            given step two
        """.trimIndent()

        // The regex finds the first match
        assertEquals("first-fragment", BerryCrushLineMarkerProvider.extractFragmentName(content))
    }

    @Test
    fun `handles fragment name with dots`() {
        val content = """
            fragment: api.v1.steps
            given test step
        """.trimIndent()

        assertEquals("api.v1.steps", BerryCrushLineMarkerProvider.extractFragmentName(content))
    }

    @Test
    fun `handles fragment name with underscores`() {
        val content = """
            fragment: my_custom_fragment
            given test step
        """.trimIndent()

        assertEquals("my_custom_fragment", BerryCrushLineMarkerProvider.extractFragmentName(content))
    }

    @Test
    fun `handles fragment name with numbers`() {
        val content = """
            fragment: step123
            given test step
        """.trimIndent()

        assertEquals("step123", BerryCrushLineMarkerProvider.extractFragmentName(content))
    }

    @Test
    fun `returns null for empty content`() {
        assertNull(BerryCrushLineMarkerProvider.extractFragmentName(""))
    }
}

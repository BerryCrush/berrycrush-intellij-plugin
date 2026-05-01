package com.berrycrush.intellij.navigation

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Unit tests for BerryCrushLineMarkerProvider helper functions.
 * BerryCrush uses strict lowercase keywords only.
 */
class BerryCrushLineMarkerProviderTest {

    // Tests for extractStepText
    @Test
    fun `extractStepText returns text after given`() {
        assertEquals("user is logged in", BerryCrushLineMarkerProvider.extractStepText("given user is logged in"))
    }

    @Test
    fun `extractStepText returns text after when`() {
        assertEquals("user clicks submit", BerryCrushLineMarkerProvider.extractStepText("when user clicks submit"))
    }

    @Test
    fun `extractStepText returns text after then`() {
        assertEquals("success message appears", BerryCrushLineMarkerProvider.extractStepText("then success message appears"))
    }

    @Test
    fun `extractStepText returns text after and`() {
        assertEquals("user has items", BerryCrushLineMarkerProvider.extractStepText("and user has items"))
    }

    @Test
    fun `extractStepText returns text after but`() {
        assertEquals("no errors shown", BerryCrushLineMarkerProvider.extractStepText("but no errors shown"))
    }

    @Test
    fun `extractStepText requires strict lowercase`() {
        assertEquals("test", BerryCrushLineMarkerProvider.extractStepText("given test"))
        // Mixed/uppercase keywords should NOT match
        assertNull(BerryCrushLineMarkerProvider.extractStepText("GIVEN test"))
        assertNull(BerryCrushLineMarkerProvider.extractStepText("Given test"))
    }

    @Test
    fun `extractStepText trims whitespace`() {
        assertEquals("user is logged in", BerryCrushLineMarkerProvider.extractStepText("  given  user is logged in  "))
    }

    @Test
    fun `extractStepText returns null for non-step lines`() {
        assertNull(BerryCrushLineMarkerProvider.extractStepText("feature: Test"))
        assertNull(BerryCrushLineMarkerProvider.extractStepText("scenario: Test"))
        assertNull(BerryCrushLineMarkerProvider.extractStepText("# Comment"))
        assertNull(BerryCrushLineMarkerProvider.extractStepText(""))
    }

    @Test
    fun `extractStepText returns null for lines without step prefix`() {
        assertNull(BerryCrushLineMarkerProvider.extractStepText("user is logged in"))
        assertNull(BerryCrushLineMarkerProvider.extractStepText("assert response is valid"))
    }

    // Tests for extractAssertionText
    @Test
    fun `extractAssertionText returns text after assert`() {
        assertEquals("response is valid", BerryCrushLineMarkerProvider.extractAssertionText("assert response is valid"))
    }

    @Test
    fun `extractAssertionText requires strict lowercase`() {
        assertEquals("test", BerryCrushLineMarkerProvider.extractAssertionText("assert test"))
        // Mixed/uppercase keywords should NOT match
        assertNull(BerryCrushLineMarkerProvider.extractAssertionText("ASSERT TEST"))
        assertNull(BerryCrushLineMarkerProvider.extractAssertionText("Assert test"))
    }

    @Test
    fun `extractAssertionText trims whitespace`() {
        assertEquals("valid", BerryCrushLineMarkerProvider.extractAssertionText("  assert  valid  "))
    }

    @Test
    fun `extractAssertionText returns null for non-assertion lines`() {
        assertNull(BerryCrushLineMarkerProvider.extractAssertionText("given test"))
        assertNull(BerryCrushLineMarkerProvider.extractAssertionText("feature: Test"))
        assertNull(BerryCrushLineMarkerProvider.extractAssertionText(""))
    }

    // Tests for extractFragmentName
    @Test
    fun `extractFragmentName returns fragment name`() {
        assertEquals("login-steps", BerryCrushLineMarkerProvider.extractFragmentName("fragment: login-steps"))
    }

    @Test
    fun `extractFragmentName requires strict lowercase`() {
        assertEquals("test", BerryCrushLineMarkerProvider.extractFragmentName("fragment: test"))
        // Mixed/uppercase keywords should NOT match
        assertNull(BerryCrushLineMarkerProvider.extractFragmentName("FRAGMENT: TEST"))
        assertNull(BerryCrushLineMarkerProvider.extractFragmentName("Fragment: test"))
    }

    @Test
    fun `extractFragmentName handles extra whitespace`() {
        assertEquals("my-fragment", BerryCrushLineMarkerProvider.extractFragmentName("fragment:    my-fragment"))
    }

    @Test
    fun `extractFragmentName returns null when no fragment`() {
        assertNull(BerryCrushLineMarkerProvider.extractFragmentName("given test"))
        assertNull(BerryCrushLineMarkerProvider.extractFragmentName(""))
    }

    // Tests for extractIncludeFragmentName
    @Test
    fun `extractIncludeFragmentName returns fragment name from include`() {
        assertEquals("login-steps", BerryCrushLineMarkerProvider.extractIncludeFragmentName("include login-steps"))
    }

    @Test
    fun `extractIncludeFragmentName handles caret prefix`() {
        assertEquals("operation-fragment", BerryCrushLineMarkerProvider.extractIncludeFragmentName("include ^operation-fragment"))
    }

    @Test
    fun `extractIncludeFragmentName requires strict lowercase`() {
        assertEquals("test", BerryCrushLineMarkerProvider.extractIncludeFragmentName("include test"))
        // Mixed/uppercase keywords should NOT match
        assertNull(BerryCrushLineMarkerProvider.extractIncludeFragmentName("INCLUDE test"))
        assertNull(BerryCrushLineMarkerProvider.extractIncludeFragmentName("Include test"))
    }

    @Test
    fun `extractIncludeFragmentName handles extra whitespace`() {
        assertEquals("my-fragment", BerryCrushLineMarkerProvider.extractIncludeFragmentName("include    my-fragment"))
    }

    @Test
    fun `extractIncludeFragmentName returns null when no include`() {
        assertNull(BerryCrushLineMarkerProvider.extractIncludeFragmentName("given test"))
        assertNull(BerryCrushLineMarkerProvider.extractIncludeFragmentName(""))
    }

    // Tests for isStepKeyword
    @Test
    fun `isStepKeyword recognizes lowercase given`() {
        assertTrue(BerryCrushLineMarkerProvider.isStepKeyword("given"))
        // Mixed/uppercase should NOT match
        assertFalse(BerryCrushLineMarkerProvider.isStepKeyword("Given"))
        assertFalse(BerryCrushLineMarkerProvider.isStepKeyword("GIVEN"))
    }

    @Test
    fun `isStepKeyword recognizes lowercase when`() {
        assertTrue(BerryCrushLineMarkerProvider.isStepKeyword("when"))
        assertFalse(BerryCrushLineMarkerProvider.isStepKeyword("When"))
    }

    @Test
    fun `isStepKeyword recognizes lowercase then`() {
        assertTrue(BerryCrushLineMarkerProvider.isStepKeyword("then"))
        assertFalse(BerryCrushLineMarkerProvider.isStepKeyword("Then"))
    }

    @Test
    fun `isStepKeyword recognizes lowercase and`() {
        assertTrue(BerryCrushLineMarkerProvider.isStepKeyword("and"))
        assertFalse(BerryCrushLineMarkerProvider.isStepKeyword("And"))
    }

    @Test
    fun `isStepKeyword recognizes lowercase but`() {
        assertTrue(BerryCrushLineMarkerProvider.isStepKeyword("but"))
        assertFalse(BerryCrushLineMarkerProvider.isStepKeyword("But"))
    }

    @Test
    fun `isStepKeyword returns false for non-keywords`() {
        assertFalse(BerryCrushLineMarkerProvider.isStepKeyword("feature"))
        assertFalse(BerryCrushLineMarkerProvider.isStepKeyword("scenario"))
        assertFalse(BerryCrushLineMarkerProvider.isStepKeyword("assert"))
        assertFalse(BerryCrushLineMarkerProvider.isStepKeyword(""))
    }
}

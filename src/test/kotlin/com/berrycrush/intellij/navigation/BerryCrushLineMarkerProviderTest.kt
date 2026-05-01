package com.berrycrush.intellij.navigation

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Unit tests for BerryCrushLineMarkerProvider helper functions.
 */
class BerryCrushLineMarkerProviderTest {

    // Tests for extractStepText
    @Test
    fun `extractStepText returns text after Given`() {
        assertEquals("user is logged in", BerryCrushLineMarkerProvider.extractStepText("Given user is logged in"))
    }

    @Test
    fun `extractStepText returns text after When`() {
        assertEquals("user clicks submit", BerryCrushLineMarkerProvider.extractStepText("When user clicks submit"))
    }

    @Test
    fun `extractStepText returns text after Then`() {
        assertEquals("success message appears", BerryCrushLineMarkerProvider.extractStepText("Then success message appears"))
    }

    @Test
    fun `extractStepText returns text after And`() {
        assertEquals("user has items", BerryCrushLineMarkerProvider.extractStepText("And user has items"))
    }

    @Test
    fun `extractStepText returns text after But`() {
        assertEquals("no errors shown", BerryCrushLineMarkerProvider.extractStepText("But no errors shown"))
    }

    @Test
    fun `extractStepText is case insensitive`() {
        assertEquals("test", BerryCrushLineMarkerProvider.extractStepText("given test"))
        assertEquals("test", BerryCrushLineMarkerProvider.extractStepText("GIVEN test"))
        assertEquals("test", BerryCrushLineMarkerProvider.extractStepText("Given test"))
    }

    @Test
    fun `extractStepText trims whitespace`() {
        assertEquals("user is logged in", BerryCrushLineMarkerProvider.extractStepText("  Given  user is logged in  "))
    }

    @Test
    fun `extractStepText returns null for non-step lines`() {
        assertNull(BerryCrushLineMarkerProvider.extractStepText("Feature: Test"))
        assertNull(BerryCrushLineMarkerProvider.extractStepText("Scenario: Test"))
        assertNull(BerryCrushLineMarkerProvider.extractStepText("# Comment"))
        assertNull(BerryCrushLineMarkerProvider.extractStepText(""))
    }

    @Test
    fun `extractStepText returns null for lines without step prefix`() {
        assertNull(BerryCrushLineMarkerProvider.extractStepText("user is logged in"))
        assertNull(BerryCrushLineMarkerProvider.extractStepText("Assert: response is valid"))
    }

    // Tests for extractAssertionText
    @Test
    fun `extractAssertionText returns text after Assert`() {
        assertEquals("response is valid", BerryCrushLineMarkerProvider.extractAssertionText("Assert response is valid"))
    }

    @Test
    fun `extractAssertionText is case insensitive`() {
        assertEquals("test", BerryCrushLineMarkerProvider.extractAssertionText("assert test"))
        assertEquals("TEST", BerryCrushLineMarkerProvider.extractAssertionText("ASSERT TEST"))
    }

    @Test
    fun `extractAssertionText trims whitespace`() {
        assertEquals("valid", BerryCrushLineMarkerProvider.extractAssertionText("  Assert  valid  "))
    }

    @Test
    fun `extractAssertionText returns null for non-assertion lines`() {
        assertNull(BerryCrushLineMarkerProvider.extractAssertionText("Given test"))
        assertNull(BerryCrushLineMarkerProvider.extractAssertionText("Feature: Test"))
        assertNull(BerryCrushLineMarkerProvider.extractAssertionText(""))
    }

    // Tests for extractFragmentName
    @Test
    fun `extractFragmentName returns fragment name`() {
        assertEquals("login-steps", BerryCrushLineMarkerProvider.extractFragmentName("Fragment: login-steps"))
    }

    @Test
    fun `extractFragmentName is case insensitive`() {
        assertEquals("test", BerryCrushLineMarkerProvider.extractFragmentName("fragment: test"))
        assertEquals("TEST", BerryCrushLineMarkerProvider.extractFragmentName("FRAGMENT: TEST"))
    }

    @Test
    fun `extractFragmentName handles extra whitespace`() {
        assertEquals("my-fragment", BerryCrushLineMarkerProvider.extractFragmentName("Fragment:    my-fragment"))
    }

    @Test
    fun `extractFragmentName returns null when no fragment`() {
        assertNull(BerryCrushLineMarkerProvider.extractFragmentName("Given test"))
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
    fun `extractIncludeFragmentName is case insensitive`() {
        assertEquals("test", BerryCrushLineMarkerProvider.extractIncludeFragmentName("INCLUDE test"))
        assertEquals("test", BerryCrushLineMarkerProvider.extractIncludeFragmentName("Include test"))
    }

    @Test
    fun `extractIncludeFragmentName handles extra whitespace`() {
        assertEquals("my-fragment", BerryCrushLineMarkerProvider.extractIncludeFragmentName("include    my-fragment"))
    }

    @Test
    fun `extractIncludeFragmentName returns null when no include`() {
        assertNull(BerryCrushLineMarkerProvider.extractIncludeFragmentName("Given test"))
        assertNull(BerryCrushLineMarkerProvider.extractIncludeFragmentName(""))
    }

    // Tests for isStepKeyword
    @Test
    fun `isStepKeyword recognizes Given`() {
        assertTrue(BerryCrushLineMarkerProvider.isStepKeyword("Given"))
        assertTrue(BerryCrushLineMarkerProvider.isStepKeyword("given"))
        assertTrue(BerryCrushLineMarkerProvider.isStepKeyword("GIVEN"))
    }

    @Test
    fun `isStepKeyword recognizes When`() {
        assertTrue(BerryCrushLineMarkerProvider.isStepKeyword("When"))
        assertTrue(BerryCrushLineMarkerProvider.isStepKeyword("when"))
    }

    @Test
    fun `isStepKeyword recognizes Then`() {
        assertTrue(BerryCrushLineMarkerProvider.isStepKeyword("Then"))
        assertTrue(BerryCrushLineMarkerProvider.isStepKeyword("then"))
    }

    @Test
    fun `isStepKeyword recognizes And`() {
        assertTrue(BerryCrushLineMarkerProvider.isStepKeyword("And"))
        assertTrue(BerryCrushLineMarkerProvider.isStepKeyword("and"))
    }

    @Test
    fun `isStepKeyword recognizes But`() {
        assertTrue(BerryCrushLineMarkerProvider.isStepKeyword("But"))
        assertTrue(BerryCrushLineMarkerProvider.isStepKeyword("but"))
    }

    @Test
    fun `isStepKeyword returns false for non-keywords`() {
        assertFalse(BerryCrushLineMarkerProvider.isStepKeyword("Feature"))
        assertFalse(BerryCrushLineMarkerProvider.isStepKeyword("Scenario"))
        assertFalse(BerryCrushLineMarkerProvider.isStepKeyword("Assert"))
        assertFalse(BerryCrushLineMarkerProvider.isStepKeyword(""))
    }
}

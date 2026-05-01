package com.berrycrush.intellij.reference

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Unit tests for BerryCrushStepReference pattern matching logic.
 *
 * Tests the pattern-to-regex conversion and matching functionality.
 */
class BerryCrushStepReferenceTest {

    // Replicate the pattern matching logic for testing
    // Note: Using Regex.escapeReplacement to preserve backslashes
    private fun matchesPattern(stepText: String, pattern: String): Boolean {
        val regexPattern = pattern
            .replace(Regex("""\{int\}"""), Regex.escapeReplacement("""(-?\d+)"""))
            .replace(Regex("""\{string\}"""), Regex.escapeReplacement("""("[^"]*"|'[^']*')"""))
            .replace(Regex("""\{word\}"""), Regex.escapeReplacement("""(\w+)"""))
            .replace(Regex("""\{float\}"""), Regex.escapeReplacement("""(-?\d+\.?\d*)"""))
            .replace(Regex("""\{any\}"""), Regex.escapeReplacement("""(.+?)"""))
            .let { "^$it$" }

        return try {
            Regex(regexPattern, RegexOption.IGNORE_CASE).matches(stepText)
        } catch (e: Exception) {
            stepText.contains(pattern, ignoreCase = true)
        }
    }

    @Test
    fun `matches simple pattern without placeholders`() {
        assertTrue(matchesPattern("user is logged in", "user is logged in"))
        assertFalse(matchesPattern("user is logged out", "user is logged in"))
    }

    @Test
    fun `matches int placeholder`() {
        assertTrue(matchesPattern("user has 5 items", "user has {int} items"))
        assertTrue(matchesPattern("user has 100 items", "user has {int} items"))
        assertTrue(matchesPattern("user has -42 items", "user has {int} items"))
        assertFalse(matchesPattern("user has many items", "user has {int} items"))
    }

    @Test
    fun `matches string placeholder with double quotes`() {
        assertTrue(matchesPattern("user clicks \"submit button\"", "user clicks {string}"))
        assertTrue(matchesPattern("user clicks \"\"", "user clicks {string}"))
    }

    @Test
    fun `matches string placeholder with single quotes`() {
        assertTrue(matchesPattern("user clicks 'cancel'", "user clicks {string}"))
    }

    @Test
    fun `matches word placeholder`() {
        assertTrue(matchesPattern("method is GET", "method is {word}"))
        assertTrue(matchesPattern("method is POST", "method is {word}"))
        assertFalse(matchesPattern("method is GET /api", "method is {word}"))
    }

    @Test
    fun `matches float placeholder`() {
        assertTrue(matchesPattern("price is 99.99", "price is {float}"))
        assertTrue(matchesPattern("price is -10.5", "price is {float}"))
        assertTrue(matchesPattern("price is 100", "price is {float}"))
    }

    @Test
    fun `matches any placeholder`() {
        assertTrue(matchesPattern("response contains hello world", "response contains {any}"))
        assertTrue(matchesPattern("response contains 123", "response contains {any}"))
    }

    @Test
    fun `matches multiple placeholders`() {
        assertTrue(matchesPattern(
            "user \"john\" has 5 items",
            "user {string} has {int} items"
        ))
    }

    @Test
    fun `is case insensitive`() {
        assertTrue(matchesPattern("USER IS LOGGED IN", "user is logged in"))
        assertTrue(matchesPattern("User Is Logged In", "user is logged in"))
    }

    @Test
    fun `handles special regex characters in pattern`() {
        // Patterns might contain characters that are special in regex
        assertTrue(matchesPattern("path is /api/users", "path is /api/users"))
    }
}

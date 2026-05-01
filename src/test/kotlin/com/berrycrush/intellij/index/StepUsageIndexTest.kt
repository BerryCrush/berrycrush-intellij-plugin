package com.berrycrush.intellij.index

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for StepUsageIndex static methods.
 * Tests pattern matching, step text extraction, and assertion text extraction.
 */
class StepUsageIndexTest {

    @Nested
    inner class PatternToRegexTests {

        @Test
        fun `simple pattern without placeholders`() {
            val regex = StepUsageIndex.patternToRegex("user is logged in")
            assertTrue(regex.matches("user is logged in"))
            assertFalse(regex.matches("user is not logged in"))
        }

        @Test
        fun `pattern is case insensitive`() {
            val regex = StepUsageIndex.patternToRegex("User Is Logged In")
            assertTrue(regex.matches("user is logged in"))
            assertTrue(regex.matches("USER IS LOGGED IN"))
            assertTrue(regex.matches("User Is Logged In"))
        }

        @Test
        fun `int placeholder matches integers`() {
            val regex = StepUsageIndex.patternToRegex("user has {int} items")
            assertTrue(regex.matches("user has 5 items"))
            assertTrue(regex.matches("user has 123 items"))
            assertTrue(regex.matches("user has -42 items"))
            assertFalse(regex.matches("user has five items"))
            assertFalse(regex.matches("user has 3.5 items"))
        }

        @Test
        fun `string placeholder matches quoted and unquoted strings`() {
            val regex = StepUsageIndex.patternToRegex("user clicks {string}")
            assertTrue(regex.matches("user clicks submit"))
            assertTrue(regex.matches("user clicks \"submit button\""))
            assertTrue(regex.matches("user clicks 'cancel'"))
        }

        @Test
        fun `word placeholder matches single word`() {
            val regex = StepUsageIndex.patternToRegex("call {word} endpoint")
            assertTrue(regex.matches("call GET endpoint"))
            assertTrue(regex.matches("call POST endpoint"))
            assertFalse(regex.matches("call GET /api endpoint"))
        }

        @Test
        fun `float placeholder matches decimals`() {
            val regex = StepUsageIndex.patternToRegex("temperature is {float} degrees")
            assertTrue(regex.matches("temperature is 98.6 degrees"))
            assertTrue(regex.matches("temperature is -10.5 degrees"))
            assertTrue(regex.matches("temperature is 100 degrees"))
        }

        @Test
        fun `number placeholder matches numbers`() {
            val regex = StepUsageIndex.patternToRegex("price is {number}")
            assertTrue(regex.matches("price is 100"))
            assertTrue(regex.matches("price is 99.99"))
            assertTrue(regex.matches("price is -50.5"))
            assertFalse(regex.matches("price is expensive"))
        }

        @Test
        fun `any placeholder matches anything`() {
            val regex = StepUsageIndex.patternToRegex("response contains {any}")
            assertTrue(regex.matches("response contains hello world"))
            assertTrue(regex.matches("response contains 123"))
            assertTrue(regex.matches("response contains {\"key\": \"value\"}"))
        }

        @Test
        fun `multiple placeholders in same pattern`() {
            val regex = StepUsageIndex.patternToRegex("user {string} has {int} items at {float} each")
            assertTrue(regex.matches("user john has 5 items at 10.50 each"))
            assertTrue(regex.matches("user \"Jane Doe\" has 100 items at 1.99 each"))
        }
    }

    @Nested
    inner class MatchesPatternTests {

        @Test
        fun `exact match returns true`() {
            val regex = StepUsageIndex.patternToRegex("user is logged in")
            assertTrue(StepUsageIndex.matchesPattern("user is logged in", regex))
        }

        @Test
        fun `partial match returns false`() {
            val regex = StepUsageIndex.patternToRegex("user is logged in")
            assertFalse(StepUsageIndex.matchesPattern("user is logged in successfully", regex))
        }

        @Test
        fun `mismatch returns false`() {
            val regex = StepUsageIndex.patternToRegex("user is logged in")
            assertFalse(StepUsageIndex.matchesPattern("user is logged out", regex))
        }

        @Test
        fun `placeholder match with value`() {
            val regex = StepUsageIndex.patternToRegex("status code is {int}")
            assertTrue(StepUsageIndex.matchesPattern("status code is 200", regex))
            assertTrue(StepUsageIndex.matchesPattern("status code is 404", regex))
        }

        @Test
        fun `case insensitive matching`() {
            val regex = StepUsageIndex.patternToRegex("API returns success")
            assertTrue(StepUsageIndex.matchesPattern("api returns success", regex))
            assertTrue(StepUsageIndex.matchesPattern("API RETURNS SUCCESS", regex))
        }
    }

    @Nested
    inner class ExtractStepTextTests {

        @Test
        fun `extracts text after Given`() {
            assertEquals("user is logged in", StepUsageIndex.extractStepText("Given user is logged in"))
        }

        @Test
        fun `extracts text after When`() {
            assertEquals("user clicks submit", StepUsageIndex.extractStepText("When user clicks submit"))
        }

        @Test
        fun `extracts text after Then`() {
            assertEquals("success message appears", StepUsageIndex.extractStepText("Then success message appears"))
        }

        @Test
        fun `extracts text after And`() {
            assertEquals("user has items", StepUsageIndex.extractStepText("And user has items"))
        }

        @Test
        fun `extracts text after But`() {
            assertEquals("no errors shown", StepUsageIndex.extractStepText("But no errors shown"))
        }

        @Test
        fun `handles case insensitive prefixes`() {
            assertEquals("test", StepUsageIndex.extractStepText("given test"))
            assertEquals("test", StepUsageIndex.extractStepText("GIVEN test"))
            assertEquals("test", StepUsageIndex.extractStepText("Given test"))
        }

        @Test
        fun `trims whitespace`() {
            assertEquals("user is logged in", StepUsageIndex.extractStepText("  Given  user is logged in  "))
        }

        @Test
        fun `returns null for non-step lines`() {
            assertNull(StepUsageIndex.extractStepText("Feature: Test"))
            assertNull(StepUsageIndex.extractStepText("Scenario: Test"))
            assertNull(StepUsageIndex.extractStepText("# Comment"))
            assertNull(StepUsageIndex.extractStepText(""))
        }

        @Test
        fun `returns null for lines without step prefix`() {
            assertNull(StepUsageIndex.extractStepText("user is logged in"))
            assertNull(StepUsageIndex.extractStepText("Assert: response is valid"))
        }
    }

    @Nested
    inner class ExtractAssertionTextTests {

        @Test
        fun `extracts text after Assert`() {
            assertEquals(": response is valid", StepUsageIndex.extractAssertionText("Assert: response is valid"))
        }

        @Test
        fun `handles case insensitive Assert`() {
            assertEquals(": test", StepUsageIndex.extractAssertionText("assert: test"))
            assertEquals(": TEST", StepUsageIndex.extractAssertionText("ASSERT: TEST"))
        }

        @Test
        fun `trims whitespace`() {
            assertEquals(": valid", StepUsageIndex.extractAssertionText("  Assert: valid  "))
        }

        @Test
        fun `returns null for non-assertion lines`() {
            assertNull(StepUsageIndex.extractAssertionText("Given test"))
            assertNull(StepUsageIndex.extractAssertionText("Feature: Test"))
            assertNull(StepUsageIndex.extractAssertionText(""))
        }
    }

    @Nested
    inner class EdgeCaseTests {

        @Test
        fun `empty pattern`() {
            val regex = StepUsageIndex.patternToRegex("")
            assertTrue(regex.matches(""))
            assertFalse(regex.matches("something"))
        }

        @Test
        fun `pattern with special regex characters`() {
            val regex = StepUsageIndex.patternToRegex("path is /api/users")
            assertTrue(regex.matches("path is /api/users"))
        }

        @Test
        fun `whitespace handling`() {
            val regex = StepUsageIndex.patternToRegex("user  has  spaces")
            assertTrue(regex.matches("user  has  spaces"))
            assertFalse(regex.matches("user has spaces"))
        }

        @Test
        fun `unicode characters`() {
            val regex = StepUsageIndex.patternToRegex("用户已登录")
            assertTrue(regex.matches("用户已登录"))
        }
    }
}

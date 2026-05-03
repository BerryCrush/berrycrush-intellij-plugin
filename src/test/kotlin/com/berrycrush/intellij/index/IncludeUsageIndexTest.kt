package com.berrycrush.intellij.index

import com.berrycrush.intellij.BerryCrushTestCase

/**
 * Unit tests for IncludeUsageIndex.
 *
 * Tests the actual index class behavior using the IntelliJ testing framework.
 */
class IncludeUsageIndexTest : BerryCrushTestCase() {

    // ========== Index Detection Tests ==========

    fun testIndexesBasicInclude() {
        createScenarioFile("test", """
            scenario: test
              given: setup
                include login-steps
        """.trimIndent())

        val fragments = IncludeUsageIndex.getAllIncludedFragments(project)
        assertTrue(
            "Should index 'login-steps'",
            fragments.contains("login-steps")
        )
    }

    fun testIndexesIncludeWithCaret() {
        createScenarioFile("test2", """
            scenario: test
              given: setup
                include ^operation-fragment
        """.trimIndent())

        // Caret prefix should be removed during indexing
        val fragments = IncludeUsageIndex.getAllIncludedFragments(project)
        assertTrue(
            "Should index 'operation-fragment' (without caret)",
            fragments.contains("operation-fragment")
        )
    }

    fun testIndexesMultipleIncludes() {
        createScenarioFile("test3", """
            scenario: test
              given: setup
                include setup-steps
              when: action
                include auth-steps
              then: verify
                include cleanup-steps
        """.trimIndent())

        val fragments = IncludeUsageIndex.getAllIncludedFragments(project)
        assertTrue("Should index 'setup-steps'", fragments.contains("setup-steps"))
        assertTrue("Should index 'auth-steps'", fragments.contains("auth-steps"))
        assertTrue("Should index 'cleanup-steps'", fragments.contains("cleanup-steps"))
    }

    fun testIndexesFragmentNameWithDots() {
        createScenarioFile("test4", """
            scenario: test
              given: setup
                include api.v1.steps
        """.trimIndent())

        val fragments = IncludeUsageIndex.getAllIncludedFragments(project)
        assertTrue(
            "Should index 'api.v1.steps'",
            fragments.contains("api.v1.steps")
        )
    }

    fun testIndexesFragmentNameWithDashes() {
        createScenarioFile("test5", """
            scenario: test
              given: setup
                include my-custom-fragment
        """.trimIndent())

        val fragments = IncludeUsageIndex.getAllIncludedFragments(project)
        assertTrue(
            "Should index 'my-custom-fragment'",
            fragments.contains("my-custom-fragment")
        )
    }

    fun testIndexesFragmentNameWithUnderscores() {
        createScenarioFile("test6", """
            scenario: test
              given: setup
                include my_custom_fragment
        """.trimIndent())

        val fragments = IncludeUsageIndex.getAllIncludedFragments(project)
        assertTrue(
            "Should index 'my_custom_fragment'",
            fragments.contains("my_custom_fragment")
        )
    }

    fun testDoesNotIndexIncludeWithoutSpace() {
        createScenarioFile("test7", """
            scenario: test
              given: includenospace
        """.trimIndent())

        val fragments = IncludeUsageIndex.getAllIncludedFragments(project)
        assertFalse(
            "Should not index 'nospace'",
            fragments.contains("nospace")
        )
    }

    fun testDoesNotIndexIncludeInMiddleOfLine() {
        createScenarioFile("test8", """
            scenario: test
              given: I include the header
        """.trimIndent())

        val fragments = IncludeUsageIndex.getAllIncludedFragments(project)
        assertFalse(
            "Should not index 'the' (include not at line start)",
            fragments.contains("the")
        )
    }

    fun testGetFilesIncludingFragment() {
        // Create scenario that includes a fragment
        createScenarioFile("includer", """
            scenario: test
              given: setup
                include target-fragment
        """.trimIndent())

        // Create another file without the include
        createScenarioFile("other", """
            scenario: other
              given: setup
                call GET /api
        """.trimIndent())

        val files = IncludeUsageIndex.getFilesIncludingFragment(project, "target-fragment")
        assertEquals(
            "Should find one file including 'target-fragment'",
            1,
            files.size
        )
        assertTrue(
            "Should be the includer file",
            files.first().name == "includer.scenario"
        )
    }

    // ========== Case Sensitivity Tests ==========

    fun testIndexRequiresLowercaseInclude() {
        // Create with uppercase INCLUDE - should NOT be indexed (strict lowercase)
        createScenarioFile("test9", """
            scenario: test
              given: setup
                INCLUDE uppercase-fragment
        """.trimIndent())

        val fragments = IncludeUsageIndex.getAllIncludedFragments(project)
        // The index uses strict lowercase matching
        assertFalse(
            "Should not index with uppercase INCLUDE keyword",
            fragments.contains("uppercase-fragment")
        )
    }
}

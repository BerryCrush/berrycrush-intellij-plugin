package org.berrycrush.intellij.index

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.berrycrush.intellij.BerryCrushTestCase
import org.junit.jupiter.api.Test

/**
 * Unit tests for IncludeUsageIndex.
 *
 * Tests the actual index class behavior using the IntelliJ testing framework.
 */
class IncludeUsageIndexTest : BerryCrushTestCase() {
    // ========== Index Detection Tests ==========
    @Test
    fun testIndexesBasicInclude() {
        createScenarioFile(
            "test",
            """
            scenario: test
              given: setup
                include login-steps
            """.trimIndent(),
        )

        val fragments = getAllIncludedFragments(project)
        assertTrue(
            "Should index 'login-steps'",
            fragments.contains("login-steps"),
        )
    }

    @Test
    fun testIndexesIncludeWithCaret() {
        createScenarioFile(
            "test2",
            """
            scenario: test
              given: setup
                include operation-fragment
            """.trimIndent(),
        )

        // Caret prefix should be removed during indexing
        val fragments = getAllIncludedFragments(project)
        assertTrue(
            "Should index 'operation-fragment' (without caret)",
            fragments.contains("operation-fragment"),
        )
    }

    @Test
    fun testIndexesMultipleIncludes() {
        createScenarioFile(
            "test3",
            """
            scenario: test
              given: setup
                include setup-steps
              when: action
                include auth-steps
              then: verify
                include cleanup-steps
            """.trimIndent(),
        )

        val fragments = getAllIncludedFragments(project)
        assertTrue("Should index 'setup-steps'", fragments.contains("setup-steps"))
        assertTrue("Should index 'auth-steps'", fragments.contains("auth-steps"))
        assertTrue("Should index 'cleanup-steps'", fragments.contains("cleanup-steps"))
    }

    @Test
    fun testIndexesFragmentNameWithDots() {
        createScenarioFile(
            "test4",
            """
            scenario: test
              given: setup
                include api.v1.steps
            """.trimIndent(),
        )

        val fragments = getAllIncludedFragments(project)
        assertTrue(
            "Should index 'api.v1.steps'",
            fragments.contains("api.v1.steps"),
        )
    }

    @Test
    fun testIndexesFragmentNameWithDashes() {
        createScenarioFile(
            "test5",
            """
            scenario: test
              given: setup
                include my-custom-fragment
            """.trimIndent(),
        )

        val fragments = getAllIncludedFragments(project)
        assertTrue(
            "Should index 'my-custom-fragment'",
            fragments.contains("my-custom-fragment"),
        )
    }

    @Test
    fun testIndexesFragmentNameWithUnderscores() {
        createScenarioFile(
            "test6",
            """
            scenario: test
              given: setup
                include my_custom_fragment
            """.trimIndent(),
        )

        val fragments = getAllIncludedFragments(project)
        assertTrue(
            "Should index 'my_custom_fragment'",
            fragments.contains("my_custom_fragment"),
        )
    }

    @Test
    fun testDoesNotIndexIncludeWithoutSpace() {
        createScenarioFile(
            "test7",
            """
            scenario: test
              given: includenospace
            """.trimIndent(),
        )

        val fragments = getAllIncludedFragments(project)
        assertFalse(
            "Should not index 'nospace'",
            fragments.contains("nospace"),
        )
    }

    @Test
    fun testDoesNotIndexIncludeInMiddleOfLine() {
        createScenarioFile(
            "test8",
            """
            scenario: test
              given: I include the header
            """.trimIndent(),
        )

        val fragments = getAllIncludedFragments(project)
        assertFalse(
            "Should not index 'the' (include not at line start)",
            fragments.contains("the"),
        )
    }

    @Test
    fun testGetFilesIncludingFragment() {
        // Create scenario that includes a fragment
        createScenarioFile(
            "includer",
            """
            scenario: test
              given: setup
                include target-fragment
            """.trimIndent(),
        )

        // Create another file without the include
        createScenarioFile(
            "other",
            """
            scenario: other
              given: setup
                call GET /api
            """.trimIndent(),
        )

        val files = getFilesIncludingFragment(project, "target-fragment")
        assertEquals(
            "Should find one file including 'target-fragment'",
            1,
            files.size,
        )
        assertTrue(
            "Should be the includer file",
            files.first().name == "includer.scenario",
        )
    }

    // ========== Case Sensitivity Tests ==========

    @Test
    fun testIndexRequiresLowercaseInclude() {
        // Create with uppercase INCLUDE - should NOT be indexed (strict lowercase)
        createScenarioFile(
            "test9",
            """
            scenario: test
              given: setup
                INCLUDE uppercase-fragment
            """.trimIndent(),
        )

        val fragments = getAllIncludedFragments(project)
        // The index uses strict lowercase matching
        assertFalse(
            "Should not index with uppercase INCLUDE keyword",
            fragments.contains("uppercase-fragment"),
        )
    }

    private fun getAllIncludedFragments(project: Project) = ReadAction.compute<Collection<String>, Throwable> {
        IncludeUsageIndex.getAllIncludedFragments(project)
    }

    private fun getFilesIncludingFragment(project: Project, fragmentName: String) = ReadAction.compute<Collection<VirtualFile>, Throwable> {
        IncludeUsageIndex.getFilesIncludingFragment(project, fragmentName)
    }
}

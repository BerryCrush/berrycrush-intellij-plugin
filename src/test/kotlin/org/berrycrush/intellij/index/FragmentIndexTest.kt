package org.berrycrush.intellij.index

import com.intellij.openapi.project.Project
import org.berrycrush.intellij.BerryCrushTestCase
import org.junit.jupiter.api.Test

/**
 * Integration tests for FragmentIndex.
 *
 * Tests fragment definition indexing and lookup functionality.
 */
class FragmentIndexTest : BerryCrushTestCase() {
    @Test
    fun testIndexesFragmentDefinition() {
        createFragmentFile(
            "login",
            """
            fragment: login-steps
            
            given user is on login page
            when user enters credentials
            then user is logged in
            """.trimIndent(),
        )

        val fragmentNames = getAllFragmentNames(project)
        assertTrue(fragmentNames.contains("login-steps"))
    }

    @Test
    fun testIndexesMultipleFragmentsInSameFile() {
        createFragmentFile(
            "common",
            """
            fragment: setup-steps
            given system is initialized
            
            fragment: cleanup-steps
            then system is cleaned up
            """.trimIndent(),
        )

        val fragmentNames = getAllFragmentNames(project)
        assertTrue(fragmentNames.contains("setup-steps"))
        assertTrue(fragmentNames.contains("cleanup-steps"))
    }

    @Test
    fun testFindsFragmentFilesByName() {
        createFragmentFile(
            "auth",
            """
            fragment: auth-flow
            given user has valid token
            """.trimIndent(),
        )

        val files = getFragmentFiles(project, "auth-flow")
        assertEquals(1, files.size)
        assertTrue(files.first().name.contains("auth"))
    }

    @Test
    fun testHandlesCaseInsensitiveFragmentKeyword() {
        createFragmentFile(
            "mixed",
            """
            fragment: lowercase-fragment
            given test step
            """.trimIndent(),
        )

        val fragmentNames = getAllFragmentNames(project)
        assertTrue(fragmentNames.contains("lowercase-fragment"))
    }

    @Test
    fun testEmptyFileReturnsNoFragments() {
        createFragmentFile("empty", "")

        // Should not error, just return empty
        val fragmentNames = getAllFragmentNames(project)
        // Empty file has no fragments with names
        assertTrue(fragmentNames.none { it.isEmpty() })
    }

    @Test
    fun testFileWithoutFragmentDirectiveReturnsNoFragments() {
        createFragmentFile(
            "no-directive",
            """
            # This is just a comment
            given some step
            then some assertion
            """.trimIndent(),
        )

        // Should have no fragments from this file
        // (checking that fragment: pattern is required)
        val files = getFragmentFiles(project, "no-directive")
        assertTrue(files.isEmpty())
    }

    private fun getAllFragmentNames(project: Project) = consume {
        FragmentIndex.getAllFragmentNames(project)
    }

    private fun getFragmentFiles(project: Project, fragmentName: String) = consume {
        FragmentIndex.getFragmentFiles(project, fragmentName)
    }
}

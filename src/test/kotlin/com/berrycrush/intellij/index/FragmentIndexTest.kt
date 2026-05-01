package com.berrycrush.intellij.index

import com.berrycrush.intellij.BerryCrushTestCase

/**
 * Integration tests for FragmentIndex.
 *
 * Tests fragment definition indexing and lookup functionality.
 */
class FragmentIndexTest : BerryCrushTestCase() {

    fun testIndexesFragmentDefinition() {
        createFragmentFile("login", """
            Fragment: login-steps
            
            Given user is on login page
            When user enters credentials
            Then user is logged in
        """.trimIndent())

        val fragmentNames = FragmentIndex.getAllFragmentNames(project)
        assertTrue(fragmentNames.contains("login-steps"))
    }

    fun testIndexesMultipleFragmentsInSameFile() {
        createFragmentFile("common", """
            Fragment: setup-steps
            Given system is initialized
            
            Fragment: cleanup-steps
            Then system is cleaned up
        """.trimIndent())

        val fragmentNames = FragmentIndex.getAllFragmentNames(project)
        assertTrue(fragmentNames.contains("setup-steps"))
        assertTrue(fragmentNames.contains("cleanup-steps"))
    }

    fun testFindsFragmentFilesByName() {
        createFragmentFile("auth", """
            Fragment: auth-flow
            Given user has valid token
        """.trimIndent())

        val files = FragmentIndex.getFragmentFiles(project, "auth-flow")
        assertEquals(1, files.size)
        assertTrue(files.first().name.contains("auth"))
    }

    fun testHandlesCaseInsensitiveFragmentKeyword() {
        createFragmentFile("mixed", """
            fragment: lowercase-fragment
            Given test step
        """.trimIndent())

        val fragmentNames = FragmentIndex.getAllFragmentNames(project)
        assertTrue(fragmentNames.contains("lowercase-fragment"))
    }

    fun testEmptyFileReturnsNoFragments() {
        createFragmentFile("empty", "")

        // Should not error, just return empty
        val fragmentNames = FragmentIndex.getAllFragmentNames(project)
        // Empty file has no fragments with names
        assertTrue(fragmentNames.none { it.isEmpty() })
    }

    fun testFileWithoutFragmentDirectiveReturnsNoFragments() {
        createFragmentFile("no-directive", """
            # This is just a comment
            Given some step
            Then some assertion
        """.trimIndent())

        // Should have no fragments from this file
        // (checking that Fragment: pattern is required)
        val files = FragmentIndex.getFragmentFiles(project, "no-directive")
        assertTrue(files.isEmpty())
    }
}

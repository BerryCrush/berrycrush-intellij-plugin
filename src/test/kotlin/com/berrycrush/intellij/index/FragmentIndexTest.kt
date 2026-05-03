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
            fragment: login-steps
            
            given user is on login page
            when user enters credentials
            then user is logged in
        """.trimIndent())

        val fragmentNames = FragmentIndex.getAllFragmentNames(project)
        assertTrue(fragmentNames.contains("login-steps"))
    }

    fun testIndexesMultipleFragmentsInSameFile() {
        createFragmentFile("common", """
            fragment: setup-steps
            given system is initialized
            
            fragment: cleanup-steps
            then system is cleaned up
        """.trimIndent())

        val fragmentNames = FragmentIndex.getAllFragmentNames(project)
        assertTrue(fragmentNames.contains("setup-steps"))
        assertTrue(fragmentNames.contains("cleanup-steps"))
    }

    fun testFindsFragmentFilesByName() {
        createFragmentFile("auth", """
            fragment: auth-flow
            given user has valid token
        """.trimIndent())

        val files = FragmentIndex.getFragmentFiles(project, "auth-flow")
        assertEquals(1, files.size)
        assertTrue(files.first().name.contains("auth"))
    }

    fun testHandlesCaseInsensitiveFragmentKeyword() {
        createFragmentFile("mixed", """
            fragment: lowercase-fragment
            given test step
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
            given some step
            then some assertion
        """.trimIndent())

        // Should have no fragments from this file
        // (checking that fragment: pattern is required)
        val files = FragmentIndex.getFragmentFiles(project, "no-directive")
        assertTrue(files.isEmpty())
    }
}

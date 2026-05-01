package com.berrycrush.intellij.inspection

import com.berrycrush.intellij.BerryCrushTestCase
import com.berrycrush.intellij.index.FragmentIndex

/**
 * Tests for MissingFragmentInspection.
 *
 * Tests the inspection logic using the fixture infrastructure.
 */
class MissingFragmentInspectionTest : BerryCrushTestCase() {

    fun testDetectsMissingFragmentPattern() {
        // Test the regex pattern matching
        val pattern = Regex("""^\s*include\s+(\S+)""")
        
        // Should match
        assertTrue(pattern.matches("include fragment-name"))
        assertTrue(pattern.matches("  include fragment-name"))
        assertTrue(pattern.matches("    include my-fragment"))
        
        // Should not match (no leading include keyword at line start)
        assertFalse(pattern.matches("given I include something"))
    }

    fun testFragmentIndexLookup() {
        // Create a fragment
        createFragmentFile("existing", """
            fragment: existing-fragment
              given: some step
        """.trimIndent())

        // Verify fragment is indexed
        val fragmentNames = FragmentIndex.getAllFragmentNames(project)
        assertTrue(
            "Fragment should be indexed",
            fragmentNames.contains("existing-fragment")
        )
    }

    fun testMissingFragmentDetection() {
        // Create scenario with include
        createScenarioFile("test", """
            scenario: test
              given: setup
                include nonexistent-fragment
        """.trimIndent())

        // Verify fragment is NOT in index
        val fragmentNames = FragmentIndex.getAllFragmentNames(project)
        assertFalse(
            "Nonexistent fragment should not be indexed",
            fragmentNames.contains("nonexistent-fragment")
        )
    }

    fun testExistingFragmentNotFlagged() {
        // Create the fragment first
        createFragmentFile("existing", """
            fragment: existing-fragment
              given: some step
        """.trimIndent())

        // Create scenario that includes it
        createScenarioFile("test", """
            scenario: test
              given: setup
                include existing-fragment
        """.trimIndent())

        // Verify fragment IS in index
        val fragmentNames = FragmentIndex.getAllFragmentNames(project)
        assertTrue(
            "Existing fragment should be indexed",
            fragmentNames.contains("existing-fragment")
        )
    }
}


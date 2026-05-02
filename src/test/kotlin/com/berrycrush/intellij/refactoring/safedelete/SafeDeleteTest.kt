package com.berrycrush.intellij.refactoring.safedelete

import com.berrycrush.intellij.BerryCrushTestCase
import com.berrycrush.intellij.index.IncludeUsageIndex
import com.intellij.usageView.UsageInfo

/**
 * Tests for BerryCrush safe delete functionality.
 */
class SafeDeleteTest : BerryCrushTestCase() {

    fun testSafeDeleteProcessorHandlesFragmentFile() {
        val file = createFragmentFile("test", """
            Fragment: test-fragment
            Given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        val processor = BerryCrushSafeDeleteProcessor()
        assertTrue(processor.handlesElement(psiFile!!))
    }

    fun testSafeDeleteProcessorDoesNotHandleScenarioFile() {
        val file = createScenarioFile("test", """
            Scenario: Test
            Given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        // Safe delete only handles fragment files, not scenario files
        val processor = BerryCrushSafeDeleteProcessor()
        assertFalse(processor.handlesElement(psiFile!!))
    }

    fun testSafeDeleteFindsUsagesOfIncludedFragment() {
        // Create fragment
        val fragmentFile = createFragmentFile("common", """
            Fragment: common-steps
            Given common setup
        """.trimIndent())

        // Create scenario that includes the fragment
        createScenarioFile("test", """
            Scenario: Test
            include common-steps
            Then done
        """.trimIndent())

        // Verify include usage is indexed
        val usages = IncludeUsageIndex.findIncludeUsages(project, "common-steps")
        assertEquals(1, usages.size)

        // Test safe delete processor finds the usage
        val psiFile = psiManager.findFile(fragmentFile)
        assertNotNull(psiFile)

        val processor = BerryCrushSafeDeleteProcessor()
        val usageInfos = mutableListOf<UsageInfo>()
        processor.findUsages(psiFile!!, arrayOf(psiFile), usageInfos)

        assertEquals(1, usageInfos.size)
    }

    fun testSafeDeleteFindsNoUsagesForUnusedFragment() {
        val file = createFragmentFile("unused", """
            Fragment: unused-fragment
            Given unused step
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        val processor = BerryCrushSafeDeleteProcessor()
        val usageInfos = mutableListOf<UsageInfo>()
        processor.findUsages(psiFile!!, arrayOf(psiFile), usageInfos)

        assertEquals(0, usageInfos.size)
    }

    fun testSafeDeleteFindsMultipleUsages() {
        // Create fragment
        val fragmentFile = createFragmentFile("shared", """
            Fragment: shared-steps
            Given shared setup
        """.trimIndent())

        // Create multiple scenarios that include the fragment
        createScenarioFile("test1", """
            Scenario: Test1
            include shared-steps
        """.trimIndent())

        createScenarioFile("test2", """
            Scenario: Test2
            include shared-steps
        """.trimIndent())

        val psiFile = psiManager.findFile(fragmentFile)
        assertNotNull(psiFile)

        val processor = BerryCrushSafeDeleteProcessor()
        val usageInfos = mutableListOf<UsageInfo>()
        processor.findUsages(psiFile!!, arrayOf(psiFile), usageInfos)

        assertEquals(2, usageInfos.size)
    }
}

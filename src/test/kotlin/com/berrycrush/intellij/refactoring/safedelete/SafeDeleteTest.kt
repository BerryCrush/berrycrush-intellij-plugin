package com.berrycrush.intellij.refactoring.safedelete

import com.berrycrush.intellij.BerryCrushTestCase
import com.berrycrush.intellij.index.IncludeUsageIndex
import com.berrycrush.intellij.navigation.BerryCrushTargetElementEvaluator
import com.berrycrush.intellij.psi.BerryCrushElementTypes
import com.berrycrush.intellij.psi.BerryCrushFragmentElement
import com.berrycrush.intellij.refactoring.BerryCrushRefactoringSupportProvider
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.MultiMap

/**
 * Tests for BerryCrush safe delete functionality.
 */
class SafeDeleteTest : BerryCrushTestCase() {

    fun testSafeDeleteProcessorHandlesFragmentFile() {
        val file = createFragmentFile("test", """
            fragment: test-fragment
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        val processor = BerryCrushSafeDeleteProcessor()
        assertTrue(processor.handlesElement(psiFile!!))
    }

    fun testSafeDeleteProcessorDoesNotHandleScenarioFile() {
        val file = createScenarioFile("test", """
            scenario: Test
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        // Safe delete only handles fragment files, not scenario files
        val processor = BerryCrushSafeDeleteProcessor()
        assertFalse(processor.handlesElement(psiFile!!))
    }

    fun testSafeDeleteProcessorHandlesFragmentElement() {
        val file = createFragmentFile("test", """
            fragment: test-fragment
              given step one
              when step two
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        // Find the fragment element
        val fragmentElement = PsiTreeUtil.findChildOfType(psiFile, BerryCrushFragmentElement::class.java)
        assertNotNull("Should find fragment element in PSI tree", fragmentElement)

        val processor = BerryCrushSafeDeleteProcessor()
        assertTrue("Processor should handle fragment element", processor.handlesElement(fragmentElement!!))
    }

    fun testSafeDeleteFindsUsagesOfIncludedFragment() {
        // Create fragment
        val fragmentFile = createFragmentFile("common", """
            fragment: common-steps
            given common setup
        """.trimIndent())

        // Create scenario that includes the fragment
        createScenarioFile("test", """
            scenario: Test
            include common-steps
            then done
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

    fun testSafeDeleteFindsUsagesForFragmentElement() {
        // Create fragment file with multiple fragments
        val fragmentFile = createFragmentFile("multi", """
            fragment: first-fragment
            given first setup

            fragment: second-fragment
            given second setup
        """.trimIndent())

        // Create scenario that includes the first fragment
        createScenarioFile("test", """
            scenario: Test
            include first-fragment
        """.trimIndent())

        val psiFile = psiManager.findFile(fragmentFile)
        assertNotNull(psiFile)

        // Find the first fragment element
        val fragments = PsiTreeUtil.findChildrenOfType(psiFile, BerryCrushFragmentElement::class.java)
        assertEquals("Should find two fragments", 2, fragments.size)

        val firstFragment = fragments.first { it.fragmentName == "first-fragment" }
        assertNotNull(firstFragment)

        val processor = BerryCrushSafeDeleteProcessor()
        val usageInfos = mutableListOf<UsageInfo>()
        processor.findUsages(firstFragment, arrayOf(firstFragment), usageInfos)

        assertEquals("Should find one usage for first-fragment", 1, usageInfos.size)
    }

    fun testSafeDeleteFindsNoUsagesForUnusedFragment() {
        val file = createFragmentFile("unused", """
            fragment: unused-fragment
            given unused step
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
            fragment: shared-steps
            given shared setup
        """.trimIndent())

        // Create multiple scenarios that include the fragment
        createScenarioFile("test1", """
            scenario: Test1
            include shared-steps
        """.trimIndent())

        createScenarioFile("test2", """
            scenario: Test2
            include shared-steps
        """.trimIndent())

        val psiFile = psiManager.findFile(fragmentFile)
        assertNotNull(psiFile)

        val processor = BerryCrushSafeDeleteProcessor()
        val usageInfos = mutableListOf<UsageInfo>()
        processor.findUsages(psiFile!!, arrayOf(psiFile), usageInfos)

        assertEquals(2, usageInfos.size)
    }

    fun testRefactoringSupportProviderAllowsSafeDeleteForFragmentElement() {
        val file = createFragmentFile("test", """
            fragment: test-fragment
              given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        val fragmentElement = PsiTreeUtil.findChildOfType(psiFile, BerryCrushFragmentElement::class.java)
        assertNotNull(fragmentElement)

        val provider = BerryCrushRefactoringSupportProvider()
        assertTrue(
            "Safe delete should be available for fragment element",
            provider.isSafeDeleteAvailable(fragmentElement!!),
        )
    }

    fun testFragmentElementContainsNestedSteps() {
        val file = createFragmentFile("test", """
            fragment: test-fragment
            given step one
            when step two
            then step three
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        val fragmentElement = PsiTreeUtil.findChildOfType(psiFile, BerryCrushFragmentElement::class.java)
        assertNotNull("Should find fragment element", fragmentElement)

        // Verify fragment contains nested step elements
        val nestedSteps = PsiTreeUtil.findChildrenOfType(fragmentElement, com.berrycrush.intellij.psi.BerryCrushStepElement::class.java)
        assertEquals("Fragment should contain 3 nested steps", 3, nestedSteps.size)
    }

    fun testTargetElementEvaluatorFindsFragmentElement() {
        val file = createFragmentFile("test", """
            fragment: test-fragment
            given step one
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        // Find a leaf element inside the fragment (e.g., the first element)
        val leafElement = psiFile!!.findElementAt(10) // Position within "fragment:"
        assertNotNull("Should find element at offset", leafElement)

        val evaluator = BerryCrushTargetElementEvaluator()
        val namedElement = evaluator.getNamedElement(leafElement!!)
        
        assertNotNull("TargetElementEvaluator should find fragment element", namedElement)
        assertTrue("Named element should be a BerryCrushFragmentElement", namedElement is BerryCrushFragmentElement)
        assertEquals("test-fragment", (namedElement as BerryCrushFragmentElement).fragmentName)
    }

    fun testTargetElementEvaluatorAcceptsFragmentAsNamedParent() {
        val file = createFragmentFile("test", """
            fragment: test-fragment
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        val fragmentElement = PsiTreeUtil.findChildOfType(psiFile, BerryCrushFragmentElement::class.java)
        assertNotNull(fragmentElement)

        val evaluator = BerryCrushTargetElementEvaluator()
        assertTrue(
            "TargetElementEvaluator should accept fragment as named parent",
            evaluator.isAcceptableNamedParent(fragmentElement!!),
        )
    }

    fun testFindConflictsReturnsConflictsWhenUsagesExist() {
        // Create fragment
        val fragmentFile = createFragmentFile("conflict", """
            fragment: conflict-fragment
            given setup step
        """.trimIndent())

        // Create scenario that includes the fragment
        createScenarioFile("user", """
            scenario: User scenario
            include conflict-fragment
        """.trimIndent())

        val psiFile = psiManager.findFile(fragmentFile)
        assertNotNull(psiFile)

        val processor = BerryCrushSafeDeleteProcessor()
        val conflicts = MultiMap<com.intellij.psi.PsiElement, String>()
        processor.findConflicts(psiFile!!, arrayOf(psiFile), emptyArray(), conflicts)

        assertFalse("Should find conflicts when fragment has usages", conflicts.isEmpty)
        val messages = conflicts.values()
        assertTrue(
            "Conflict message should mention fragment name",
            messages.any { it.contains("conflict-fragment") },
        )
    }

    fun testFindConflictsReturnsNullWhenNoUsages() {
        val file = createFragmentFile("nousage", """
            fragment: no-usage-fragment
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        val processor = BerryCrushSafeDeleteProcessor()
        val conflicts = MultiMap<com.intellij.psi.PsiElement, String>()
        processor.findConflicts(psiFile!!, arrayOf(psiFile), emptyArray(), conflicts)

        assertTrue("Should return empty conflicts when no usages exist", conflicts.isEmpty)
    }
}

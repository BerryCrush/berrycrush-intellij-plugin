package com.berrycrush.intellij.structure

import com.berrycrush.intellij.BerryCrushTestCase
import com.berrycrush.intellij.psi.BerryCrushFile
import com.berrycrush.intellij.psi.BerryCrushFragmentElement
import com.berrycrush.intellij.psi.BerryCrushScenarioElement
import com.berrycrush.intellij.psi.BerryCrushStepElement
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.psi.util.PsiTreeUtil

/**
 * Tests for BerryCrush structure view factory.
 * Verifies the Structure tool window displays correct hierarchy.
 */
class BerryCrushStructureViewFactoryTest : BerryCrushTestCase() {

    // ========== Factory Tests ==========

    fun testFactoryReturnsBuilderForBerryCrushFile() {
        val file = createScenarioFile("factory", """
            scenario: Test
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)
        assertTrue("Should be BerryCrushFile", psiFile is BerryCrushFile)

        val factory = BerryCrushStructureViewFactory()
        val builder = factory.getStructureViewBuilder(psiFile!!)
        assertNotNull("Should return structure view builder for BerryCrush file", builder)
    }

    // ========== File Level Tests ==========

    fun testFileElementShowsScenarios() {
        val file = createScenarioFile("scenarios", """
            scenario: First Scenario
            given first step

            scenario: Second Scenario
            given second step
        """.trimIndent())

        val psiFile = psiManager.findFile(file) as? BerryCrushFile
        assertNotNull(psiFile)

        val element = BerryCrushStructureViewElement(psiFile!!)
        val children = element.children

        // Should have 2 scenario children
        assertEquals("File should have 2 scenario children", 2, children.size)
    }

    fun testFileElementShowsFragments() {
        val file = createFragmentFile("fragments", """
            fragment: first-fragment
            given first step

            fragment: second-fragment
            given second step
        """.trimIndent())

        val psiFile = psiManager.findFile(file) as? BerryCrushFile
        assertNotNull(psiFile)

        val element = BerryCrushStructureViewElement(psiFile!!)
        val children = element.children

        // Should have 2 fragment children
        assertEquals("File should have 2 fragment children", 2, children.size)
    }

    // ========== Fragment Hierarchy Tests ==========

    fun testFragmentShowsNestedSteps() {
        val file = createFragmentFile("nestedSteps", """
            fragment: my-fragment
            given step one
            when step two
            then step three
        """.trimIndent())

        val psiFile = psiManager.findFile(file) as? BerryCrushFile
        assertNotNull(psiFile)

        val fragment = PsiTreeUtil.findChildOfType(psiFile, BerryCrushFragmentElement::class.java)
        assertNotNull(fragment)

        val element = BerryCrushStructureViewElement(fragment!!)
        val children = element.children

        // Fragment should show 3 steps as children
        assertEquals("Fragment should have 3 step children", 3, children.size)
    }

    // ========== Scenario Hierarchy Tests ==========

    fun testScenarioShowsSiblingSteps() {
        val file = createScenarioFile("siblingSteps", """
            scenario: My Scenario
            given step one
            when step two
            then step three
        """.trimIndent())

        val psiFile = psiManager.findFile(file) as? BerryCrushFile
        assertNotNull(psiFile)

        val scenario = PsiTreeUtil.findChildOfType(psiFile, BerryCrushScenarioElement::class.java)
        assertNotNull(scenario)

        val element = BerryCrushStructureViewElement(scenario!!)
        val children = element.children

        // Scenario should show steps as children
        assertTrue("Scenario should have step children", children.isNotEmpty())
    }

    // ========== Presentation Tests ==========

    fun testScenarioPresentationText() {
        val file = createScenarioFile("presentation", """
            scenario: My Test Scenario
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file) as? BerryCrushFile
        val scenario = PsiTreeUtil.findChildOfType(psiFile, BerryCrushScenarioElement::class.java)
        assertNotNull(scenario)

        val element = BerryCrushStructureViewElement(scenario!!)
        val presentation = element.presentation
        val text = presentation.presentableText

        assertTrue(
            "Presentation should contain 'scenario: My Test Scenario', got: $text",
            text?.contains("scenario") == true && text.contains("My Test Scenario")
        )
    }

    fun testFragmentPresentationText() {
        val file = createFragmentFile("presentation", """
            fragment: my-fragment
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file) as? BerryCrushFile
        val fragment = PsiTreeUtil.findChildOfType(psiFile, BerryCrushFragmentElement::class.java)
        assertNotNull(fragment)

        val element = BerryCrushStructureViewElement(fragment!!)
        val presentation = element.presentation
        val text = presentation.presentableText

        assertTrue(
            "Presentation should contain 'fragment: my-fragment', got: $text",
            text?.contains("fragment") == true && text.contains("my-fragment")
        )
    }

    fun testStepPresentationText() {
        val file = createFragmentFile("stepPresentation", """
            fragment: test
            given a precondition is met
        """.trimIndent())

        val psiFile = psiManager.findFile(file) as? BerryCrushFile
        val step = PsiTreeUtil.findChildOfType(psiFile, BerryCrushStepElement::class.java)
        assertNotNull(step)

        val element = BerryCrushStructureViewElement(step!!)
        val presentation = element.presentation
        val text = presentation.presentableText

        assertTrue(
            "Presentation should contain step info, got: $text",
            text?.contains("given") == true || text?.contains("precondition") == true
        )
    }

    // ========== Icon Tests ==========

    fun testScenarioHasIcon() {
        val file = createScenarioFile("icon", """
            scenario: Test
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file) as? BerryCrushFile
        val scenario = PsiTreeUtil.findChildOfType(psiFile, BerryCrushScenarioElement::class.java)
        assertNotNull(scenario)

        val element = BerryCrushStructureViewElement(scenario!!)
        val presentation = element.presentation
        val icon = presentation.getIcon(false)

        assertNotNull("Scenario should have an icon", icon)
    }

    fun testFragmentHasIcon() {
        val file = createFragmentFile("icon", """
            fragment: test
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file) as? BerryCrushFile
        val fragment = PsiTreeUtil.findChildOfType(psiFile, BerryCrushFragmentElement::class.java)
        assertNotNull(fragment)

        val element = BerryCrushStructureViewElement(fragment!!)
        val presentation = element.presentation
        val icon = presentation.getIcon(false)

        assertNotNull("Fragment should have an icon", icon)
    }

    // ========== Navigation Tests ==========

    fun testElementIsNavigable() {
        val file = createScenarioFile("nav", """
            scenario: Test
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file) as? BerryCrushFile
        val scenario = PsiTreeUtil.findChildOfType(psiFile, BerryCrushScenarioElement::class.java)
        assertNotNull(scenario)

        val element = BerryCrushStructureViewElement(scenario!!)
        
        // Should be navigable
        assertTrue("Element should be navigable", element.canNavigate())
    }

    // ========== Value Tests ==========

    fun testGetValueReturnsPsiElement() {
        val file = createScenarioFile("value", """
            scenario: Test
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file) as? BerryCrushFile
        val scenario = PsiTreeUtil.findChildOfType(psiFile, BerryCrushScenarioElement::class.java)
        assertNotNull(scenario)

        val element = BerryCrushStructureViewElement(scenario!!)
        val value = element.value

        assertSame("getValue should return the PSI element", scenario, value)
    }
}

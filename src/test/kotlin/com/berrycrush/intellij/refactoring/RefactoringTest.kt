package com.berrycrush.intellij.refactoring

import com.berrycrush.intellij.BerryCrushTestCase
import com.berrycrush.intellij.refactoring.fragment.FragmentRenameProcessor
import com.berrycrush.intellij.refactoring.variable.VariableRenameProcessor

/**
 * Tests for BerryCrush refactoring support.
 */
class RefactoringTest : BerryCrushTestCase() {

    fun testRefactoringSupportProviderDetectsFragmentDefinition() {
        val file = createFragmentFile("test", """
            fragment: my-fragment
            given step one
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        val provider = BerryCrushRefactoringSupportProvider()
        // Find element on the fragment definition line
        val element = psiFile!!.findElementAt(10) // Inside "fragment: my-fragment"
        assertNotNull(element)

        // Provider should allow rename
        assertTrue(provider.isInplaceRenameAvailable(element!!, null))
    }

    fun testRefactoringSupportProviderDetectsIncludeDirective() {
        val file = createScenarioFile("test", """
            scenario: Test
            include my-fragment
            then done
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        val provider = BerryCrushRefactoringSupportProvider()
        val element = psiFile!!.findElementAt(24) // Inside "include my-fragment"
        assertNotNull(element)

        assertTrue(provider.isInplaceRenameAvailable(element!!, null))
    }

    fun testRefactoringSupportProviderDetectsVariablePlaceholder() {
        val file = createScenarioFile("test", """
            scenario: Test
            given step with {{myVar}}
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        val provider = BerryCrushRefactoringSupportProvider()
        val element = psiFile!!.findElementAt(35) // Inside "{{myVar}}"
        assertNotNull(element)

        assertTrue(provider.isInplaceRenameAvailable(element!!, null))
    }

    fun testFragmentRenameProcessorCanProcessFragmentDefinition() {
        val file = createFragmentFile("test", """
            fragment: my-fragment
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        val processor = FragmentRenameProcessor()
        val element = psiFile!!.findElementAt(10)
        assertNotNull(element)

        assertTrue(processor.canProcessElement(element!!))
    }

    fun testFragmentRenameProcessorCanProcessIncludeDirective() {
        val file = createScenarioFile("test", """
            scenario: Test
            include my-fragment
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        val processor = FragmentRenameProcessor()
        val element = psiFile!!.findElementAt(24)
        assertNotNull(element)

        assertTrue(processor.canProcessElement(element!!))
    }

    fun testVariableRenameProcessorCanProcessVariableUsage() {
        val file = createScenarioFile("test", """
            scenario: Test
            given step with {{petId}}
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        val processor = VariableRenameProcessor()
        val element = psiFile!!.findElementAt(35) // Inside {{petId}}
        assertNotNull(element)

        assertTrue(processor.canProcessElement(element!!))
    }

    fun testVariableRenameProcessorCanProcessVariableDefinition() {
        val file = createScenarioFile("test", """
            scenario: Test
            extract $.id => petId
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        val processor = VariableRenameProcessor()
        val element = psiFile!!.findElementAt(30) // Near "=> petId"
        assertNotNull(element)

        assertTrue(processor.canProcessElement(element!!))
    }
}

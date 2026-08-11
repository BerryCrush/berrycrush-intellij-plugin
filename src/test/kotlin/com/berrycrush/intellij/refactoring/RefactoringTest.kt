package com.berrycrush.intellij.refactoring

import com.berrycrush.intellij.BerryCrushTestCase
import com.berrycrush.intellij.psi.BerryCrushFragmentElement
import com.berrycrush.intellij.psi.BerryCrushFragmentRefElement
import com.berrycrush.intellij.psi.BerryCrushVariableRefElement
import com.berrycrush.intellij.refactoring.variable.VariableRenameProcessor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.utils.vfs.getPsiFile

/**
 * Generic tests for refactoring support and processor eligibility.
 */
class RefactoringTest : BerryCrushTestCase() {
    private fun elementInside(fileText: String, needle: String, shift: Int = 0): Int {
        val index = fileText.indexOf(needle)
        assertTrue("Expected to find '$needle' in fixture", index >= 0)
        return index + shift
    }

    fun testRenameOnIncludeDirective() {
        val file =
            createFragmentFile(
                "test",
                """
                fragment: my-fragment
                  given step one
                """.trimIndent(),
            )

        val scenario = """
            scenario: my-scenario
              given step one
                include <caret>my-fragment
        """.trimIndent()
        myFixture.configureByText("test.scenario", scenario)
        val offset = myFixture.editor.caretModel.offset
        val leaf = myFixture.file.findElementAt(offset)
        assertNotNull(leaf)
        val e = leaf?.parent
        assertTrue(e is BerryCrushFragmentRefElement)
        val newName = "superb-fragment"
        myFixture.renameElementAtCaret(newName)

        val fragment = PsiTreeUtil.findChildOfType(file.getPsiFile(project), BerryCrushFragmentElement::class.java)
        assertEquals(newName, fragment?.fragmentName)

        val leaf2 = myFixture.file.findElementAt(offset)
        assertNotNull(leaf2)
        val e2 = leaf2?.parent
        assertTrue(e2 is BerryCrushFragmentRefElement)
        assertEquals(newName, e2?.text)
    }

    fun testRefactoringSupportProviderDetectsFragmentDefinition() {
        val file =
            createFragmentFile(
                "test",
                """
                fragment: my-fragment
                  given step one
                """.trimIndent(),
            )

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        val provider = BerryCrushRefactoringSupportProvider()
        val element = psiFile!!.findElementAt(elementInside(psiFile.text, "my-fragment", 2))
        assertNotNull(element)

        assertTrue(provider.isInplaceRenameAvailable(element!!, null))
    }

    fun testRefactoringSupportProviderDetectsIncludeDirective() {
        val file =
            createScenarioFile(
                "test",
                """
                scenario: Test
                                    include my-fragment
                  then done
                """.trimIndent(),
            )

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        val provider = BerryCrushRefactoringSupportProvider()
        val element = psiFile!!.findElementAt(elementInside(psiFile.text, "my-fragment", 2))
        assertNotNull(element)

        assertTrue(provider.isInplaceRenameAvailable(element!!, null))
    }

    fun testRefactoringSupportProviderDetectsVariablePlaceholder() {
        val file =
            createScenarioFile(
                "test",
                """
                scenario: Test
                                    extract $.id => myVar
                                    given step with {{myVar}}
                """.trimIndent(),
            )

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        val provider = BerryCrushRefactoringSupportProvider()
        val element = psiFile!!.findElementAt(elementInside(psiFile.text, "{{myVar}}", 3))
        assertNotNull(element)

        assertTrue(provider.isInplaceRenameAvailable(element!!, null))
    }

    fun testVariableRenameProcessorCanProcessExtractVariableUsage() {
        val file =
            createScenarioFile(
                "test",
                """
                scenario: Test
                                    given capture id
                                        extract $.id => petId
                  given step with {{petId}}
                """.trimIndent(),
            )

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        val processor = VariableRenameProcessor()
        val element = PsiTreeUtil.findChildrenOfType(psiFile, BerryCrushVariableRefElement::class.java)
            .firstOrNull { it.text.contains("petId") }
        assertNotNull(element)

        assertTrue(processor.canProcessElement(element!!))
    }

    fun testVariableRenameProcessorCanProcessParameterDefinition() {
        val file =
            createScenarioFile(
                "test",
                """
                scenario: Test
                  parameters:
                    petId: 123
                  when use {{param.petId}}
                """.trimIndent(),
            )

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        val processor = VariableRenameProcessor()
        val element = psiFile!!.findElementAt(elementInside(psiFile.text, "petId", 1))
        assertNotNull(element)

        assertTrue(processor.canProcessElement(element!!))
    }
}

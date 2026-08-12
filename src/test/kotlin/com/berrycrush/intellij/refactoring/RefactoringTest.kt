package com.berrycrush.intellij.refactoring

import com.berrycrush.intellij.BerryCrushTestCase
import com.berrycrush.intellij.psi.BerryCrushExampleHeaderElement
import com.berrycrush.intellij.psi.BerryCrushExtractElement
import com.berrycrush.intellij.psi.BerryCrushFragmentElement
import com.berrycrush.intellij.psi.BerryCrushFragmentRefElement
import com.berrycrush.intellij.psi.BerryCrushVariableRefElement
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

    fun testRenameExtractVariable() {
        val scenario = """
            scenario: foo
              given extract id
                extract $.id => id
              when I call
                call ^operationId
                  id: {{id<caret>}}
        """.trimIndent()
        val file = myFixture.configureByText("test.scenario", scenario)
        val newName = "petId"
        myFixture.renameElementAtCaret(newName)
        val extract = PsiTreeUtil.findChildOfType(file, BerryCrushExtractElement::class.java)
        assertNotNull(extract)
        assertEquals(newName, extract!!.name)
        val ref = PsiTreeUtil.findChildOfType(file, BerryCrushVariableRefElement::class.java)
        assertNotNull(ref)
        assertEquals(newName, ref!!.name)
    }

    fun testRenameOutlineHeader() {
        val scenario = """
            outline: foo
              when I call
                call ^operationId
                  id: {{id<caret>}}
              examples:
                | id |
                |  1 |
        """.trimIndent()
        val file = myFixture.configureByText("test.scenario", scenario)
        val newName = "petId"
        myFixture.renameElementAtCaret(newName)
        val header = PsiTreeUtil.findChildOfType(file, BerryCrushExampleHeaderElement::class.java)
        assertNotNull(header)
        assertEquals(newName, header!!.name)
        val ref = PsiTreeUtil.findChildOfType(file, BerryCrushVariableRefElement::class.java)
        assertNotNull(ref)
        assertEquals(newName, ref!!.name)
    }

    fun testRenameFragmentOnIncludeDirective() {
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
        createFragmentFile("my-fragment",
            """
                fragment: my-fragment
                  given step one
                    call ^operationId
            """.trimIndent())
        val file =
            createScenarioFile(
                "test",
                """
                scenario: Test
                  then done
                    include my-fragment
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
                  given step with {{myVar}}
                    extract $.id => myVar
                """.trimIndent(),
            )

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        val provider = BerryCrushRefactoringSupportProvider()
        val element = psiFile!!.findElementAt(elementInside(psiFile.text, "{{myVar}}", 3))
        assertNotNull(element)

        assertTrue(provider.isInplaceRenameAvailable(element!!, null))
    }
}

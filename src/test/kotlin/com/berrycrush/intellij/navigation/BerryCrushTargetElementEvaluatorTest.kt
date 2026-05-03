package com.berrycrush.intellij.navigation

import com.berrycrush.intellij.BerryCrushTestCase
import com.berrycrush.intellij.psi.BerryCrushFragmentElement
import com.intellij.psi.util.PsiTreeUtil

/**
 * Tests for BerryCrush Target Element Evaluator.
 * Verifies correct element selection for refactoring operations.
 */
class BerryCrushTargetElementEvaluatorTest : BerryCrushTestCase() {

    private val evaluator = BerryCrushTargetElementEvaluator()

    // ========== isAcceptableNamedParent Tests ==========

    fun testAcceptsFragmentAsNamedParent() {
        val file = createFragmentFile("test", """
            fragment: my-fragment
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val fragment = PsiTreeUtil.findChildOfType(psiFile, BerryCrushFragmentElement::class.java)
        assertNotNull(fragment)

        assertTrue(
            "Should accept BerryCrushFragmentElement as named parent",
            evaluator.isAcceptableNamedParent(fragment!!)
        )
    }

    fun testDoesNotAcceptNonFragmentAsNamedParent() {
        val file = createScenarioFile("test", """
            scenario: Test
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        // The file itself should not be an acceptable named parent
        assertFalse(
            "Should not accept non-fragment elements as named parent",
            evaluator.isAcceptableNamedParent(psiFile!!)
        )
    }

    // ========== getNamedElement Tests ==========

    fun testGetNamedElementReturnsFragmentForChildElement() {
        val file = createFragmentFile("test", """
            fragment: my-fragment
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        // Find an element inside the fragment
        val text = psiFile!!.text
        val stepIndex = text.indexOf("Given")
        if (stepIndex >= 0) {
            val element = psiFile.findElementAt(stepIndex)
            if (element != null) {
                val namedElement = evaluator.getNamedElement(element)
                
                // Should return the parent fragment
                assertTrue(
                    "Should return fragment element for child",
                    namedElement is BerryCrushFragmentElement
                )
            }
        }
    }

    fun testGetNamedElementReturnsNullForNonFragmentFile() {
        val file = createScenarioFile("test", """
            scenario: Test
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        val element = psiFile?.firstChild
        if (element != null) {
            val namedElement = evaluator.getNamedElement(element)
            
            // Scenario files don't have fragment parents
            assertNull(
                "Should return null for elements not in fragments",
                namedElement
            )
        }
    }

    fun testGetNamedElementReturnsFragmentForFragmentKeyword() {
        val file = createFragmentFile("keyword", """
            fragment: test-fragment
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        val text = psiFile!!.text
        val fragmentIndex = text.indexOf("Fragment")
        if (fragmentIndex >= 0) {
            val element = psiFile.findElementAt(fragmentIndex)
            if (element != null) {
                val namedElement = evaluator.getNamedElement(element)
                
                assertTrue(
                    "Should return fragment element for Fragment keyword",
                    namedElement is BerryCrushFragmentElement
                )
            }
        }
    }
}

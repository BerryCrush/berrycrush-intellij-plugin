package com.berrycrush.intellij.parser

import com.berrycrush.intellij.BerryCrushTestCase
import com.berrycrush.intellij.psi.BerryCrushFragmentElement
import com.berrycrush.intellij.psi.BerryCrushStepElement
import com.intellij.psi.util.PsiTreeUtil

/**
 * Tests for BerryCrush parser - verifies PSI tree structure.
 */
class BerryCrushParserTest : BerryCrushTestCase() {

    fun testFragmentContainsNestedSteps() {
        val file = createFragmentFile("test", """
            Fragment: my-fragment
            Given step one
            When step two
            Then step three
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull("PSI file should be created", psiFile)

        // Find fragment element
        val fragments = PsiTreeUtil.findChildrenOfType(psiFile, BerryCrushFragmentElement::class.java)
        assertEquals("Should find 1 fragment", 1, fragments.size)

        val fragment = fragments.first()
        assertEquals("my-fragment", fragment.fragmentName)

        // Find ALL steps in file (should be nested in fragment)
        val allSteps = PsiTreeUtil.findChildrenOfType(psiFile, BerryCrushStepElement::class.java)
        assertEquals("File should contain 3 steps", 3, allSteps.size)

        // Find steps nested in fragment
        val nestedSteps = PsiTreeUtil.findChildrenOfType(fragment, BerryCrushStepElement::class.java)
        assertEquals("Fragment should contain 3 nested steps", 3, nestedSteps.size)
    }

    fun testMultipleFragmentsAreSeparate() {
        val file = createFragmentFile("multi", """
            Fragment: first
            Given first step

            Fragment: second
            When second step
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        val fragments = PsiTreeUtil.findChildrenOfType(psiFile, BerryCrushFragmentElement::class.java)
        assertEquals("Should find 2 fragments", 2, fragments.size)

        val firstFragment = fragments.find { it.fragmentName == "first" }
        val secondFragment = fragments.find { it.fragmentName == "second" }

        assertNotNull("First fragment should exist", firstFragment)
        assertNotNull("Second fragment should exist", secondFragment)

        // Steps should be in correct fragments
        val firstSteps = PsiTreeUtil.findChildrenOfType(firstFragment, BerryCrushStepElement::class.java)
        val secondSteps = PsiTreeUtil.findChildrenOfType(secondFragment, BerryCrushStepElement::class.java)

        assertEquals("First fragment should have 1 step", 1, firstSteps.size)
        assertEquals("Second fragment should have 1 step", 1, secondSteps.size)
    }
}

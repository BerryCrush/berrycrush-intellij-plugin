package com.berrycrush.intellij.reference

import com.berrycrush.intellij.BerryCrushTestCase
import com.berrycrush.intellij.psi.BerryCrushAssertElement
import com.intellij.openapi.util.TextRange
import com.intellij.psi.util.PsiTreeUtil

/**
 * Tests for BerryCrush Assertion Reference.
 * Verifies reference resolution for @Assertion annotated methods.
 */
class BerryCrushAssertionReferenceTest : BerryCrushTestCase() {

    // ========== Reference Creation Tests ==========

    fun testReferenceCanBeCreated() {
        val file = createScenarioFile("assert", """
            scenario: Test
            assert response.status == 200
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val assert = PsiTreeUtil.findChildOfType(psiFile, BerryCrushAssertElement::class.java)
        assertNotNull(assert)

        // Create a reference
        val reference = BerryCrushAssertionReference(
            assert!!,
            TextRange(0, assert.textLength),
            "response.status == 200"
        )
        assertNotNull(reference)
    }

    fun testReferenceResolveReturnsNullWithoutAnnotatedMethods() {
        val file = createScenarioFile("resolve", """
            scenario: Test
            assert response.status == 200
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val assert = PsiTreeUtil.findChildOfType(psiFile, BerryCrushAssertElement::class.java)
        assertNotNull(assert)

        val reference = BerryCrushAssertionReference(
            assert!!,
            TextRange(0, assert.textLength),
            "response.status == 200"
        )

        // No @Assertion methods exist in project, so resolve should return null
        val resolved = reference.resolve()
        assertNull("Should return null when no @Assertion methods exist", resolved)
    }

    fun testMultiResolveReturnsEmptyWithoutAnnotatedMethods() {
        val file = createScenarioFile("multiResolve", """
            scenario: Test
            assert response.status == 200
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val assert = PsiTreeUtil.findChildOfType(psiFile, BerryCrushAssertElement::class.java)
        assertNotNull(assert)

        val reference = BerryCrushAssertionReference(
            assert!!,
            TextRange(0, assert.textLength),
            "response.status == 200"
        )

        val results = reference.multiResolve(false)
        assertTrue("Should return empty array when no @Assertion methods exist", results.isEmpty())
    }

    // ========== Companion Object Method Tests ==========

    fun testFindMatchingAssertionMethodsWithoutAnnotations() {
        val methods = BerryCrushAssertionReference.findMatchingAssertionMethods(
            project,
            "response.status == 200"
        )
        assertTrue("Should return empty list when no @Assertion methods", methods.isEmpty())
    }

    fun testGetAllAssertionMethodsWithoutAnnotations() {
        val methods = BerryCrushAssertionReference.getAllAssertionMethods(project)
        assertTrue("Should return empty list when no @Assertion methods", methods.isEmpty())
    }

    // ========== Variants Tests ==========

    fun testGetVariantsReturnsEmptyWithoutAnnotatedMethods() {
        val file = createScenarioFile("variants", """
            scenario: Test
            assert response.status == 200
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val assert = PsiTreeUtil.findChildOfType(psiFile, BerryCrushAssertElement::class.java)
        assertNotNull(assert)

        val reference = BerryCrushAssertionReference(
            assert!!,
            TextRange(0, assert.textLength),
            "response.status"
        )

        val variants = reference.variants
        assertTrue("Should return empty variants when no @Assertion methods", variants.isEmpty())
    }

    // ========== Reference Range Tests ==========

    fun testReferenceRangeIsCorrect() {
        val file = createScenarioFile("range", """
            scenario: Test
            assert response.status == 200
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val assert = PsiTreeUtil.findChildOfType(psiFile, BerryCrushAssertElement::class.java)
        assertNotNull(assert)

        val range = TextRange(7, 30) // "response.status == 200"
        val reference = BerryCrushAssertionReference(
            assert!!,
            range,
            "response.status == 200"
        )

        assertEquals(range, reference.rangeInElement)
    }
}

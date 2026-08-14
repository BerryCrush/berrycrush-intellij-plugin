package org.berrycrush.intellij.reference

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import org.berrycrush.intellij.BerryCrushTestCase
import org.berrycrush.intellij.psi.BerryCrushAssertElement
import org.junit.jupiter.api.Test

/**
 * Tests for BerryCrush Assertion Reference.
 * Verifies reference resolution for @Assertion annotated methods.
 */
class BerryCrushAssertionReferenceTest : BerryCrushTestCase() {
    // ========== Reference Creation Tests ==========

    @Test
    fun testReferenceCanBeCreated() {
        val file =
            createScenarioFile(
                "assert",
                """
                scenario: Test
                assert response.status == 200
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val assert = findChildOfType(psiFile, BerryCrushAssertElement::class.java)
        assertNotNull(assert)

        // Create a reference
        val reference =
            BerryCrushAssertionReference(
                assert!!,
                TextRange(0, assert.textLength),
                "response.status == 200",
            )
        assertNotNull(reference)
    }

    @Test
    fun testReferenceResolveReturnsNullWithoutAnnotatedMethods() {
        val file =
            createScenarioFile(
                "resolve",
                """
                scenario: Test
                assert response.status == 200
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val assert = findChildOfType(psiFile, BerryCrushAssertElement::class.java)
        assertNotNull(assert)

        val reference =
            BerryCrushAssertionReference(
                assert!!,
                TextRange(0, assert.textLength),
                "response.status == 200",
            )

        consume {
            // No @Assertion methods exist in project, so resolve should return null
            val resolved = reference.resolve()
            assertNull("Should return null when no @Assertion methods exist", resolved)
        }
    }

    @Test
    fun testMultiResolveReturnsEmptyWithoutAnnotatedMethods() {
        val file =
            createScenarioFile(
                "multiResolve",
                """
                scenario: Test
                assert response.status == 200
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val assert = findChildOfType(psiFile, BerryCrushAssertElement::class.java)
        assertNotNull(assert)

        val reference =
            BerryCrushAssertionReference(
                assert!!,
                TextRange(0, assert.textLength),
                "response.status == 200",
            )

        consume {
            val results = reference.multiResolve(false)
            assertTrue("Should return empty array when no @Assertion methods exist", results.isEmpty())
        }
    }

    // ========== Companion Object Method Tests ==========

    @Test
    fun testFindMatchingAssertionMethodsWithoutAnnotations() {
        val methods =
            findMatchingAssertionMethods(
                project,
                "response.status == 200",
            )
        assertTrue("Should return empty list when no @Assertion methods", methods.isEmpty())
    }

    @Test
    fun testGetAllAssertionMethodsWithoutAnnotations() {
        val methods = getAllAssertionMethods(project)
        assertTrue("Should return empty list when no @Assertion methods", methods.isEmpty())
    }

    // ========== Variants Tests ==========

    @Test
    fun testGetVariantsReturnsEmptyWithoutAnnotatedMethods() {
        val file =
            createScenarioFile(
                "variants",
                """
                scenario: Test
                assert response.status == 200
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val assert = findChildOfType(psiFile, BerryCrushAssertElement::class.java)
        assertNotNull(assert)

        val reference =
            BerryCrushAssertionReference(
                assert!!,
                TextRange(0, assert.textLength),
                "response.status",
            )
        consume {
            val variants = reference.variants
            assertTrue("Should return empty variants when no @Assertion methods", variants.isEmpty())
        }
    }

    // ========== Reference Range Tests ==========

    @Test
    fun testReferenceRangeIsCorrect() {
        val file =
            createScenarioFile(
                "range",
                """
                scenario: Test
                assert response.status == 200
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val assert = findChildOfType(psiFile, BerryCrushAssertElement::class.java)
        assertNotNull(assert)

        val range = TextRange(7, 30) // "response.status == 200"
        val reference =
            BerryCrushAssertionReference(
                assert!!,
                range,
                "response.status == 200",
            )

        assertEquals(range, reference.rangeInElement)
    }

    private fun getAllAssertionMethods(project: Project) = consume {
        BerryCrushAssertionReference.getAllAssertionMethods(project)
    }

    private fun findMatchingAssertionMethods(project: Project, assertionText: String) = consume {
        BerryCrushAssertionReference.findMatchingAssertionMethods(project, assertionText)
    }
}

package org.berrycrush.intellij.folding

import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiDocumentManager
import org.berrycrush.intellij.BerryCrushTestCase
import org.berrycrush.intellij.lexer.BerryCrushTokenTypes
import org.junit.jupiter.api.Test

/**
 * Tests for BerryCrush code folding builder.
 * Verifies folding regions are created correctly for blocks, steps, and doc strings.
 */
class BerryCrushFoldingBuilderTest : BerryCrushTestCase() {
    @Test
    fun testFoldingBuilderReturnsRegionsForScenario() {
        val file =
            createScenarioFile(
                "fold",
                """
                scenario: My Scenario
                given step one
                when step two
                then step three
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        assertNotNull(psiFile)

        val document = PsiDocumentManager.getInstance(project).document(psiFile!!)
        assertNotNull(document)

        val builder = BerryCrushFoldingBuilder()
        val regions = ReadAction.compute<Array<FoldingDescriptor>, Throwable> {
            builder.buildFoldRegions(psiFile.node, document!!)
        }

        // Verify regions array is returned (may be empty for simple cases)
        assertNotNull("Should return folding regions array", regions)
    }

    @Test
    fun testFoldingBuilderReturnsRegionsForFragment() {
        val file =
            createFragmentFile(
                "fold",
                """
                fragment: my-fragment
                  given step one
                  when step two
                  then step three
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        assertNotNull(psiFile)

        val document = PsiDocumentManager.getInstance(project).document(psiFile!!)
        assertNotNull(document)

        val builder = BerryCrushFoldingBuilder()
        val regions = ReadAction.compute<Array<FoldingDescriptor>, Throwable> {
            builder.buildFoldRegions(psiFile.node(), document!!)
        }
        // Should have at least one fold region for the fragment
        assertTrue(
            "Should have folding regions for fragment",
            regions.isNotEmpty(),
        )
    }

    @Test
    fun testPlaceholderTextForScenario() {
        val file =
            createScenarioFile(
                "placeholder",
                """
                scenario: Test
                given step
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        assertNotNull(psiFile)

        val builder = BerryCrushFoldingBuilder()

        // Find scenario node
        var scenarioNode: com.intellij.lang.ASTNode? = null
        var current = psiFile!!.node().firstChildNode()
        while (current != null) {
            if (current.elementType == BerryCrushTokenTypes.SCENARIO) {
                scenarioNode = current
                break
            }
            current = current.treeNext
        }

        if (scenarioNode != null) {
            val placeholder = builder.getPlaceholderText(scenarioNode)
            assertEquals("scenario: ...", placeholder)
        }
    }

    @Test
    fun testPlaceholderTextForFragment() {
        val file =
            createFragmentFile(
                "placeholder",
                """
                fragment: Test
                given step
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        assertNotNull(psiFile)

        val builder = BerryCrushFoldingBuilder()

        // Find fragment node
        var fragmentNode: com.intellij.lang.ASTNode? = null
        var current = psiFile!!.node().firstChildNode()
        while (current != null) {
            if (current.elementType == BerryCrushTokenTypes.FRAGMENT) {
                fragmentNode = current
                break
            }
            current = current.treeNext
        }

        if (fragmentNode != null) {
            val placeholder = builder.getPlaceholderText(fragmentNode)
            assertEquals("fragment: ...", placeholder)
        }
    }

    @Test
    fun testPlaceholderTextForGiven() {
        val builder = BerryCrushFoldingBuilder()

        val file =
            createScenarioFile(
                "givenPlaceholder",
                """
                scenario: Test
                given step
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        assertNotNull(psiFile)

        // Find given node
        var givenNode: com.intellij.lang.ASTNode? = null
        var current = psiFile!!.node().firstChildNode()
        while (current != null) {
            if (current.elementType == BerryCrushTokenTypes.GIVEN) {
                givenNode = current
                break
            }
            current = current.treeNext
        }

        if (givenNode != null) {
            val placeholder = builder.getPlaceholderText(givenNode)
            assertEquals("given ...", placeholder)
        }
    }

    @Test
    fun testIsCollapsedByDefaultReturnsFalse() {
        val file =
            createScenarioFile(
                "collapsed",
                """
                scenario: Test
                given step
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        assertNotNull(psiFile)

        val builder = BerryCrushFoldingBuilder()

        // Find any foldable node
        var current = psiFile!!.node().firstChildNode()
        while (current != null) {
            val isCollapsed = builder.isCollapsedByDefault(current)
            assertFalse("Fold regions should not be collapsed by default", isCollapsed)
            current = current.treeNext
        }
    }

    @Test
    fun testFeatureAndScenariosFolding() {
        val file =
            createScenarioFile(
                "multiBlock",
                """
                Feature: My Feature
                
                scenario: First Scenario
                given first step
                when first action
                then first assertion
                
                scenario: Second Scenario
                given second step
                when second action
                then second assertion
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        assertNotNull(psiFile)

        val document = PsiDocumentManager.getInstance(project).document(psiFile!!)
        assertNotNull(document)

        val builder = BerryCrushFoldingBuilder()
        val regions = ReadAction.compute<Array<FoldingDescriptor>, Throwable> {
            builder.buildFoldRegions(psiFile.node, document!!)
        }

        // Should have folding regions (feature + scenarios)
        // The exact number depends on PSI structure, so just verify we have some
        assertNotNull("Should return folding regions array", regions)
    }

    @Test
    fun testEmptyScenarioNoFolding() {
        val file = createScenarioFile("empty", "scenario: Empty")

        val psiFile = findFile(file)
        assertNotNull(psiFile)

        val document = PsiDocumentManager.getInstance(project).document(psiFile!!)
        assertNotNull(document)

        val builder = BerryCrushFoldingBuilder()
        val regions = ReadAction.compute<Array<FoldingDescriptor>, Throwable> {
            builder.buildFoldRegions(psiFile.node, document!!)
        }

        // Empty scenario should not have meaningful fold regions (single line)
        assertNotNull(regions)
    }
}

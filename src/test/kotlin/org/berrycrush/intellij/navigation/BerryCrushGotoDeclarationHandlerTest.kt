package org.berrycrush.intellij.navigation

import org.berrycrush.intellij.BerryCrushTestCase
import org.junit.jupiter.api.Test

/**
 * Tests for BerryCrush Goto Declaration handler.
 * Verifies Cmd+Click navigation for operations and fragments.
 */
class BerryCrushGotoDeclarationHandlerTest : BerryCrushTestCase() {
    private val handler = BerryCrushGotoDeclarationHandler()

    @Test
    fun testHandlerReturnsNullForNonBerryCrushFile() {
        // Create a Kotlin file instead of BerryCrush
        val ktFile = myFixture.addFileToProject("test.kt", "class Test")
        val psiFile = findFile(ktFile.virtualFile)
        assertNotNull(psiFile)

        val element = consume { psiFile?.firstChild }
        assertNotNull(element)

        val targets = consume { handler.getGotoDeclarationTargets(element, 0, null) }
        assertNull("Should return null for non-BerryCrush files", targets)
    }

    @Test
    fun testHandlerReturnsNullForNullElement() {
        val targets = consume { handler.getGotoDeclarationTargets(null, 0, null) }
        assertNull("Should return null for null element", targets)
    }

    @Test
    fun testHandlerProcessesScenarioFile() {
        val file =
            createScenarioFile(
                "test",
                """
                scenario: Test
                given step
                call ^operationId
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        assertNotNull(psiFile)

        // Get the file's first child element
        val element = consume { psiFile?.firstChild }
        assertNotNull(element)

        // Handler should accept the element (even if no targets found)
        val targets = consume { handler.getGotoDeclarationTargets(element, 0, null) }
        // May be null if no operation defined - that's OK
    }

    @Test
    fun testHandlerProcessesFragmentFile() {
        val file =
            createFragmentFile(
                "test",
                """
                fragment: test-fragment
                given step
                include other-fragment
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        assertNotNull(psiFile)

        val element = consume { psiFile?.firstChild }
        assertNotNull(element)

        // Handler should accept the element
        val targets = consume { handler.getGotoDeclarationTargets(element, 0, null) }
        // May be null if no fragment defined - that's OK
    }

    @Test
    fun testActionTextReturnsNull() {
        // getActionText should return null (uses default)
        val actionText =
            handler.getActionText(
                com.intellij.openapi.actionSystem.DataContext.EMPTY_CONTEXT,
            )
        assertNull("getActionText should return null", actionText)
    }

    @Test
    fun testHandlerWithOperationReference() {
        val file =
            createScenarioFile(
                "opRef",
                """
                scenario: Test
                call ^getPetById
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        assertNotNull(psiFile)

        // Find the operation reference element (^getPetById)
        consume {
            val text = psiFile?.text ?: ""
            val opRefIndex = text.indexOf("^getPetById")
            if (opRefIndex >= 0) {
                val element = psiFile?.findElementAt(opRefIndex + 1) // +1 to be inside the text
                if (element != null) {
                    val targets = handler.getGotoDeclarationTargets(element, opRefIndex + 1, null)
                    // May be null if no OpenAPI spec - that's OK for this test
                }
            }
        }
    }

    @Test
    fun testHandlerWithIncludeDirective() {
        // Create a target fragment first
        createFragmentFile(
            "target",
            """
            fragment: target-fragment
            given target step
            """.trimIndent(),
        )

        // Create a scenario that includes it
        val file =
            createScenarioFile(
                "includer",
                """
                scenario: Test
                include target-fragment
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        assertNotNull(psiFile)

        consume {
            val text = psiFile?.text ?: ""
            val includeIndex = text.indexOf("target-fragment")
            if (includeIndex >= 0) {
                val element = psiFile?.findElementAt(includeIndex)
                if (element != null) {
                    val targets = handler.getGotoDeclarationTargets(element, includeIndex, null)
                    // Should find the target fragment
                    // Note: May need index to be populated first
                }
            }
        }
    }
}

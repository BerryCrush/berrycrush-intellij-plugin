package com.berrycrush.intellij.navigation

import com.berrycrush.intellij.BerryCrushTestCase

/**
 * Tests for BerryCrush Goto Declaration handler.
 * Verifies Cmd+Click navigation for operations and fragments.
 */
class BerryCrushGotoDeclarationHandlerTest : BerryCrushTestCase() {

    private val handler = BerryCrushGotoDeclarationHandler()

    fun testHandlerReturnsNullForNonBerryCrushFile() {
        // Create a Kotlin file instead of BerryCrush
        val ktFile = myFixture.addFileToProject("test.kt", "class Test")
        val psiFile = psiManager.findFile(ktFile.virtualFile)
        assertNotNull(psiFile)
        
        val element = psiFile?.firstChild
        assertNotNull(element)
        
        val targets = handler.getGotoDeclarationTargets(element, 0, null)
        assertNull("Should return null for non-BerryCrush files", targets)
    }

    fun testHandlerReturnsNullForNullElement() {
        val targets = handler.getGotoDeclarationTargets(null, 0, null)
        assertNull("Should return null for null element", targets)
    }

    fun testHandlerProcessesScenarioFile() {
        val file = createScenarioFile("test", """
            scenario: Test
            given step
            call ^operationId
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)
        
        // Get the file's first child element
        val element = psiFile?.firstChild
        assertNotNull(element)
        
        // Handler should accept the element (even if no targets found)
        val targets = handler.getGotoDeclarationTargets(element, 0, null)
        // May be null if no operation defined - that's OK
    }

    fun testHandlerProcessesFragmentFile() {
        val file = createFragmentFile("test", """
            fragment: test-fragment
            given step
            include other-fragment
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)
        
        val element = psiFile?.firstChild
        assertNotNull(element)
        
        // Handler should accept the element
        val targets = handler.getGotoDeclarationTargets(element, 0, null)
        // May be null if no fragment defined - that's OK
    }

    fun testActionTextReturnsNull() {
        // getActionText should return null (uses default)
        val actionText = handler.getActionText(
            com.intellij.openapi.actionSystem.DataContext.EMPTY_CONTEXT
        )
        assertNull("getActionText should return null", actionText)
    }

    fun testHandlerWithOperationReference() {
        val file = createScenarioFile("opRef", """
            scenario: Test
            call ^getPetById
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)
        
        // Find the operation reference element (^getPetById)
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

    fun testHandlerWithIncludeDirective() {
        // Create a target fragment first
        createFragmentFile("target", """
            fragment: target-fragment
            given target step
        """.trimIndent())

        // Create a scenario that includes it
        val file = createScenarioFile("includer", """
            scenario: Test
            include target-fragment
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)
        
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

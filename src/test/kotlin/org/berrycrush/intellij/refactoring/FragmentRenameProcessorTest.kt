package org.berrycrush.intellij.refactoring

import org.berrycrush.intellij.BerryCrushTestCase
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FragmentRenameProcessorTest : BerryCrushTestCase() {
    fun testRenameFragmentFromDefinitionUpdatesAllReferences() {
        val definitionFile = createFragmentFile(
            "login",
            """
            fragment: login-<caret>steps
              given authenticate user
            """.trimIndent(),
        )

        val scenarioFile = createScenarioFile(
            "flow",
            """
            scenario: login flow
              include login-steps
              include login-steps
            """.trimIndent(),
        )

        val fragmentFile = createFragmentFile(
            "reuse",
            """
            fragment: helper-fragment
              include login-steps
            """.trimIndent(),
        )

        myFixture.configureFromExistingVirtualFile(definitionFile)
        myFixture.renameElementAtCaret("auth-steps")

        val definitionText = psiManager.findFile(definitionFile)?.text
        assertTrue(definitionText?.contains("fragment: auth-steps") == true)
        assertTrue(definitionText?.contains("fragment: login-steps") == false)

        val scenarioText = psiManager.findFile(scenarioFile)?.text
        val fragmentText = psiManager.findFile(fragmentFile)?.text

        assertEquals(2, scenarioText?.split("include auth-steps")?.size?.minus(1))
        assertTrue(fragmentText?.contains("include auth-steps") == true)
        assertTrue(fragmentText?.contains("include login-steps") == false)
    }

    fun testRenameFragmentFromReferenceUpdatesDefinitionAndReferences() {
        val definitionFile = createFragmentFile(
            "shared",
            """
            fragment: login-steps
              given authenticate user
            """.trimIndent(),
        )

        val secondaryFile = createScenarioFile(
            "secondary",
            """
            scenario: secondary flow
              include login-steps
            """.trimIndent(),
        )

        myFixture.configureByText(
            "primary.scenario",
            """
            scenario: primary flow
              given invoke shared flow
                include login-<caret>steps
            """.trimIndent(),
        )

        myFixture.renameElementAtCaret("auth-steps")

        val definitionText = psiManager.findFile(definitionFile)?.text
        val secondaryText = psiManager.findFile(secondaryFile)?.text

        assertTrue(myFixture.file.text.contains("include auth-steps"))
        assertTrue(!myFixture.file.text.contains("include login-steps"))
        assertTrue(secondaryText?.contains("include auth-steps") == true)
        assertTrue(definitionText?.contains("fragment: auth-steps") == true)
        assertTrue(definitionText?.contains("fragment: login-steps") == false)
    }
}

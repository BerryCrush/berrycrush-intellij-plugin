package com.berrycrush.intellij.refactoring.variable

import com.berrycrush.intellij.BerryCrushTestCase
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Behavior-focused rename tests for variable and parameter PSI rename support.
 */
class VariableRenameProcessorTest : BerryCrushTestCase() {
    fun testRenameExtractVariableFromDefinitionUpdatesAllReferences() {
        myFixture.configureByText(
            "extract-definition.scenario",
            """
            scenario: extraction
              given capture id
                extract $.id => pet<caret>Id
              when using {{petId}}
              and using {{petId}}
            """.trimIndent(),
        )

        myFixture.renameElementAtCaret("userId")

        val text = myFixture.file.text
        assertTrue(text.contains("extract $.id => userId"), text)
        assertEquals(2, text.split("{{userId}}").size - 1)
        assertTrue(!text.contains("{{petId}}"))
    }

    fun testRenameExtractVariableFromReferenceUpdatesDefinitionAndReferences() {
        myFixture.configureByText(
            "extract-reference.scenario",
            """
            scenario: extraction
              given capture id
                extract $.id => petId
              when using {{pet<caret>Id}}
              and using {{petId}}
            """.trimIndent(),
        )

        myFixture.renameElementAtCaret("userId")

        val text = myFixture.file.text
        assertTrue(text.contains("extract $.id => userId"), text)
        assertEquals(2, text.split("{{userId}}").size - 1)
        assertTrue(!text.contains("{{petId}}"))
    }

    fun testRenameParameterFromDefinitionUpdatesReferences() {
        myFixture.configureByText(
            "parameter-definition.scenario",
            """
            scenario: parameter usage
              parameters:
                pet<caret>Id: 123
              when using {{param.petId}}
              and using {{param.petId}}
            """.trimIndent(),
        )

        myFixture.renameElementAtCaret("userId")

        val text = myFixture.file.text
        assertTrue(text.contains("userId: 123"), text)
        assertEquals(2, text.split("{{param.userId}}").size - 1, text)
        assertTrue(!text.contains("{{param.petId}}"))
    }

    fun testRenameParameterFromReferenceUpdatesDefinitionAndReferences() {
        myFixture.configureByText(
            "parameter-reference.scenario",
            """
            scenario: parameter usage
              parameters:
                petId: 123
              when using {{param.pet<caret>Id}}
              and using {{param.petId}}
            """.trimIndent(),
        )

        myFixture.renameElementAtCaret("userId")

        val text = myFixture.file.text
        assertTrue(text.contains("userId: 123"), text)
        assertEquals(2, text.split("{{param.userId}}").size - 1, text)
        assertTrue(!text.contains("{{param.petId}}"))
    }

    fun testRenameExampleHeaderFromDefinitionUpdatesVariableReferences() {
        myFixture.configureByText(
            "outline-header-definition.scenario",
            """
            outline: rename header
              when use {{petId}}
              and use {{petId}}
              examples:
                | pet<caret>Id |
                | 123 |
            """.trimIndent(),
        )

        myFixture.renameElementAtCaret("userId")

        val text = myFixture.file.text
        assertTrue(text.contains("| userId |"), text)
        assertEquals(2, text.split("{{userId}}").size - 1)
        assertTrue(!text.contains("{{petId}}"))
    }

    fun testRenameExampleHeaderFromReferenceUpdatesHeaderAndReferences() {
        myFixture.configureByText(
            "outline-header-reference.scenario",
            """
            outline: rename header
              when use {{pet<caret>Id}}
              and use {{petId}}
              examples:
                | petId |
                | 123 |
            """.trimIndent(),
        )

        myFixture.renameElementAtCaret("userId")

        val text = myFixture.file.text
        assertTrue(text.contains("| userId |"), text)
        assertEquals(2, text.split("{{userId}}").size - 1)
        assertTrue(!text.contains("{{petId}}"))
    }

    fun testRenameExtractVariableFromInterpolatedQuotedStringReference() {
        myFixture.configureByText(
            "extract-string-reference.scenario",
            """
            scenario: extraction
              given capture id
                extract $.id => petId
              when using
                call ^operationId
                  message: "value {{pet<caret>Id}} and {{petId}}"
            """.trimIndent(),
        )

        myFixture.renameElementAtCaret("userId")

        val text = myFixture.file.text
        assertTrue(text.contains("extract $.id => userId"), text)
        assertTrue(text.contains("\"value {{userId}} and {{userId}}\""), text)
        assertTrue(!text.contains("{{petId}}"), text)
    }

    fun testRenameParameterFromInterpolatedQuotedStringReference() {
        myFixture.configureByText(
            "param-string-reference.scenario",
            """
            scenario: parameter usage
              parameters:
                petId: 123
              when using
                call ^operationId
                  message: "value {{param.pet<caret>Id}} and {{param.petId}}"
            """.trimIndent(),
        )

        myFixture.renameElementAtCaret("userId")

        val text = myFixture.file.text
        assertTrue(text.contains("userId: 123"), text)
        assertTrue(text.contains("\"value {{param.userId}} and {{param.userId}}\""), text)
        assertTrue(!text.contains("{{param.petId}}"), text)
    }
}

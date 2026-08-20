package org.berrycrush.intellij.formatting

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.codeStyle.CodeStyleManager
import org.berrycrush.intellij.BerryCrushTestCase

abstract class BerryCrushFormattingTestCase : BerryCrushTestCase() {
    protected fun applyFormatting(
        input: String,
        fileExtension: String = "scenario",
    ): String {
        myFixture.configureByText("test.$fileExtension", input)

        WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager
                .getInstance(project)
                .reformatText(myFixture.file, 0, myFixture.editor.document.textLength)
        }

        return myFixture.editor.document.text
    }

    /**
     * Helper function to test formatting.
     * Configures file with input, runs reformat, and checks result.
     */
    protected fun doFormattingTest(
        input: String,
        expected: String,
        fileExtension: String = "scenario",
    ) {
        val actual = applyFormatting(input, fileExtension)

        // Debug output for test failures
        if (actual != expected) {
            println("=== INPUT ===")
            println(input.replace(" ", "·"))
            println("=== EXPECTED ===")
            println(expected.replace(" ", "·"))
            println("=== ACTUAL ===")
            println(actual.replace(" ", "·"))
            println("=== END ===")
        }

        assertEquals(
            "Formatting result mismatch",
            expected,
            actual,
        )
    }

    protected fun doIdempotencyTest(
        input: String,
        expected: String,
        fileExtension: String = "scenario",
    ) {
        val firstPass = applyFormatting(input, fileExtension)
        assertEquals("First pass result mismatch", expected, firstPass)

        val secondPass = applyFormatting(firstPass, fileExtension)
        assertEquals("Formatting must be idempotent", firstPass, secondPass)
    }
}

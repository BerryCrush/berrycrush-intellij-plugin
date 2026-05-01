package com.berrycrush.intellij.inspection

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.awt.datatransfer.StringSelection

/**
 * Quick fix to create an @Assertion annotated method template.
 *
 * Copies a method skeleton to the clipboard that the user can paste
 * into their assertion definition class.
 */
class CreateAssertionQuickFix(
    private val assertionText: String
) : LocalQuickFix {

    override fun getName(): String = "Copy @Assertion method template to clipboard"

    override fun getFamilyName(): String = "BerryCrush"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val template = generateAssertionTemplate()

        // Copy to clipboard
        CopyPasteManager.getInstance().setContents(StringSelection(template))

        Messages.showInfoMessage(
            project,
            "@Assertion method template copied to clipboard.\n\nPaste it into your assertion definition class.",
            "Assertion Template Created"
        )
    }

    private fun generateAssertionTemplate(): String {
        val methodName = generateMethodName()
        val pattern = escapePattern(assertionText)

        return """
    @Assertion("$pattern")
    public void $methodName(Object actual) {
        // TODO: Implement assertion
    }
""".trimIndent()
    }

    /**
     * Generate a camelCase method name from the assertion text.
     */
    private fun generateMethodName(): String {
        return "assert" + assertionText
            .replace(Regex("""["'].*?["']"""), " Value ")  // Replace quoted strings
            .replace(Regex("""\d+"""), " Number ")         // Replace numbers
            .replace(Regex("""[^a-zA-Z0-9]+"""), " ")      // Non-alphanumeric to space
            .trim()
            .split(Regex("""\s+"""))
            .filter { it.isNotBlank() }
            .joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
            .take(50)
            .ifEmpty { "Condition" }
    }

    /**
     * Convert assertion text to a pattern with placeholders.
     */
    private fun escapePattern(text: String): String {
        return text
            .replace(Regex(""""[^"]*""""), """{string}""") // Replace quoted strings
            .replace(Regex("""'[^']*'"""), """{string}""")
            .replace(Regex("""\b\d+\b"""), """{int}""")    // Replace numbers
    }
}

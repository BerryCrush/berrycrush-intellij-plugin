package com.berrycrush.intellij.inspection

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import java.awt.datatransfer.StringSelection

/**
 * Quick fix to create a @Step annotated method template.
 *
 * Copies a method skeleton to the clipboard that the user can paste
 * into their step definition class.
 */
class CreateStepQuickFix(
    private val stepText: String
) : LocalQuickFix {

    override fun getName(): String = "Copy @Step method template to clipboard"

    override fun getFamilyName(): String = "BerryCrush"

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val template = generateStepTemplate()

        // Copy to clipboard
        CopyPasteManager.getInstance().setContents(StringSelection(template))

        Messages.showInfoMessage(
            project,
            "@Step method template copied to clipboard.\n\nPaste it into your step definition class.",
            "Step Template Created"
        )
    }

    private fun generateStepTemplate(): String {
        val methodName = generateMethodName()
        val pattern = escapePattern(stepText)

        return """
    @Step("$pattern")
    public void $methodName() {
        // TODO: Implement step
    }
""".trimIndent()
    }

    /**
     * Generate a camelCase method name from the step text.
     */
    private fun generateMethodName(): String {
        return stepText
            .replace(Regex("""["'].*?["']"""), " value ")  // Replace quoted strings
            .replace(Regex("""\d+"""), " number ")         // Replace numbers
            .replace(Regex("""[^a-zA-Z0-9]+"""), " ")      // Non-alphanumeric to space
            .trim()
            .split(Regex("""\s+"""))
            .filter { it.isNotBlank() }
            .mapIndexed { index, word ->
                if (index == 0) word.lowercase()
                else word.replaceFirstChar { it.uppercase() }
            }
            .joinToString("")
            .take(50)
            .ifEmpty { "step" }
    }

    /**
     * Convert step text to a pattern with placeholders.
     */
    private fun escapePattern(text: String): String {
        return text
            .replace(Regex(""""[^"]*""""), """{string}""") // Replace quoted strings
            .replace(Regex("""'[^']*'"""), """{string}""")
            .replace(Regex("""\b\d+\b"""), """{int}""")    // Replace numbers
    }
}

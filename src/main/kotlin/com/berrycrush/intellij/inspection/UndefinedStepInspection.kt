package com.berrycrush.intellij.inspection

import com.berrycrush.intellij.reference.BerryCrushStepReference
import com.berrycrush.intellij.util.ModuleScopeResolver
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiFile

/**
 * Inspection that detects undefined custom step references.
 *
 * Highlights steps that don't have directives (call/assert/extract/include)
 * and don't match any @Step annotated method in the project.
 */
class UndefinedStepInspection : BerryCrushInspection() {

    override fun getDisplayName(): String = "Undefined custom step"
    override fun getShortName(): String = "BerryCrushUndefinedStep"
    override fun getGroupDisplayName(): String = "BerryCrush"
    override fun isEnabledByDefault(): Boolean = true

    override fun checkFile(file: PsiFile, holder: ProblemsHolder) {
        val project = file.project
        val scope = ModuleScopeResolver.getModuleDependencyScope(file)
        val lines = file.text.lines()

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val stepMatch = STEP_PATTERN.find(line)

            if (stepMatch != null) {
                val stepText = extractStepText(stepMatch.groupValues[2])

                // Check if next line has a directive
                val hasDirective = i + 1 < lines.size && DIRECTIVE_PATTERN.matches(lines[i + 1].trim())

                if (!hasDirective && stepText.isNotBlank()) {
                    // Check if step matches any @Step annotation
                    val matchingMethods = BerryCrushStepReference.findMatchingStepMethodsInScope(
                        project,
                        stepText,
                        scope
                    )

                    if (matchingMethods.isEmpty()) {
                        // Find element at step keyword position
                        findElementAtLine(file, i, stepMatch.range.first)?.let { element ->
                            holder.registerProblem(
                                element,
                                "Step '$stepText' has no matching @Step definition",
                                ProblemHighlightType.WEAK_WARNING,
                                CreateStepQuickFix(stepText)
                            )
                        }
                    }
                }
            }
            i++
        }
    }

    /**
     * Extract step text from the matched group.
     * Handles "given: something" and "given something" formats.
     */
    private fun extractStepText(text: String): String {
        val trimmed = text.trim()
        return if (trimmed.startsWith(":")) {
            trimmed.removePrefix(":").trim()
        } else {
            trimmed
        }
    }

    companion object {
        // Matches: given/when/then/and/but followed by text (with optional : suffix)
        private val STEP_PATTERN = Regex(
            """^\s*(given|when|then|and|but):?\s*(.*)$""",
            RegexOption.IGNORE_CASE
        )

        // Matches directive lines: call, assert, extract, include
        private val DIRECTIVE_PATTERN = Regex(
            """^(call|assert|extract|include)\s+.*""",
            RegexOption.IGNORE_CASE
        )
    }
}

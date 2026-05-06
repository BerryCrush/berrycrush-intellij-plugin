package com.berrycrush.intellij.inspection

import com.berrycrush.intellij.reference.BerryCrushAssertionReference
import com.berrycrush.intellij.util.ModuleScopeResolver
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiFile

/**
 * Inspection that detects undefined assertion references.
 *
 * Highlights `assert` directives that don't match any @Assertion
 * annotated method in the project.
 */
class UndefinedAssertionInspection : BerryCrushInspection() {

    override fun getDisplayName(): String = "Undefined assertion"
    override fun getShortName(): String = "BerryCrushUndefinedAssertion"
    override fun getGroupDisplayName(): String = "BerryCrush"
    override fun isEnabledByDefault(): Boolean = true

    override fun checkFile(file: PsiFile, holder: ProblemsHolder) {
        val project = file.project
        val scope = ModuleScopeResolver.getModuleDependencyScope(file)
        val lines = file.text.lines()

        lines.forEachIndexed { lineIndex, line ->
            ASSERT_PATTERN.find(line)?.let { match ->
                val assertionText = match.groupValues[1].trim()

                if (assertionText.isNotBlank() && !isBuiltInAssertion(assertionText)) {
                    val matchingMethods = BerryCrushAssertionReference.findMatchingAssertionMethodsInScope(
                        project,
                        assertionText,
                        scope
                    )

                    if (matchingMethods.isEmpty()) {
                        findElementAtLine(file, lineIndex, match.range.first)?.let { element ->
                            holder.registerProblem(
                                element,
                                "Assertion '$assertionText' has no matching @Assertion definition",
                                ProblemHighlightType.WEAK_WARNING,
                                CreateAssertionQuickFix(assertionText)
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Check if the assertion text matches a built-in assertion pattern.
     */
    private fun isBuiltInAssertion(text: String): Boolean {
        return BUILT_IN_PATTERNS.any { it.matches(text) }
    }

    companion object {
        // Matches: assert <assertion text>
        private val ASSERT_PATTERN = Regex(
            """^\s*assert\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )

        // Built-in assertion patterns (from scenario-syntax.rst)
        private val BUILT_IN_PATTERNS = listOf(
            // status <code> or statusCode <code>
            Regex("""^status(Code)?\s+(\d+|\dxx)$""", RegexOption.IGNORE_CASE),
            // contains "<text>" or not contains "<text>"
            Regex("""^(not\s+)?contains\s+.+$""", RegexOption.IGNORE_CASE),
            // JSONPath assertions: $.path equals/=/matches/notEmpty/size/exists/not equals/not exists
            Regex("""^\$[^\s]+\s+(equals|=|matches|notEmpty|size|exists)\s*.*$""", RegexOption.IGNORE_CASE),
            Regex("""^\$[^\s]+\s+not\s+(equals|exists)\s*.*$""", RegexOption.IGNORE_CASE),
            // header <name> or header <name> = "<value>" or header <name>: "<value>"
            Regex("""^header\s+\S+.*$""", RegexOption.IGNORE_CASE),
            // responseTime <ms>
            Regex("""^responseTime\s+\d+$""", RegexOption.IGNORE_CASE),
            // schema (with optional path)
            Regex("""^schema(\s+.+)?$""", RegexOption.IGNORE_CASE),
        )
    }
}

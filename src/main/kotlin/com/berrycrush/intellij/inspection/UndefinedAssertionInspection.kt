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

                if (assertionText.isNotBlank()) {
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

    companion object {
        // Matches: assert <assertion text>
        private val ASSERT_PATTERN = Regex(
            """^\s*assert\s+(.+)$""",
            RegexOption.IGNORE_CASE
        )
    }
}

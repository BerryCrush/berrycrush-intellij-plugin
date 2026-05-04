package com.berrycrush.intellij.inspection

import com.berrycrush.intellij.reference.BerryCrushFragmentReference
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiFile

/**
 * Inspection that detects unused parameters in parameterized fragment includes.
 *
 * Warns when a parameter is passed to a fragment but the fragment doesn't
 * use that parameter (no {{paramName}} reference found in the fragment).
 */
class UnusedParameterInspection : BerryCrushInspection() {

    override fun getDisplayName(): String = "Unused fragment parameter"
    override fun getShortName(): String = "BerryCrushUnusedParameter"
    override fun getGroupDisplayName(): String = "BerryCrush"
    override fun isEnabledByDefault(): Boolean = true

    override fun checkFile(file: PsiFile, holder: ProblemsHolder) {
        val text = file.text
        val lines = text.lines()

        var lineIndex = 0
        while (lineIndex < lines.size) {
            val line = lines[lineIndex]
            val includeMatch = INCLUDE_PATTERN.find(line)

            if (includeMatch != null) {
                val fragmentName = includeMatch.groupValues[1]
                val includeIndent = countIndent(line)

                // Collect parameters from following indented lines
                val parameters = mutableListOf<Pair<String, Int>>() // paramName to lineIndex
                var paramLineIndex = lineIndex + 1

                while (paramLineIndex < lines.size) {
                    val paramLine = lines[paramLineIndex]
                    val paramIndent = countIndent(paramLine)

                    // Stop if we hit a line with same or less indentation
                    if (paramLine.isNotBlank() && paramIndent <= includeIndent) {
                        break
                    }

                    // Parse parameter
                    val paramMatch = PARAM_PATTERN.find(paramLine)
                    if (paramMatch != null) {
                        parameters.add(paramMatch.groupValues[1] to paramLineIndex)
                    }

                    paramLineIndex++
                }

                // Check if any parameters are unused
                if (parameters.isNotEmpty()) {
                    checkUnusedParameters(file, fragmentName, parameters, holder)
                }

                lineIndex = paramLineIndex
            } else {
                lineIndex++
            }
        }
    }

    private fun checkUnusedParameters(
        file: PsiFile,
        fragmentName: String,
        parameters: List<Pair<String, Int>>,
        holder: ProblemsHolder
    ) {
        val project = file.project
        val fragmentFile = BerryCrushFragmentReference.findFragmentByName(project, fragmentName)
            as? PsiFile ?: return

        val fragmentText = fragmentFile.text
        val usedVariables = VARIABLE_PATTERN.findAll(fragmentText)
            .map { it.groupValues[1] }
            .toSet()

        parameters.forEach { (paramName, lineIndex) ->
            if (paramName !in usedVariables) {
                findElementAtLine(file, lineIndex, 0)?.let { element ->
                    holder.registerProblem(
                        element,
                        "Parameter '$paramName' is not used in fragment '$fragmentName'",
                        ProblemHighlightType.WEAK_WARNING
                    )
                }
            }
        }
    }

    private fun countIndent(line: String): Int {
        var count = 0
        for (c in line) {
            if (c == ' ') count++ else break
        }
        return count
    }

    companion object {
        private val INCLUDE_PATTERN = Regex("""^\s*include\s+(\S+)""")
        private val PARAM_PATTERN = Regex("""^\s+(\w+)\s*:""")
        private val VARIABLE_PATTERN = Regex("""\{\{(\w+)\}\}""")
    }
}

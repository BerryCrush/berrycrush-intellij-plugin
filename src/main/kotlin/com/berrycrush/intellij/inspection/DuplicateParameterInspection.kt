package com.berrycrush.intellij.inspection

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiFile

/**
 * Inspection that detects duplicate parameters in parameterized fragment includes.
 *
 * Reports an error when the same parameter name is specified multiple times
 * in a single include directive.
 */
class DuplicateParameterInspection : BerryCrushInspection() {

    override fun getDisplayName(): String = "Duplicate fragment parameter"
    override fun getShortName(): String = "BerryCrushDuplicateParameter"
    override fun getGroupDisplayName(): String = "BerryCrush"
    override fun isEnabledByDefault(): Boolean = true

    override fun checkFile(file: PsiFile, holder: ProblemsHolder) {
        val lines = file.text.lines()

        var lineIndex = 0
        while (lineIndex < lines.size) {
            val line = lines[lineIndex]
            val includeMatch = INCLUDE_PATTERN.find(line)

            if (includeMatch != null) {
                val includeIndent = countIndent(line)
                val seenParams = mutableMapOf<String, Int>() // paramName -> first occurrence line

                // Check following indented parameter lines
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
                        val paramName = paramMatch.groupValues[1]

                        if (paramName in seenParams) {
                            // Duplicate found
                            findElementAtLine(file, paramLineIndex, paramMatch.range.first)?.let { element ->
                                holder.registerProblem(
                                    element,
                                    "Duplicate parameter '$paramName' (first defined on line ${seenParams[paramName]!! + 1})",
                                    ProblemHighlightType.ERROR
                                )
                            }
                        } else {
                            seenParams[paramName] = paramLineIndex
                        }
                    }

                    paramLineIndex++
                }

                lineIndex = paramLineIndex
            } else {
                lineIndex++
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
    }
}

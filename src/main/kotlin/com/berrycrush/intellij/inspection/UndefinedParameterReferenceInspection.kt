package com.berrycrush.intellij.inspection

import com.berrycrush.intellij.psi.BerryCrushParameterEntryElement
import com.berrycrush.intellij.psi.BerryCrushVariableInterpolationElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

/**
 * Inspection that detects undefined parameter references.
 *
 * Highlights `${param.name}` references where the parameter name
 * doesn't exist in any parameters block in the file.
 */
class UndefinedParameterReferenceInspection : BerryCrushInspection() {

    override fun getDisplayName(): String = "Undefined parameter reference"
    override fun getShortName(): String = "BerryCrushUndefinedParameterReference"
    override fun getGroupDisplayName(): String = "BerryCrush"
    override fun isEnabledByDefault(): Boolean = true

    override fun checkFile(file: PsiFile, holder: ProblemsHolder) {
        // Find all defined parameter names
        val definedParameters = PsiTreeUtil.findChildrenOfType(file, BerryCrushParameterEntryElement::class.java)
            .mapNotNull { it.parameterName }
            .toSet()

        // Find all extracted variables (for context variable validation)
        val extractedVariables = findExtractedVariables(file)

        // Check all variable interpolation elements
        val lines = file.text.lines()

        lines.forEachIndexed { lineIndex, line ->
            // Check for ${param.name} references
            PARAM_REF_PATTERN.findAll(line).forEach { match ->
                val paramName = match.groupValues[1]
                if (paramName !in definedParameters) {
                    findElementAtLine(file, lineIndex, match.range.first)?.let { element ->
                        holder.registerProblem(
                            element,
                            "Parameter '$paramName' is not defined in any parameters block",
                            ProblemHighlightType.ERROR
                        )
                    }
                }
            }

            // Check for ${context.varName} or ${varName} references (shorthand)
            CONTEXT_REF_PATTERN.findAll(line).forEach { match ->
                val varName = match.groupValues[1]
                if (varName !in extractedVariables) {
                    findElementAtLine(file, lineIndex, match.range.first)?.let { element ->
                        holder.registerProblem(
                            element,
                            "Variable '$varName' is not extracted in this scenario",
                            ProblemHighlightType.WARNING
                        )
                    }
                }
            }

            // Check shorthand variable references
            SHORTHAND_REF_PATTERN.findAll(line).forEach { match ->
                val varName = match.groupValues[1]
                // Skip if it's a known prefix
                if (varName !in listOf("env", "context", "param") && varName !in extractedVariables) {
                    findElementAtLine(file, lineIndex, match.range.first)?.let { element ->
                        holder.registerProblem(
                            element,
                            "Variable '$varName' is not extracted in this scenario",
                            ProblemHighlightType.WARNING
                        )
                    }
                }
            }
        }
    }

    /**
     * Find all extracted variable names from extract directives.
     */
    private fun findExtractedVariables(file: PsiFile): Set<String> {
        val text = file.text
        val pattern = Regex("""extract\s+(\w+)\s*=""")
        return pattern.findAll(text)
            .map { it.groupValues[1] }
            .toSet()
    }

    companion object {
        private val PARAM_REF_PATTERN = Regex("""\$\{param\.(\w+)}""")
        private val CONTEXT_REF_PATTERN = Regex("""\$\{context\.(\w+)}""")
        // Shorthand pattern: ${varName} where varName is NOT followed by a dot
        private val SHORTHAND_REF_PATTERN = Regex("""\$\{(\w+)}(?!\.)""")
    }
}

package com.berrycrush.intellij.inspection

import com.berrycrush.intellij.psi.BerryCrushIncludeElement
import com.berrycrush.intellij.psi.BerryCrushIncludeParameterElement
import com.berrycrush.intellij.reference.BerryCrushFragmentReference
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

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

    override fun checkFile(
        file: PsiFile,
        holder: ProblemsHolder,
    ) {
        PsiTreeUtil.findChildrenOfType(file, BerryCrushIncludeElement::class.java).forEach { include ->
            include.fragmentName?.let { fragmentName ->
                include.parameters?.let { parameters ->
                    checkUnusedParameters(file, fragmentName, parameters, holder)
                }
            }
        }
    }

    private fun checkUnusedParameters(
        file: PsiFile,
        fragmentName: String,
        parameters: BerryCrushIncludeParameterElement,
        holder: ProblemsHolder,
    ) {
        val project = file.project
        val fragmentFile =
            BerryCrushFragmentReference.findFragmentByName(project, fragmentName)
                as? PsiFile ?: return

        val fragmentText = fragmentFile.text
        val usedVariables =
            VARIABLE_PATTERN
                .findAll(fragmentText)
                .map { it.groupValues[1] }
                .toSet()

        parameters.entries.forEach { element ->
            val paramName = element.parameterName
            if (paramName !in usedVariables) {
                holder.registerProblem(
                    element,
                    "Parameter '$paramName' is not used in fragment '$fragmentName'",
                    ProblemHighlightType.WEAK_WARNING,
                )
            }
        }
    }

    companion object {
        private val VARIABLE_PATTERN = Regex("""\{\{(\w+)\}\}""")
    }
}

package com.berrycrush.intellij.inspection

import com.berrycrush.intellij.psi.BerryCrushBlockElement
import com.berrycrush.intellij.psi.BerryCrushParametersElement
import com.berrycrush.intellij.psi.BerryCrushVariableRefElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.util.PsiTreeUtil

/**
 * Inspection that detects undefined parameter references.
 *
 * Highlights `{{param.name}}` references where the parameter name
 * doesn't exist in any parameters block in the file.
 */
class UndefinedParameterReferenceInspection : BerryCrushInspection() {
    override fun getDisplayName(): String = "Undefined parameter reference"

    override fun getShortName(): String = "BerryCrushUndefinedParameterReference"

    override fun getGroupDisplayName(): String = "BerryCrush"

    override fun isEnabledByDefault(): Boolean = true

    override fun checkFile(
        file: PsiFile,
        holder: ProblemsHolder,
    ) {
        checkParameters(file, emptyList(), emptyList(), holder)
    }

    private fun checkParameters(
        element: PsiElement,
        parentParameters: List<BerryCrushParametersElement>,
        extractedVariables: List<PsiNamedElement>,
        holder: ProblemsHolder,
    ) {
        element.children.forEach { element ->
            when (element) {
                is BerryCrushBlockElement -> {
                    val parameters = element.children.filterIsInstance<BerryCrushParametersElement>()
                    val extracted: Collection<PsiNamedElement> =
                        PsiTreeUtil.findChildrenOfType(
                            element,
                            PsiNamedElement::class.java,
                        )
                    checkParameters(element, parameters + parentParameters, extractedVariables + extracted, holder)
                }
                else -> checkElement(element, parentParameters, extractedVariables, holder)
            }
        }
    }

    private fun checkElement(
        element: PsiElement,
        parentParameters: List<BerryCrushParametersElement>,
        extractedVariables: List<PsiNamedElement>,
        holder: ProblemsHolder,
    ) {
        PsiTreeUtil.findChildrenOfAnyType(element, true, BerryCrushVariableRefElement::class.java).forEach { variable ->
            val name = variable.variableName

            if (name.startsWith("param.")) {
                val result = parentParameters.find { name in it.parameterNames }
                if (result == null) {
                    holder.registerProblem(
                        variable,
                        "Parameter '$name' is not defined in any parameters block",
                        ProblemHighlightType.WARNING,
                    )
                }
            } else {
                val result = extractedVariables.find { name == it.name }
                if (result == null) {
                    holder.registerProblem(
                        variable,
                        "Variable '$name' is not extracted in this block",
                        ProblemHighlightType.WARNING,
                    )
                }
            }
        }
    }
}

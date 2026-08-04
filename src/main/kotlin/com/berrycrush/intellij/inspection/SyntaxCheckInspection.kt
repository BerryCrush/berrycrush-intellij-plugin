package com.berrycrush.intellij.inspection

import com.berrycrush.intellij.psi.BerryCrushFeatureElement
import com.berrycrush.intellij.psi.BerryCrushParameterEntryElement
import com.berrycrush.intellij.psi.BerryCrushParametersElement
import com.berrycrush.intellij.psi.BerryCrushScenarioElement
import com.berrycrush.intellij.psi.BerryCrushStepElement
import com.berrycrush.intellij.psi.BerryCrushTextElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class SyntaxCheckInspection : BerryCrushInspection() {
    override fun checkFile(file: PsiFile, holder: ProblemsHolder) {
        file.children.forEach { element ->
            when (element) {
                is BerryCrushParametersElement -> checkParameter(element, holder)
                is BerryCrushFeatureElement -> checkFeature(element, holder)
                is BerryCrushScenarioElement -> checkScenario(element, holder)
                else -> holder.registerProblem(element, "Unknown element in file")
            }
        }
    }

    fun checkParameter(parameter: BerryCrushParametersElement, holder: ProblemsHolder) {
        parameter.children.forEach { child ->
            if (child !is BerryCrushParameterEntryElement) {
                holder.registerProblem(child, "Invalid parameter entry")
            }
        }
    }

    fun checkFeature(feature: BerryCrushFeatureElement, holder: ProblemsHolder) {
        checkText(feature.children).forEach { element ->
            when (element) {
                is BerryCrushParametersElement -> checkParameter(element, holder)
                is BerryCrushScenarioElement -> checkScenario(element, holder)
                else -> if (element.text.isNotBlank())
                    holder.registerProblem(
                        element,
                        "Feature must contain only parameter, background, scenario or outline"
                    )
            }
        }
    }

    fun checkScenario(scenario: BerryCrushScenarioElement, holder: ProblemsHolder) {
        checkText(scenario.children).forEach { child ->
            when (child) {
                is BerryCrushParametersElement -> checkParameter(child, holder)
                !is BerryCrushStepElement -> if (child.text.isNotBlank())
                    holder.registerProblem(child, "Scenario must contain only parameter or step")
            }
        }
    }

    private fun checkText(elements: Array<PsiElement>): Array<PsiElement> {
        return if (elements.isEmpty()) emptyArray()
        else {
            when (elements[0]) {
                is BerryCrushTextElement -> elements.drop(0).toTypedArray()
                else -> elements
            }
        }
    }
}

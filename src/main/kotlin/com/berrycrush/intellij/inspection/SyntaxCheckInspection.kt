package com.berrycrush.intellij.inspection

import com.berrycrush.intellij.lexer.BerryCrushTokenTypes
import com.berrycrush.intellij.psi.BerryCrushAssertElement
import com.berrycrush.intellij.psi.BerryCrushBackgroundElement
import com.berrycrush.intellij.psi.BerryCrushCallElement
import com.berrycrush.intellij.psi.BerryCrushElseElement
import com.berrycrush.intellij.psi.BerryCrushExamplesElement
import com.berrycrush.intellij.psi.BerryCrushExtractElement
import com.berrycrush.intellij.psi.BerryCrushFeatureElement
import com.berrycrush.intellij.psi.BerryCrushFragmentElement
import com.berrycrush.intellij.psi.BerryCrushIfElement
import com.berrycrush.intellij.psi.BerryCrushIncludeElement
import com.berrycrush.intellij.psi.BerryCrushOutlineElement
import com.berrycrush.intellij.psi.BerryCrushParameterEntryElement
import com.berrycrush.intellij.psi.BerryCrushParametersElement
import com.berrycrush.intellij.psi.BerryCrushPsiElement
import com.berrycrush.intellij.psi.BerryCrushScenarioElement
import com.berrycrush.intellij.psi.BerryCrushScenarioLikeElement
import com.berrycrush.intellij.psi.BerryCrushStepElement
import com.berrycrush.intellij.psi.BerryCrushTagElement
import com.berrycrush.intellij.psi.BerryCrushTextElement
import com.berrycrush.intellij.psi.BerryCrushWebhookElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.elementType

class SyntaxCheckInspection : BerryCrushInspection() {
    override fun checkFile(
        file: PsiFile,
        holder: ProblemsHolder,
    ) {
        val isFragmentFile = file.name.endsWith(".fragment")
        val isScenarioFile = file.name.endsWith(".scenario")

        file.children.filterIsInstance<BerryCrushPsiElement>().forEach { element ->
            when (element) {
                is PsiComment -> Unit
                is BerryCrushFragmentElement -> {
                    if (!isFragmentFile) {
                        holder.registerProblem(element, "Fragment block is only valid in .fragment files")
                    }
                    checkFragment(element, holder)
                }
                is BerryCrushParametersElement -> {
                    if (!isScenarioFile) {
                        holder.registerProblem(element, "Top-level parameters are only valid in .scenario files")
                    }
                    checkParameter(element, holder)
                }
                is BerryCrushFeatureElement -> {
                    if (!isScenarioFile) {
                        holder.registerProblem(element, "Feature block is only valid in .scenario files")
                    }
                    checkFeature(element, holder)
                }
                is BerryCrushScenarioLikeElement -> {
                    if (!isScenarioFile) {
                        holder.registerProblem(element, "Scenario/outline/background blocks are only valid in .scenario files")
                    }
                    checkScenarioLike(element, holder)
                }
                is BerryCrushTagElement -> checkTag(element, holder)
                else -> holder.registerProblem(element, "Unknown element in file")
            }
        }
    }

    private fun checkTag(element: BerryCrushTagElement, holder: ProblemsHolder) {
        fun skipTagAndComment(element: PsiElement?): PsiElement? {
            return when (element) {
                is BerryCrushTagElement,
                is PsiComment,
                is PsiWhiteSpace -> skipTagAndComment(element.nextSibling)
                else -> if (element.elementType == BerryCrushTokenTypes.NEWLINE || element.elementType == BerryCrushTokenTypes.INDENT) {
                    skipTagAndComment(element?.nextSibling)
                } else {
                    element
                }
            }
        }
        when (val e = skipTagAndComment(element)) {
            is BerryCrushFeatureElement, is BerryCrushScenarioElement, is BerryCrushOutlineElement -> Unit
            else -> holder.registerProblem(element, "@tag can only be used on feature, scenario and outline")
        }
    }

    private fun checkParameter(
        parameter: BerryCrushParametersElement,
        holder: ProblemsHolder,
    ) {
        blockChildren(parameter).forEach { child ->
            if (child !is BerryCrushParameterEntryElement) {
                holder.registerProblem(child, "Invalid parameter entry")
            }
        }
    }

    private fun checkFeature(
        feature: BerryCrushFeatureElement,
        holder: ProblemsHolder,
    ) {
        blockChildren(feature).forEach { element ->
            when (element) {
                is BerryCrushParametersElement -> checkParameter(element, holder)
                is BerryCrushBackgroundElement -> checkScenarioLike(element, holder)
                is BerryCrushScenarioElement -> checkScenarioLike(element, holder)
                is BerryCrushOutlineElement -> checkScenarioLike(element, holder)
                is BerryCrushTagElement -> checkTag(element, holder)
                is PsiComment -> Unit
                else ->
                    if (element.text.isNotBlank()) {
                        holder.registerProblem(
                            element,
                            "Feature must contain only parameters, background, scenario or outline blocks",
                        )
                    }
            }
        }
    }

    private fun checkScenarioLike(
        scenario: BerryCrushScenarioLikeElement,
        holder: ProblemsHolder,
    ) {
        val allowParameters = scenario !is BerryCrushBackgroundElement
        val allowExamples = scenario is BerryCrushOutlineElement

        blockChildren(scenario).forEach { child ->
            when (child) {
                is BerryCrushParametersElement -> {
                    if (allowParameters) {
                        checkParameter(child, holder)
                    } else {
                        holder.registerProblem(child, "Background must not contain parameters block")
                    }
                }
                is BerryCrushStepElement -> checkStep(child, holder)
                is BerryCrushExamplesElement -> {
                    if (!allowExamples) {
                        holder.registerProblem(child, "Examples block is only valid in outline")
                    }
                }
                is PsiComment -> Unit
                else ->
                    if (child.text.isNotBlank() && !isRecoveredOutlineRowText(child, allowExamples)) {
                        holder.registerProblem(
                            child,
                            "Scenario/background/outline must contain only parameters, steps and (for outline) examples",
                        )
                    }
            }
        }
    }

    private fun checkStep(
        step: BerryCrushStepElement,
        holder: ProblemsHolder,
    ) {
        blockChildren(step).forEach { child ->
            when (child) {
                is BerryCrushCallElement,
                is BerryCrushWebhookElement,
                is BerryCrushIncludeElement,
                is BerryCrushAssertElement,
                is BerryCrushExtractElement,
                is BerryCrushIfElement,
                is BerryCrushElseElement,
                is BerryCrushStepElement,
                is PsiComment
                -> Unit

                else ->
                    if (child.text.isNotBlank()) {
                        holder.registerProblem(child, "Step must contain only directives")
                    }
            }
        }
    }

    private fun checkFragment(
        fragment: BerryCrushFragmentElement,
        holder: ProblemsHolder,
    ) {
        blockChildren(fragment).forEach { child ->
            when (child) {
                is BerryCrushStepElement -> checkStep(child, holder)
                is BerryCrushCallElement,
                is BerryCrushWebhookElement,
                is BerryCrushIncludeElement,
                is BerryCrushAssertElement,
                is BerryCrushExtractElement,
                is BerryCrushIfElement,
                is BerryCrushElseElement,
                is PsiComment,
                -> Unit

                else ->
                    if (child.text.isNotBlank()) {
                        holder.registerProblem(
                            child,
                            "Fragment must contain only steps or directives",
                        )
                    }
            }
        }
    }

    private fun blockChildren(element: PsiElement): List<BerryCrushPsiElement> {
        val children = element.children.filterIsInstance<BerryCrushPsiElement>()
        if (children.isEmpty()) {
            return emptyList()
        }

        return if (children.first() is BerryCrushTextElement) {
            children.drop(1)
        } else {
            children
        }
    }

    private fun isRecoveredOutlineRowText(
        element: BerryCrushPsiElement,
        allowExamples: Boolean,
    ): Boolean = allowExamples &&
        element is BerryCrushTextElement &&
        element.text.trimStart().startsWith("|")
}

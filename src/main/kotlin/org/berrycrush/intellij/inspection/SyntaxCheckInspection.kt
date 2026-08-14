package org.berrycrush.intellij.inspection

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.elementType
import org.berrycrush.intellij.lexer.BerryCrushTokenTypes
import org.berrycrush.intellij.psi.BerryCrushAssertElement
import org.berrycrush.intellij.psi.BerryCrushBackgroundElement
import org.berrycrush.intellij.psi.BerryCrushBlockNameElement
import org.berrycrush.intellij.psi.BerryCrushCallElement
import org.berrycrush.intellij.psi.BerryCrushElseElement
import org.berrycrush.intellij.psi.BerryCrushExamplesElement
import org.berrycrush.intellij.psi.BerryCrushExtractElement
import org.berrycrush.intellij.psi.BerryCrushFeatureElement
import org.berrycrush.intellij.psi.BerryCrushFragmentElement
import org.berrycrush.intellij.psi.BerryCrushIfElement
import org.berrycrush.intellij.psi.BerryCrushIncludeElement
import org.berrycrush.intellij.psi.BerryCrushNamedScenarioLikeElement
import org.berrycrush.intellij.psi.BerryCrushOutlineElement
import org.berrycrush.intellij.psi.BerryCrushParameterEntryElement
import org.berrycrush.intellij.psi.BerryCrushParametersElement
import org.berrycrush.intellij.psi.BerryCrushPsiElement
import org.berrycrush.intellij.psi.BerryCrushScenarioElement
import org.berrycrush.intellij.psi.BerryCrushStepElement
import org.berrycrush.intellij.psi.BerryCrushStepNameElement
import org.berrycrush.intellij.psi.BerryCrushTagElement
import org.berrycrush.intellij.psi.BerryCrushTextElement
import org.berrycrush.intellij.psi.BerryCrushWebhookElement

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
                is BerryCrushBackgroundElement -> {
                    if (!isScenarioFile) {
                        holder.registerProblem(element, "Background block is only valid in .scenario files")
                    }
                    checkBackground(element, holder)
                }
                is BerryCrushNamedScenarioLikeElement -> {
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
        fun skipTagAndComment(element: PsiElement?): PsiElement? = when (element) {
            is BerryCrushTagElement,
            is PsiComment,
            is PsiWhiteSpace,
            -> skipTagAndComment(element.nextSibling)
            else -> if (element.elementType == BerryCrushTokenTypes.NEWLINE || element.elementType == BerryCrushTokenTypes.INDENT) {
                skipTagAndComment(element?.nextSibling)
            } else {
                element
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
        parameter.children.filterIsInstance<BerryCrushPsiElement>().forEach { child ->
            if (child !is BerryCrushParameterEntryElement) {
                holder.registerProblem(child, "Invalid parameter entry")
            }
        }
    }

    private fun checkFeature(
        feature: BerryCrushFeatureElement,
        holder: ProblemsHolder,
    ) {
        blockChildren<BerryCrushBlockNameElement>(feature, holder).forEach { element ->
            when (element) {
                is BerryCrushParametersElement -> checkParameter(element, holder)
                is BerryCrushBackgroundElement -> checkBackground(element, holder)
                is BerryCrushScenarioElement -> checkScenarioLike(element, holder)
                is BerryCrushOutlineElement -> checkScenarioLike(element, holder)
                is BerryCrushTagElement -> checkTag(element, holder)
                is BerryCrushBlockNameElement,
                is PsiComment,
                -> Unit
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

    private fun checkBackground(
        background: BerryCrushBackgroundElement,
        holder: ProblemsHolder,
    ) {
        background.children.forEach { child ->
            when (child) {
                is BerryCrushStepElement -> checkStep(child, holder)
                is PsiComment -> Unit
                else -> holder.registerProblem(child, "Background must contain only steps")
            }
        }
    }

    private fun checkScenarioLike(
        scenario: BerryCrushNamedScenarioLikeElement,
        holder: ProblemsHolder,
    ) {
        val allowExamples = scenario is BerryCrushOutlineElement

        blockChildren<BerryCrushBlockNameElement>(scenario, holder).forEach { child ->
            when (child) {
                is BerryCrushParametersElement -> checkParameter(child, holder)
                is BerryCrushStepElement -> checkStep(child, holder)
                is BerryCrushBlockNameElement -> Unit
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
        blockChildren<BerryCrushStepNameElement>(step, holder).forEach { child ->
            when (child) {
                is BerryCrushCallElement,
                is BerryCrushWebhookElement,
                is BerryCrushIncludeElement,
                is BerryCrushAssertElement,
                is BerryCrushExtractElement,
                is BerryCrushIfElement,
                is BerryCrushElseElement,
                is BerryCrushStepElement,
                is PsiComment,
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
        blockChildren<BerryCrushBlockNameElement>(fragment, holder) { element, holder ->
            val text = element.text
            if (!text.all { c -> c.isLetterOrDigit() || c in listOf('_', '-', '.') }) {
                holder.registerProblem(element, "Fragment name must be an identifier")
            }
        }.forEach { child ->
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

    private inline fun <reified T> blockChildren(
        element: PsiElement,
        holder: ProblemsHolder,
        nameValidator: (T, ProblemsHolder) -> Unit = { _, _ -> },
    ): List<BerryCrushPsiElement> {
        val children = element.children.filterIsInstance<BerryCrushPsiElement>()
        if (children.isEmpty()) {
            return emptyList()
        }

        val first = children.first()
        return if (first is T) {
            nameValidator(first, holder)
            children.drop(1)
        } else {
            holder.registerProblem(element, "Missing description")
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

package com.berrycrush.intellij.reference

import com.berrycrush.intellij.psi.BerryCrushElementFactory
import com.berrycrush.intellij.psi.BerryCrushExampleHeaderElement
import com.berrycrush.intellij.psi.BerryCrushExtractElement
import com.berrycrush.intellij.psi.BerryCrushOutlineElement
import com.berrycrush.intellij.psi.BerryCrushScenarioLikeElement
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.util.PsiTreeUtil

class BerryCrushVariableReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val variableName: String,
) : PsiReferenceBase<PsiElement>(
    element,
    rangeInElement,
    true,
) {

    override fun resolve(): PsiElement? = resolve(
        element,
        variableName,
    )

    override fun handleElementRename(
        newElementName: String,
    ): PsiElement {
        val range = rangeInElement

        val newText =
            element.text.replaceRange(
                range.startOffset,
                range.endOffset,
                newElementName,
            )

        return element.replace(
            BerryCrushElementFactory.createVariableRefElement(
                element.project,
                newText,
            ),
        )
    }

    override fun getVariants(): Array<Any> = emptyArray()

    companion object {
        fun declarations(
            scenario: BerryCrushScenarioLikeElement,
        ): Sequence<PsiElement> = sequence {
            yieldAll(
                scenario
                    .steps
                    .asSequence()
                    .flatMap { step ->
                        PsiTreeUtil
                            .findChildrenOfType(
                                step,
                                BerryCrushExtractElement::class.java,
                            )
                            .asSequence()
                    },
            )

            if (scenario is BerryCrushOutlineElement) {
                yieldAll(
                    PsiTreeUtil
                        .findChildrenOfType(
                            scenario,
                            BerryCrushExampleHeaderElement::class.java,
                        ),
                )
            }
        }

        fun resolve(
            context: PsiElement,
            name: String,
        ): PsiElement? {
            val scenario =
                PsiTreeUtil.getParentOfType(
                    context,
                    BerryCrushScenarioLikeElement::class.java,
                ) ?: return null

            return declarations(scenario)
                .filter { it is PsiNamedElement }
                .map { it as PsiNamedElement }
                .singleOrNull { it.name == name }
        }
    }
}

package com.berrycrush.intellij.reference

import com.berrycrush.intellij.psi.BerryCrushElementFactory
import com.berrycrush.intellij.psi.BerryCrushExampleHeaderElement
import com.berrycrush.intellij.psi.BerryCrushExtractElement
import com.berrycrush.intellij.psi.BerryCrushFeatureElement
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
) : PsiReferenceBase<PsiElement>(element, rangeInElement, false) {

    override fun resolve(): PsiElement? = resolve(
        element,
        variableName,
    )

    override fun handleElementRename(newElementName: String): PsiElement = element.replace(
        BerryCrushElementFactory.createVariableRefElement(
            element.project,
            newElementName,
        ),
    )

    override fun getVariants(): Array<Any> = emptyArray()

    companion object {
        fun declarations(scenario: BerryCrushScenarioLikeElement): Sequence<PsiElement> = sequence {
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
        ): PsiElement? = PsiTreeUtil.getParentOfType(context, BerryCrushFeatureElement::class.java)?.let { feature ->
            PsiTreeUtil.getChildrenOfType(feature, BerryCrushScenarioLikeElement::class.java)
                ?.flatMap { declarations(it) }
                ?.filterIsInstance<PsiNamedElement>()
                ?.singleOrNull { it.name == name }
        } ?: PsiTreeUtil.getParentOfType(context, BerryCrushScenarioLikeElement::class.java)?.let { scenario ->
            declarations(scenario)
                .filterIsInstance<PsiNamedElement>()
                .singleOrNull { it.name == name }
        }
    }
}

package org.berrycrush.intellij.reference

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.util.PsiTreeUtil
import org.berrycrush.intellij.psi.BerryCrushElementFactory
import org.berrycrush.intellij.psi.BerryCrushExampleHeaderElement
import org.berrycrush.intellij.psi.BerryCrushExtractElement
import org.berrycrush.intellij.psi.BerryCrushFeatureElement
import org.berrycrush.intellij.psi.BerryCrushOutlineElement
import org.berrycrush.intellij.psi.BerryCrushScenarioLikeElement

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

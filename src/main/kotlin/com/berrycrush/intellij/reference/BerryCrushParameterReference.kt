package com.berrycrush.intellij.reference

import com.berrycrush.intellij.psi.BerryCrushElementFactory
import com.berrycrush.intellij.psi.BerryCrushNamedBlockElement
import com.berrycrush.intellij.psi.BerryCrushParameterEntryElement
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.util.PsiTreeUtil

class BerryCrushParameterReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val path: List<String>,
) : PsiReferenceBase<PsiElement>(
    element,
    rangeInElement,
    true,
) {

    override fun resolve(): PsiElement? = resolve(element, path)

    override fun handleElementRename(
        newElementName: String,
    ): PsiElement {
        val oldRange = rangeInElement
        val normalizedName = newElementName.removePrefix("param.")

        val newText =
            element.text.replaceRange(
                oldRange.startOffset,
                oldRange.endOffset,
                normalizedName,
            )

        return element.replace(
            BerryCrushElementFactory.createVariableRefElement(
                element.project,
                newText,
            ),
        )
    }

    companion object {
        fun resolve(
            context: PsiElement,
            path: List<String>,
        ): BerryCrushParameterEntryElement? {
            if (path.isEmpty()) {
                return null
            }

            val scope =
                (context as? BerryCrushNamedBlockElement)
                    ?: PsiTreeUtil.getParentOfType(
                        context,
                        BerryCrushNamedBlockElement::class.java,
                    )
                    ?: return null

            val parameters =
                scope.parameter
                    ?: return null

            var current =
                parameters.entries
                    .firstOrNull {
                        it.parameterName == path.first()
                    }
                    ?: return null

            for (name in path.drop(1)) {
                current =
                    current.findNestedParameter(name)
                        ?: return null
            }

            return current
        }
    }
}

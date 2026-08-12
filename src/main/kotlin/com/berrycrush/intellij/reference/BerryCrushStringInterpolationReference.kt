package com.berrycrush.intellij.reference

import com.berrycrush.intellij.psi.BerryCrushElementFactory
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase

/**
 * Variable interpolation reference inside a quoted string literal.
 */
class BerryCrushStringVariableReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val variableName: String,
) : PsiReferenceBase<PsiElement>(
    element,
    rangeInElement,
    false,
) {
    override fun resolve(): PsiElement? = BerryCrushVariableReference.resolve(element, variableName)

    override fun handleElementRename(newElementName: String): PsiElement {
        val newText = element.text.replaceRange(rangeInElement.startOffset, rangeInElement.endOffset, newElementName)
        return element.replace(BerryCrushElementFactory.createStringLiteralElement(element.project, newText))
    }

    override fun getVariants(): Array<Any> = emptyArray()
}

/**
 * Parameter interpolation reference (e.g. {{param.foo}}) inside a quoted string literal.
 */
class BerryCrushStringParameterReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val path: List<String>,
) : PsiReferenceBase<PsiElement>(
    element,
    rangeInElement,
    false,
) {
    override fun resolve(): PsiElement? = BerryCrushParameterReference.resolve(element, path)

    override fun handleElementRename(newElementName: String): PsiElement {
        val normalizedName = newElementName.removePrefix("param.")
        val newText = element.text.replaceRange(rangeInElement.startOffset, rangeInElement.endOffset, normalizedName)
        return element.replace(BerryCrushElementFactory.createStringLiteralElement(element.project, newText))
    }

    override fun getVariants(): Array<Any> = emptyArray()
}

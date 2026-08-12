package com.berrycrush.intellij.psi

import com.berrycrush.intellij.reference.BerryCrushFragmentReference
import com.berrycrush.intellij.reference.BerryCrushOperationReference
import com.berrycrush.intellij.reference.BerryCrushParameterReference
import com.berrycrush.intellij.reference.BerryCrushVariableReference
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference

interface BerryCrushReferenceElement : PsiElement

/**
 * Variable ref `{{var}}`
 */
class BerryCrushVariableRefElement(
    node: ASTNode,
) : BerryCrushPsiElement(node),
    BerryCrushReferenceElement {
    val variableName
        get() = node.text.removePrefix("{{").removeSuffix("}}")

    val isParameterReference: Boolean
        get() = variableName.startsWith("param.")

    override fun getName(): String = variableName

    override fun getReference(): PsiReference? = if (isParameterReference) {
        val start = 2 + "param.".length
        val end = textLength - 2
        if (start < end) {
            BerryCrushParameterReference(
                this,
                TextRange(2 + "param.".length, textLength - 2),
                variableName.removePrefix("param.").split("."),
            )
        } else {
            null
        }
    } else {
        if (2 < this.textLength - 2) {
            BerryCrushVariableReference(
                this,
                TextRange(2, textLength - 2),
                variableName,
            )
        } else {
            null
        }
    }

    override fun getReferences(): Array<PsiReference> = reference?.let { arrayOf(it) } ?: emptyArray()
}

/**
 * Operation reference element: `^operationId`
 */
class BerryCrushOperationRefElement(
    node: ASTNode,
) : BerryCrushPsiElement(node),
    BerryCrushReferenceElement {
    val operationId: String
        get() = node.text.removePrefix("^")

    override fun getName(): String = operationId

    override fun getReference(): PsiReference {
        val startOffset = if (text.startsWith("^")) 1 else 0
        return BerryCrushOperationReference(
            this,
            TextRange(startOffset, textLength),
            operationId,
        )
    }

    override fun getReferences(): Array<PsiReference> = arrayOf(reference)
}

/**
 * Fragment reference element: the fragment name in an include directive.
 */
class BerryCrushFragmentRefElement(
    node: ASTNode,
) : BerryCrushPsiElement(node),
    BerryCrushReferenceElement {
    override fun getName(): String = node.text

    override fun getReference(): PsiReference = BerryCrushFragmentReference(
        this,
        TextRange(0, textLength),
        name,
    )

    override fun getReferences(): Array<PsiReference> = arrayOf(reference)
}

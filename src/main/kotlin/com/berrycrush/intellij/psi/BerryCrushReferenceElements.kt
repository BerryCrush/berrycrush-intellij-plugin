package com.berrycrush.intellij.psi

import com.berrycrush.intellij.reference.BerryCrushFragmentReference
import com.berrycrush.intellij.reference.BerryCrushOperationReference
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiReference

/**
 * Variable ref `{{var}}`
 */
class BerryCrushVariableRefElement(
    node: ASTNode,
) : BerryCrushPsiElement(node),
    PsiNameIdentifierOwner {
    val variableName
        get() = node.text.removePrefix("{{").removeSuffix("}}")

    override fun getName(): String = variableName

    override fun setName(name: String): PsiElement = this

    override fun getNameIdentifier(): PsiElement? = null
}

/**
 * Operation reference element: `^operationId`
 */
class BerryCrushOperationRefElement(
    node: ASTNode,
) : BerryCrushPsiElement(node) {
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
}

/**
 * Fragment reference element: the fragment name in an include directive.
 */
class BerryCrushFragmentRefElement(
    node: ASTNode,
) : BerryCrushPsiElement(node) {
    override fun getName(): String = node.text

    override fun getReference(): PsiReference = BerryCrushFragmentReference(
        this,
        TextRange(0, textLength),
        name,
    )
}

/**
 * Parameter reference.
 * Similar to `{{var}}` but prefixed with `param.`. e.g. `{{param.name}}`
 */
class BerryCrushParameterRefElement(node: ASTNode) : BerryCrushPsiElement(node)

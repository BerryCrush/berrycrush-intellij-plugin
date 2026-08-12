package org.berrycrush.intellij.psi

import com.intellij.lang.ASTNode

/**
 * Step element.
 */
class BerryCrushStepElement(
    node: ASTNode,
) : BerryCrushPsiElement(node) {
    val keyword: String?
        get() {
            val text = node.text.trim().lowercase()
            return when {
                text.startsWith("given") -> "given"
                text.startsWith("when") -> "when"
                text.startsWith("then") -> "then"
                text.startsWith("and") -> "and"
                text.startsWith("but") -> "but"
                else -> null
            }
        }

    val stepText: String?
        get() = directChildrenOfType<BerryCrushTextElement>().firstOrNull()?.text

    val callDirectives: List<BerryCrushCallElement>
        get() = directChildrenOfType()

    val assertDirectives: List<BerryCrushAssertElement>
        get() = directChildrenOfType()

    val directives: List<BerryCrushDirectiveElement>
        get() = directChildrenOfType()
}

abstract class BerryCrushOperatorLikeElement(node: ASTNode) : BerryCrushPsiElement(node) {
    val operatorName: String
        get() = node.text.trim()
    val operatorType
        get() = node.firstChildNode.elementType
}

class BerryCrushOperatorElement(node: ASTNode) : BerryCrushOperatorLikeElement(node)
class BerryCrushAssertOperationElement(node: ASTNode) : BerryCrushOperatorLikeElement(node)

class BerryCrushJsonPathElement(
    node: ASTNode,
) : BerryCrushPsiElement(node) {
    val jsonPathText
        get() = node.text.trim()
}

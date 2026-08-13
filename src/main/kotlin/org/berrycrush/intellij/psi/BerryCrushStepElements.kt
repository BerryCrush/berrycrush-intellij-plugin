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
        get() = directChildrenOfType<BerryCrushStepNameElement>().firstOrNull()?.text

    val callDirectives: List<BerryCrushCallElement>
        get() = directChildrenOfType()

    val assertDirectives: List<BerryCrushAssertElement>
        get() = directChildrenOfType()

    val directives: List<BerryCrushDirectiveElement>
        get() = directChildrenOfType()
}

class BerryCrushStepNameElement(node: ASTNode) : BerryCrushPsiElement(node)

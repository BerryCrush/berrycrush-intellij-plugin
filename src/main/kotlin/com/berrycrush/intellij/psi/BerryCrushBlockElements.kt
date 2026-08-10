package com.berrycrush.intellij.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement

/**
 * Block element interface
 * - feature
 * - fragment
 * - scenario
 * - background
 * - outline
 */
abstract class BerryCrushBlockElement(
    node: ASTNode,
) : BerryCrushPsiElement(node),
    PsiNameIdentifierOwner {
    abstract val keyword: String
    val description: String?
        get() = directChildrenOfType<BerryCrushTextElement>().firstOrNull()?.text

    val parameter: BerryCrushParametersElement?
        get() = directChildrenOfType<BerryCrushParametersElement>().firstOrNull()
    override fun getName(): String? = description
}

/**
 * Fragment definition element.
 */
class BerryCrushFragmentElement(
    node: ASTNode,
) : BerryCrushScenarioLikeElement(node),
    PsiNameIdentifierOwner {
    override val keyword = "fragment"

    val fragmentName: String?
        get() = description

    override fun setName(name: String): PsiElement = this

    override fun getNameIdentifier(): PsiElement? = directChildrenOfType<BerryCrushTextElement>().firstOrNull()
}

/**
 * Feature block element.
 */
class BerryCrushFeatureElement(
    node: ASTNode,
) : BerryCrushBlockElement(node),
    PsiNameIdentifierOwner {
    override val keyword = "feature"

    val blocks: List<BerryCrushScenarioLikeElement>
        get() = directChildrenOfType()

    val backgrounds: List<BerryCrushBackgroundElement>
        get() = directChildrenOfType()

    val scenarios: List<BerryCrushScenarioElement>
        get() = directChildrenOfType()

    override fun setName(name: String): PsiElement = this

    override fun getNameIdentifier(): PsiElement? = null
}

/**
 * Scenario block element.
 */
abstract class BerryCrushScenarioLikeElement(
    node: ASTNode,
) : BerryCrushBlockElement(node),
    PsiNameIdentifierOwner {
    val steps: List<BerryCrushStepElement>
        get() = directChildrenOfType()

    override fun setName(name: String): PsiElement = this

    override fun getNameIdentifier(): PsiElement? = null
}

/**
 * ```berrycrush
 * scenario: name
 *   steps...
 * ```
 */
class BerryCrushScenarioElement(node: ASTNode) : BerryCrushScenarioLikeElement(node) {
    override val keyword = "scenario"
}

/**
 * ```berrycrush
 * background:
 *   steps...
 * ```
 */
class BerryCrushBackgroundElement(node: ASTNode) : BerryCrushScenarioLikeElement(node) {
    override val keyword = "background"
}

/**
 * ```berrycrush
 * outline: name
 *   steps...
 *   examples:
 * ```
 */
class BerryCrushOutlineElement(node: ASTNode) : BerryCrushScenarioLikeElement(node) {
    override val keyword = "outline"
}

class BerryCrushExamplesElement(node: ASTNode) : BerryCrushPsiElement(node)

class BerryCrushExampleRowElement(node: ASTNode) : BerryCrushPsiElement(node)
class BerryCrushExampleHeaderElement(node: ASTNode) :
    BerryCrushPsiElement(node),
    PsiNamedElement {
    override fun getName(): String = node.text
    override fun setName(name: String): PsiElement = this
}
class BerryCrushExampleValueElement(node: ASTNode) : BerryCrushPsiElement(node)

class BerryCrushTagElement(node: ASTNode) : BerryCrushPsiElement(node)

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
interface BerryCrushBlockElement {
    val keyword: String
}

interface BerryCrushParameterizedElement {
    val parameter: BerryCrushParametersElement?
}

abstract class BerryCrushNamedBlockElement(
    node: ASTNode,
) : BerryCrushNamedElement(node),
    BerryCrushBlockElement,
    BerryCrushParameterizedElement {
    val description: String?
        get() = directChildrenOfType<BerryCrushBlockNameElement>().firstOrNull()?.text

    override fun getName(): String? = description

    override val parameter: BerryCrushParametersElement?
        get() = directChildrenOfType<BerryCrushParametersElement>().firstOrNull()

    override fun getNameIdentifier(): PsiElement? = directChildrenOfType<BerryCrushBlockNameElement>().firstOrNull()

    override fun createIdentifier(text: String): PsiElement = BerryCrushElementFactory.createBlockNameIdentifier(project, "$keyword: $text")
}

/**
 * Fragment, feature, scenario or outline description
 */
class BerryCrushBlockNameElement(node: ASTNode) : BerryCrushPsiElement(node)

/**
 * Feature block element.
 */
class BerryCrushFeatureElement(
    node: ASTNode,
) : BerryCrushNamedBlockElement(node),
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
 * Scenario like block element.
 */
interface BerryCrushScenarioLikeElement : BerryCrushBlockElement {
    val steps: List<BerryCrushStepElement>
}

abstract class BerryCrushNamedScenarioLikeElement(
    node: ASTNode,
) : BerryCrushNamedBlockElement(node),
    BerryCrushScenarioLikeElement {
    override val steps: List<BerryCrushStepElement>
        get() = directChildrenOfType()

    override fun setName(name: String): PsiElement = this

    override fun getNameIdentifier(): PsiElement? = null
}

/**
 * Fragment definition element.
 */
class BerryCrushFragmentElement(
    node: ASTNode,
) : BerryCrushNamedScenarioLikeElement(node),
    PsiNameIdentifierOwner {
    override val keyword = "fragment"

    val fragmentName: String?
        get() = description

    override fun setName(name: String): PsiElement = this

    override fun getNameIdentifier(): PsiElement? = directChildrenOfType<BerryCrushTextElement>().firstOrNull()
}

/**
 * ```berrycrush
 * scenario: name
 *   steps...
 * ```
 */
class BerryCrushScenarioElement(node: ASTNode) : BerryCrushNamedScenarioLikeElement(node) {
    override val keyword = "scenario"
}

/**
 * ```berrycrush
 * background:
 *   steps...
 * ```
 */
class BerryCrushBackgroundElement(node: ASTNode) :
    BerryCrushPsiElement(node),
    BerryCrushScenarioLikeElement {
    override val keyword = "background"
    override val steps: List<BerryCrushStepElement>
        get() = directChildrenOfType()
}

/**
 * ```berrycrush
 * outline: name
 *   steps...
 *   examples:
 * ```
 */
class BerryCrushOutlineElement(node: ASTNode) : BerryCrushNamedScenarioLikeElement(node) {
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

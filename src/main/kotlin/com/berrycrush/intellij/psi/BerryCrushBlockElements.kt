package com.berrycrush.intellij.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner

/**
 * Block element interface
 * - feature
 * - fragment
 * - scenario
 * - background
 * - outline
 */
interface BerryCrushBlockElement : PsiElement {
    val keyword: String
}

interface BerryCrushParameterizedElement : PsiElement {
    val parameter: BerryCrushParametersElement?
}

interface BerryCrushNamedBlockElement :
    BerryCrushBlockElement,
    BerryCrushParameterizedElement {
    val description: String?
}

abstract class BerryCrushNamedBlockIdentifierElement(
    node: ASTNode,
) : BerryCrushNamedElement(node),
    BerryCrushNamedBlockElement {
    override val description: String?
        get() = directChildrenOfType<BerryCrushBlockNameElement>().firstOrNull()?.text

    override fun getName(): String? = description

    override val parameter: BerryCrushParametersElement?
        get() = directChildrenOfType<BerryCrushParametersElement>().firstOrNull()

    override fun getNameIdentifier(): PsiElement? = directChildrenOfType<BerryCrushBlockNameElement>().firstOrNull()

    override fun createIdentifier(text: String): PsiElement = BerryCrushElementFactory.createBlockNameIdentifier(project, "$keyword: $text")
}

abstract class BerryCrushUnrenamableNamedBlockElement(node: ASTNode) :
    BerryCrushPsiElement(node),
    BerryCrushNamedBlockElement {
    override val description: String?
        get() = directChildrenOfType<BerryCrushBlockNameElement>().firstOrNull()?.text

    override fun getName(): String? = description

    override val parameter: BerryCrushParametersElement?
        get() = directChildrenOfType<BerryCrushParametersElement>().firstOrNull()
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
) : BerryCrushUnrenamableNamedBlockElement(node) {
    override val keyword = "feature"

    val blocks: List<BerryCrushScenarioLikeElement>
        get() = directChildrenOfType()

    val backgrounds: List<BerryCrushBackgroundElement>
        get() = directChildrenOfType()

    val scenarios: List<BerryCrushScenarioElement>
        get() = directChildrenOfType()
}

/**
 * Scenario like block element.
 */
interface BerryCrushScenarioLikeElement : BerryCrushBlockElement {
    val steps: List<BerryCrushStepElement>
}

interface BerryCrushNamedScenarioLikeElement :
    BerryCrushNamedBlockElement,
    BerryCrushScenarioLikeElement

/**
 * Fragment definition element.
 */
class BerryCrushFragmentElement(
    node: ASTNode,
) : BerryCrushNamedBlockIdentifierElement(node),
    BerryCrushNamedScenarioLikeElement,
    PsiNameIdentifierOwner {
    override val steps: List<BerryCrushStepElement>
        get() = directChildrenOfType()

    override val keyword = "fragment"

    val fragmentName: String?
        get() = description

    // Only for fragment, this would be a identifier
    override fun getIdentifyingElement(): PsiElement? = directChildrenOfType<BerryCrushBlockNameElement>().firstOrNull()
}

abstract class BerryCrushUnrenamableScenarioLikeElement(node: ASTNode) :
    BerryCrushUnrenamableNamedBlockElement(node),
    BerryCrushNamedScenarioLikeElement {
    override val steps: List<BerryCrushStepElement>
        get() = directChildrenOfType()
}

/**
 * ```berrycrush
 * scenario: name
 *   steps...
 * ```
 */
class BerryCrushScenarioElement(node: ASTNode) : BerryCrushUnrenamableScenarioLikeElement(node) {
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
class BerryCrushOutlineElement(node: ASTNode) : BerryCrushUnrenamableScenarioLikeElement(node) {
    override val keyword = "outline"
}

class BerryCrushExamplesElement(node: ASTNode) : BerryCrushPsiElement(node)

class BerryCrushExampleRowElement(node: ASTNode) : BerryCrushPsiElement(node)
class BerryCrushExampleHeaderElement(node: ASTNode) : BerryCrushNamedElement(node) {
    override fun getName(): String = node.text
    override fun getNameIdentifier(): PsiElement = this
    override fun createIdentifier(text: String): PsiElement = BerryCrushElementFactory.createExampleHeaderElement(project, text)
}
class BerryCrushExampleValueElement(node: ASTNode) : BerryCrushPsiElement(node)

class BerryCrushTagElement(node: ASTNode) : BerryCrushPsiElement(node)

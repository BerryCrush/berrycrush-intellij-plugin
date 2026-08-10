package com.berrycrush.intellij.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement

/**
 * Base directive element
 */
abstract class BerryCrushDirectiveElement(
    val directiveName: String,
    node: ASTNode,
) : BerryCrushPsiElement(node)

/**
 * Include like, base class for directives which accept parameter
 * - call
 * - include
 * - webhook
 */
abstract class BerryCrushIncludeLikeElement(
    name: String,
    node: ASTNode,
) : BerryCrushDirectiveElement(name, node) {
    /**
     * Get all parameter elements for this include directive.
     */
    val parameters: BerryCrushIncludeParameterElement?
        get() = findChildByClass(BerryCrushIncludeParameterElement::class.java)

    /**
     * Get parameter names as a set.
     */
    val parameterNames: List<String>
        get() = parameters?.parameterNames?.toList() ?: emptyList()

    fun findParameter(name: String): BerryCrushParameterEntryElement? = parameters?.entries?.find { it.parameterName == name }
}

/**
 * Call directive element: `call ^operationId` with optional parameters
 */
class BerryCrushCallElement(
    node: ASTNode,
) : BerryCrushIncludeLikeElement("call", node) {
    val operationRef: BerryCrushOperationRefElement?
        get() = findChildByClass(BerryCrushOperationRefElement::class.java)

    val operationId: String?
        get() = operationRef?.operationId
}

/**
 * Include directive element: `include fragmentName` with optional parameters.
 */
class BerryCrushIncludeElement(
    node: ASTNode,
) : BerryCrushIncludeLikeElement("include", node),
    PsiNameIdentifierOwner {
    val fragmentRef: BerryCrushFragmentRefElement?
        get() = directChildrenOfType<BerryCrushFragmentRefElement>().firstOrNull()

    val fragmentName: String?
        get() = fragmentRef?.name

    override fun getName(): String? = fragmentName

    override fun setName(name: String): PsiElement = this

    override fun getNameIdentifier(): PsiElement? {
        val childNode = node.findChildByType(BerryCrushElementTypes.FRAGMENT_REF)
        return childNode?.psi
    }
}

/**
 * Webhook directive element
 */
class BerryCrushWebhookElement(
    node: ASTNode,
) : BerryCrushIncludeLikeElement("webhook", node)

/**
 * Assert directive element.
 */
class BerryCrushAssertElement(
    node: ASTNode,
) : BerryCrushDirectiveElement("assert", node) {
    val assertionText: String?
        get() {
            val text = node.text.trim()
            // Strict lowercase matching for "assert" keyword
            val match = Regex("""^$directiveName\s+(.+)$""").find(text)
            return match?.groupValues?.get(1)?.trim()
        }
    val condition: BerryCrushConditionElement?
        get() = directChildrenOfType<BerryCrushConditionElement>().firstOrNull()
}

/**
 * ```berrycrush
 * extract $.foo => var
 * ```
 */
class BerryCrushExtractElement(
    node: ASTNode,
) : BerryCrushDirectiveElement("extract", node),
    PsiNamedElement {
    val extractName
        get() = node.text.trim()

    override fun getName(): String = extractName
    override fun setName(name: String): PsiElement = this
}

/**
 * If directive element
 */
class BerryCrushIfElement(
    node: ASTNode,
) : BerryCrushDirectiveElement("if", node)

/**
 * Else directive element
 */
class BerryCrushElseElement(
    node: ASTNode,
) : BerryCrushDirectiveElement("else", node)

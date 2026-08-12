package com.berrycrush.intellij.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner

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
) : BerryCrushIncludeLikeElement("include", node) {
    val fragmentRef: BerryCrushFragmentRefElement?
        get() = directChildrenOfType<BerryCrushFragmentRefElement>().firstOrNull()

    val fragmentName: String?
        get() = fragmentRef?.name
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
    PsiNameIdentifierOwner {
    private val extractNamePattern = Regex("""(=>\s*)([A-Za-z_][A-Za-z0-9_]*)""")

    val extractName
        get() = name

    override fun getName(): String? = extractNamePattern.find(text)?.groupValues?.get(2)

    override fun setName(name: String): PsiElement {
        val nameIdentifier = nameIdentifier ?: return this
        nameIdentifier.replace(BerryCrushElementFactory.createExtractVariableNameElement(project, name).nameIdentifier!!)
        return this
    }

    override fun getNameIdentifier(): PsiElement? = directChildrenOfType<BerryCrushVariableRefElement>().lastOrNull()
        ?: directChildrenOfType<BerryCrushTextElement>().lastOrNull()
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

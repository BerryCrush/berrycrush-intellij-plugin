package org.berrycrush.intellij.psi

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

class BerryCrushUsingElement(
    node: ASTNode,
) : BerryCrushPsiElement(node) {
    override fun getName(): String = text
}

class BerryCrushBindingNameElement(
    node: ASTNode,
) : BerryCrushPsiElement(node) {
    override fun getName(): String = text
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

interface BerryCrushConditionHolderElement : PsiElement {
    val condition: BerryCrushConditionElement?
}

/**
 * Assert directive element.
 */
class BerryCrushAssertElement(
    node: ASTNode,
) : BerryCrushDirectiveElement("assert", node),
    BerryCrushConditionHolderElement {
    val assertionText: String?
        get() {
            val text = node.text.trim()
            // Strict lowercase matching for "assert" keyword
            val match = Regex("""^$directiveName\s+(.+)$""").find(text)
            return match?.groupValues?.get(1)?.trim()
        }
    override val condition: BerryCrushConditionElement?
        get() = directChildrenOfType<BerryCrushConditionElement>().firstOrNull()
}

/**
 * Fail directive
 * ```berrycrush
 * fail "foo"
 * ```
 */
class BerryCrushFailElement(node: ASTNode) : BerryCrushDirectiveElement("fail", node)

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

abstract class BerryCrushConditionalElement(name: String, node: ASTNode) : BerryCrushDirectiveElement(name, node)

/**
 * If directive element
 */
class BerryCrushIfElement(
    node: ASTNode,
) : BerryCrushConditionalElement("if", node),
    BerryCrushConditionHolderElement {
    override val condition: BerryCrushConditionElement?
        get() = directChildrenOfType<BerryCrushConditionElement>().firstOrNull()
}

/**
 * Else directive element
 */
class BerryCrushElseElement(
    node: ASTNode,
) : BerryCrushConditionalElement("else", node)

// leaf elements
class BerryCrushJsonPathElement(
    node: ASTNode,
) : BerryCrushPsiElement(node) {
    val jsonPathText
        get() = node.text.trim()
}

abstract class BerryCrushOperatorLikeElement(node: ASTNode) : BerryCrushPsiElement(node) {
    val operatorName: String
        get() = node.text.trim()
    val operatorType
        get() = node.firstChildNode.elementType
}

class BerryCrushOperatorElement(node: ASTNode) : BerryCrushOperatorLikeElement(node)
class BerryCrushAssertOperationElement(node: ASTNode) : BerryCrushOperatorLikeElement(node)

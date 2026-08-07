package com.berrycrush.intellij.psi

import com.berrycrush.intellij.reference.BerryCrushFragmentReference
import com.berrycrush.intellij.reference.BerryCrushOperationReference
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.psi.util.PsiTreeUtil

/**
 * Base class for all BerryCrush PSI elements.
 * Integrates with ReferenceProvidersRegistry for reference discovery.
 */
abstract class BerryCrushPsiElement(
    node: ASTNode,
) : ASTWrapperPsiElement(node) {
    override fun getReferences(): Array<PsiReference> = ReferenceProvidersRegistry.getReferencesFromProviders(this)

    companion object {
        /**
         * Extract parameter name from a "key: value" formatted string.
         */
        @JvmStatic
        fun extractParamName(text: String): String? {
            val colonIndex = text.indexOf(':')
            return if (colonIndex > 0) text.substring(0, colonIndex).trim() else null
        }

        /**
         * Extract parameter value from a "key: value" formatted string.
         */
        @JvmStatic
        fun extractParamValue(text: String): String? {
            val colonIndex = text.indexOf(':')
            return if (colonIndex >= 0 && colonIndex < text.length - 1) {
                text.substring(colonIndex + 1).trim()
            } else {
                null
            }
        }
    }

    protected inline fun <reified T : PsiElement> directChildrenOfType(): List<T> = children.filterIsInstance<T>()
}

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
 * Fragment reference element: the fragment name in an include directive.
 */
class BerryCrushFragmentRefElement(
    node: ASTNode,
) : BerryCrushPsiElement(node),
    PsiNameIdentifierOwner,
    PsiNamedElement {
    override fun getName(): String = node.text

    override fun setName(name: String): PsiElement = this

    override fun getNameIdentifier(): PsiElement = this

    override fun getReference(): PsiReference = BerryCrushFragmentReference(
        this,
        TextRange(0, textLength),
        name,
    )
}

/**
 * Operation reference element: `^operationId`
 */
class BerryCrushOperationRefElement(
    node: ASTNode,
) : BerryCrushPsiElement(node),
    PsiNameIdentifierOwner {
    val operationId: String
        get() = node.text.removePrefix("^")

    override fun getName(): String = operationId

    override fun setName(name: String): PsiElement = this

    override fun getNameIdentifier(): PsiElement = this

    override fun getReference(): PsiReference {
        val startOffset = if (text.startsWith("^")) 1 else 0
        return BerryCrushOperationReference(
            this,
            TextRange(startOffset, textLength),
            operationId,
        )
    }
}

abstract class BerryCrushDirectiveElement(
    val directiveName: String,
    node: ASTNode,
) : BerryCrushPsiElement(node)

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
 * Variable ref `{{var}}`
 */
class BerryCrushVariableRefElement(
    node: ASTNode,
) : BerryCrushPsiElement(node),
    PsiNameIdentifierOwner {
    val variableName
        get() = node.text.removePrefix("{{").removeSuffix("}}")

    override fun getName(): String? = variableName

    override fun setName(name: String): PsiElement = this

    override fun getNameIdentifier(): PsiElement? = null
}

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

class BerryCrushScenarioElement(node: ASTNode) : BerryCrushScenarioLikeElement(node) {
    override val keyword = "scenario"
}

class BerryCrushOutlineElement(node: ASTNode) : BerryCrushScenarioLikeElement(node) {
    override val keyword = "outline"
}

class BerryCrushBackgroundElement(node: ASTNode) : BerryCrushScenarioLikeElement(node) {
    override val keyword = "background"
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

class BerryCrushIfElement(
    node: ASTNode,
) : BerryCrushDirectiveElement("if", node)

class BerryCrushElseElement(
    node: ASTNode,
) : BerryCrushDirectiveElement("else", node)

class BerryCrushConditionElement(node: ASTNode) : BerryCrushPsiElement(node)

class BerryCrushNotElement(
    node: ASTNode,
) : BerryCrushPsiElement(node)

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

class BerryCrushExtractElement(
    node: ASTNode,
) : BerryCrushDirectiveElement("extract", node),
    PsiNamedElement {
    val extractName
        get() = node.text.trim()

    override fun getName(): String = extractName
    override fun setName(name: String): PsiElement = this
}

class BerryCrushWebhookElement(
    node: ASTNode,
) : BerryCrushDirectiveElement("webhook", node)

/**
 * Include parameter element: `paramName: value` inside an include directive.
 */
class BerryCrushIncludeParameterElement(
    node: ASTNode,
) : BerryCrushParameterLikeElement(node)

/**
 * Generic element for unspecified element types.
 */
class BerryCrushGenericElement(
    node: ASTNode,
) : BerryCrushPsiElement(node)

abstract class BerryCrushParameterLikeElement(
    node: ASTNode,
) : BerryCrushPsiElement(node) {
    /**
     * Get all parameter entries in this block.
     */
    val entries: List<BerryCrushParameterEntryElement>
        get() = findChildrenByClass(BerryCrushParameterEntryElement::class.java).toList()

    /**
     * Get parameter names defined in this block.
     */
    val parameterNames: List<String>
        get() = entries.mapNotNull { it.parameterName }

    /**
     * Get a parameter value by name.
     */
    fun getParameterValue(name: String): String? = entries.firstOrNull { it.parameterName == name }?.parameterValue
}

/**
 * Parameters block element:
 * ```
 * parameters:
 *   key: value
 *   key2: value2
 * ```
 * Used in scenario and feature blocks.
 */
class BerryCrushParametersElement(
    node: ASTNode,
) : BerryCrushParameterLikeElement(node)

/**
 * Single parameter entry element: `key: value`
 * Used inside a parameters block.
 */
class BerryCrushParameterEntryElement(
    node: ASTNode,
) : BerryCrushPsiElement(node),
    PsiNameIdentifierOwner {
    /**
     * The parameter name (key before the colon).
     */
    val parameterName: String?
        get() = extractParamName(node.text.trim())

    /**
     * The parameter value (after the colon).
     */
    val parameterValue: String?
        get() = extractParamValue(node.text.trim())

    fun findNestedParameter(name: String): BerryCrushParameterEntryElement? = PsiTreeUtil
        .findChildrenOfType(this, BerryCrushParameterEntryElement::class.java)
        .find { it.parameterName == name }

    override fun getName(): String? = parameterName

    override fun setName(name: String): PsiElement = this

    override fun getNameIdentifier(): PsiElement? = null
}

class BerryCrushParameterKeyElement(
    node: ASTNode,
) : BerryCrushPsiElement(node),
    PsiNamedElement {
    val keyName
        get() = node.text.removeSuffix(":").trim()

    override fun getName(): String = keyName
    override fun setName(name: String): PsiElement = this
}

// value can be text or other node...
class BerryCrushParameterValueElement(
    node: ASTNode,
) : BerryCrushPsiElement(node)

class BerryCrushTextElement(
    node: ASTNode,
) : BerryCrushPsiElement(node)

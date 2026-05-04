package com.berrycrush.intellij.psi

import com.berrycrush.intellij.lexer.BerryCrushTokenTypes
import com.berrycrush.intellij.reference.BerryCrushFragmentReference
import com.berrycrush.intellij.reference.BerryCrushOperationReference
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry

/**
 * Base class for all BerryCrush PSI elements.
 * Integrates with ReferenceProvidersRegistry for reference discovery.
 */
abstract class BerryCrushPsiElement(node: ASTNode) : ASTWrapperPsiElement(node) {
    override fun getReferences(): Array<PsiReference> {
        return ReferenceProvidersRegistry.getReferencesFromProviders(this)
    }
}

/**
 * Include directive element: `include fragmentName` with optional parameters.
 */
class BerryCrushIncludeElement(node: ASTNode) : BerryCrushPsiElement(node), PsiNameIdentifierOwner {
    val fragmentName: String?
        get() {
            val text = node.text
            val match = Regex("""include\s+\^?([a-zA-Z_][a-zA-Z0-9_.\-]*)""").find(text)
            return match?.groupValues?.get(1)
        }

    /**
     * Get all parameter elements for this include directive.
     */
    val parameters: List<BerryCrushIncludeParameterElement>
        get() = findChildrenByClass(BerryCrushIncludeParameterElement::class.java).toList()

    /**
     * Get parameter names as a set.
     */
    val parameterNames: Set<String>
        get() = parameters.mapNotNull { it.parameterName }.toSet()

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
class BerryCrushFragmentRefElement(node: ASTNode) : BerryCrushPsiElement(node), PsiNameIdentifierOwner {
    override fun getName(): String = node.text.removePrefix("^")

    override fun setName(name: String): PsiElement = this

    override fun getNameIdentifier(): PsiElement = this

    override fun getReference(): PsiReference {
        return BerryCrushFragmentReference(
            this,
            TextRange(0, textLength),
            name
        )
    }
}

/**
 * Operation reference element: `^operationId`
 */
class BerryCrushOperationRefElement(node: ASTNode) : BerryCrushPsiElement(node), PsiNameIdentifierOwner {
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
            operationId
        )
    }
}

/**
 * Call directive element: `call ^operationId`
 */
class BerryCrushCallElement(node: ASTNode) : BerryCrushPsiElement(node) {
    val operationRef: BerryCrushOperationRefElement?
        get() = findChildByClass(BerryCrushOperationRefElement::class.java)

    val operationId: String?
        get() = operationRef?.operationId
}

/**
 * Feature block element.
 */
class BerryCrushFeatureElement(node: ASTNode) : BerryCrushPsiElement(node), PsiNameIdentifierOwner {
    val featureName: String?
        get() {
            val text = node.text
            val match = Regex("""[Ff]eature:\s*(.+)""").find(text.lines().first())
            return match?.groupValues?.get(1)?.trim()
        }

    override fun getName(): String? = featureName

    override fun setName(name: String): PsiElement = this

    override fun getNameIdentifier(): PsiElement? = null
}

/**
 * Scenario block element.
 */
class BerryCrushScenarioElement(node: ASTNode) : BerryCrushPsiElement(node), PsiNameIdentifierOwner {
    val scenarioName: String?
        get() {
            val text = node.text
            val match = Regex("""[Ss]cenario:\s*(.+)""").find(text.lines().first())
            return match?.groupValues?.get(1)?.trim()
        }

    override fun getName(): String? = scenarioName

    override fun setName(name: String): PsiElement = this

    override fun getNameIdentifier(): PsiElement? = null
}

/**
 * Fragment definition element.
 */
class BerryCrushFragmentElement(node: ASTNode) : BerryCrushPsiElement(node), PsiNameIdentifierOwner {
    val fragmentName: String?
        get() {
            val text = node.text
            val match = Regex("""[Ff]ragment:\s*(.+)""").find(text.lines().first())
            return match?.groupValues?.get(1)?.trim()
        }

    override fun getName(): String? = fragmentName

    override fun setName(name: String): PsiElement = this

    override fun getNameIdentifier(): PsiElement? {
        // Find the TEXT token after the FRAGMENT keyword that contains the name
        var child = node.firstChildNode
        while (child != null) {
            if (child.elementType == BerryCrushTokenTypes.TEXT) {
                return child.psi
            }
            // Stop searching after first NEWLINE (name is on the first line)
            if (child.elementType == BerryCrushTokenTypes.NEWLINE) {
                break
            }
            child = child.treeNext
        }
        return null
    }
}

/**
 * Step element.
 */
class BerryCrushStepElement(node: ASTNode) : BerryCrushPsiElement(node) {
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
        get() {
            val text = node.text.trim()
            val kw = keyword ?: return null
            // Remove the keyword prefix (case-insensitive)
            val prefixLength = kw.length
            return text.substring(prefixLength).trim()
        }
}

/**
 * Assert directive element.
 */
class BerryCrushAssertElement(node: ASTNode) : BerryCrushPsiElement(node) {
    val assertionText: String?
        get() {
            val text = node.text.trim()
            // Strict lowercase matching for "assert" keyword
            val match = Regex("""^assert\s+(.+)$""").find(text)
            return match?.groupValues?.get(1)?.trim()
        }
}

/**
 * Include parameter element: `paramName: value` inside an include directive.
 */
class BerryCrushIncludeParameterElement(node: ASTNode) : BerryCrushPsiElement(node), PsiNameIdentifierOwner {
    /**
     * The parameter name (key before the colon).
     */
    val parameterName: String?
        get() {
            val text = node.text.trim()
            val colonIndex = text.indexOf(':')
            return if (colonIndex > 0) text.substring(0, colonIndex).trim() else null
        }

    /**
     * The parameter value (after the colon).
     */
    val parameterValue: String?
        get() {
            val text = node.text.trim()
            val colonIndex = text.indexOf(':')
            return if (colonIndex >= 0 && colonIndex < text.length - 1) {
                text.substring(colonIndex + 1).trim()
            } else {
                null
            }
        }

    override fun getName(): String? = parameterName

    override fun setName(name: String): PsiElement = this

    override fun getNameIdentifier(): PsiElement? = null
}

/**
 * Generic element for unspecified element types.
 */
class BerryCrushGenericElement(node: ASTNode) : BerryCrushPsiElement(node)

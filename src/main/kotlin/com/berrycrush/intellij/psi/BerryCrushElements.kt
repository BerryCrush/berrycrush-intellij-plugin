package com.berrycrush.intellij.psi

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
 * Include directive element: `include fragmentName`
 */
class BerryCrushIncludeElement(node: ASTNode) : BerryCrushPsiElement(node), PsiNameIdentifierOwner {
    val fragmentName: String?
        get() {
            val text = node.text
            val match = Regex("""include\s+\^?([a-zA-Z_][a-zA-Z0-9_.\-]*)""").find(text)
            return match?.groupValues?.get(1)
        }

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
            val match = Regex("""Feature:\s*(.+)""").find(text.lines().first())
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
            val match = Regex("""Scenario:\s*(.+)""").find(text.lines().first())
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
            val match = Regex("""Fragment:\s*(.+)""").find(text.lines().first())
            return match?.groupValues?.get(1)?.trim()
        }

    override fun getName(): String? = fragmentName

    override fun setName(name: String): PsiElement = this

    override fun getNameIdentifier(): PsiElement? = null
}

/**
 * Step element.
 */
class BerryCrushStepElement(node: ASTNode) : BerryCrushPsiElement(node) {
    val keyword: String?
        get() {
            val text = node.text.trim()
            return when {
                text.startsWith("Given") -> "Given"
                text.startsWith("When") -> "When"
                text.startsWith("Then") -> "Then"
                text.startsWith("And") -> "And"
                text.startsWith("But") -> "But"
                else -> null
            }
        }

    val stepText: String?
        get() {
            val text = node.text.trim()
            val keyword = keyword ?: return null
            return text.removePrefix(keyword).trim()
        }
}

/**
 * Generic element for unspecified element types.
 */
class BerryCrushGenericElement(node: ASTNode) : BerryCrushPsiElement(node)

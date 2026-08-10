package com.berrycrush.intellij.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry

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

class BerryCrushConditionElement(node: ASTNode) : BerryCrushPsiElement(node)

class BerryCrushNotElement(
    node: ASTNode,
) : BerryCrushPsiElement(node)

/**
 * Generic element for unspecified element types.
 */
class BerryCrushGenericElement(
    node: ASTNode,
) : BerryCrushPsiElement(node)

class BerryCrushTextElement(
    node: ASTNode,
) : BerryCrushPsiElement(node)

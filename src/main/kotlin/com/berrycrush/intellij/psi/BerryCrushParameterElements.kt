package com.berrycrush.intellij.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil

/**
 * Base lass for parameter
 */
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
 * Include parameter element: `paramName: value` inside an include like directive.
 */
class BerryCrushIncludeParameterElement(
    node: ASTNode,
) : BerryCrushParameterLikeElement(node)

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
) : BerryCrushNamedElement(node),
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

    override fun getName(): String? = parameterName

    fun findNestedParameter(name: String): BerryCrushParameterEntryElement? = PsiTreeUtil
        .findChildrenOfType(this, BerryCrushParameterEntryElement::class.java)
        .find { it.parameterName == name }

    override fun getUseScope(): SearchScope {
        val namedBlock = PsiTreeUtil.getParentOfType(this, BerryCrushNamedBlockElement::class.java)
        return if (namedBlock != null) LocalSearchScope(namedBlock) else super.getUseScope()
    }

    override fun createIdentifier(text: String): PsiElement = BerryCrushElementFactory.createParameterKeyElement(project, text)

    override fun getNameIdentifier(): PsiElement? = directChildrenOfType<BerryCrushParameterKeyElement>().firstOrNull()
}

class BerryCrushParameterKeyElement(
    node: ASTNode,
) : BerryCrushPsiElement(node) {
    val keyName
        get() = node.text.removeSuffix(":").trim()
}

// value can be text or other node...
class BerryCrushParameterValueElement(
    node: ASTNode,
) : BerryCrushPsiElement(node)

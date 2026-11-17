package org.berrycrush.intellij.psi

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.PsiReference
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.berrycrush.intellij.reference.BerryCrushNamedParametersReference

/**
 * Base class for parameter
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
) : BerryCrushParameterLikeElement(node),
    BerryCrushNameIdentifierOwner {
    override fun getName(): String? = nameIdentifier?.text
    override fun getNameIdentifier(): PsiElement? = directChildrenOfType<BerryCrushBlockNameElement>().firstOrNull()
    override fun createIdentifier(text: String): PsiElement = BerryCrushElementFactory.createBlockNameIdentifier(project, "parameters: $text")
}

class BerryCrushParameterIncludeRefElement(
    node: ASTNode,
) : BerryCrushPsiElement(node),
    BerryCrushReferenceElement {
    override fun getName(): String = node.text.substringAfter("<<").trim()

    override fun getReference(): PsiReference? {
        val rawText = text
        val markerOffset = rawText.indexOf("<<")
        val nameStart = if (markerOffset >= 0) markerOffset + 2 else 0
        val leadingSpaces = rawText.substring(nameStart).takeWhile { it.isWhitespace() }.length
        val start = nameStart + leadingSpaces
        val end = rawText.length
        if (start >= end) return null

        return BerryCrushNamedParametersReference(
            this,
            TextRange(start, end),
            name,
        )
    }

    override fun getReferences(): Array<PsiReference> = reference?.let { arrayOf(it) } ?: emptyArray()
}

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

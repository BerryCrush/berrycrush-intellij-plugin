package com.berrycrush.intellij.navigation

import com.berrycrush.intellij.language.BerryCrushLanguage
import com.berrycrush.intellij.psi.BerryCrushAssertElement
import com.berrycrush.intellij.psi.BerryCrushCallElement
import com.berrycrush.intellij.psi.BerryCrushElementTypes
import com.berrycrush.intellij.psi.BerryCrushFeatureElement
import com.berrycrush.intellij.psi.BerryCrushFile
import com.berrycrush.intellij.psi.BerryCrushFragmentElement
import com.berrycrush.intellij.psi.BerryCrushIncludeElement
import com.berrycrush.intellij.psi.BerryCrushScenarioElement
import com.berrycrush.intellij.psi.BerryCrushStepElement
import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.breadcrumbs.BreadcrumbsProvider

/**
 * Breadcrumbs provider for BerryCrush files.
 *
 * Shows the current position in the file hierarchy:
 * File > Scenario/Fragment > Step > Directive
 */
class BerryCrushBreadcrumbsProvider : BreadcrumbsProvider {

    override fun getLanguages(): Array<Language> = arrayOf(BerryCrushLanguage)

    override fun acceptElement(element: PsiElement): Boolean {
        // Accept BerryCrush semantic elements
        if (isBlockElement(element) || isStepElement(element) || isDirectiveElement(element)) {
            return true
        }
        // Also accept elements that have a BerryCrush semantic parent
        // This allows breadcrumbs to work when cursor is on tokens inside blocks/steps
        return findSemanticParent(element) != null
    }

    /**
     * Find the nearest semantic BerryCrush element containing this element.
     */
    private fun findSemanticParent(element: PsiElement): PsiElement? {
        var current: PsiElement? = element
        while (current != null && current !is BerryCrushFile) {
            if (isBlockElement(current) || isStepElement(current) || isDirectiveElement(current)) {
                return current
            }
            current = current.parent
        }
        return null
    }

    override fun getElementInfo(element: PsiElement): String {
        // If element is not a semantic element, use its semantic parent for info
        val semanticElement = if (isBlockElement(element) || isStepElement(element) || isDirectiveElement(element)) {
            element
        } else {
            findSemanticParent(element) ?: return element.text.trim().takeWhile { it != '\n' }.take(30)
        }

        return when (semanticElement) {
            is BerryCrushScenarioElement -> "scenario: ${semanticElement.scenarioName ?: ""}".take(40)
            is BerryCrushFragmentElement -> "fragment: ${semanticElement.fragmentName ?: ""}".take(40)
            is BerryCrushFeatureElement -> "feature: ${semanticElement.featureName ?: ""}".take(40)
            is BerryCrushStepElement ->
                "${semanticElement.keyword?.lowercase() ?: ""} ${semanticElement.stepText ?: ""}".trim().take(40)
            is BerryCrushCallElement -> "call ^${semanticElement.operationId ?: ""}"
            is BerryCrushAssertElement -> semanticElement.text.trim().takeWhile { it != '\n' }.take(40)
            is BerryCrushIncludeElement -> "include ${semanticElement.fragmentName ?: ""}"
            else -> {
                // Fallback: check element type
                val elementType = semanticElement.node?.elementType
                when (elementType) {
                    BerryCrushElementTypes.SCENARIO -> extractBlockName(semanticElement, "scenario")
                    BerryCrushElementTypes.FRAGMENT -> extractBlockName(semanticElement, "fragment")
                    BerryCrushElementTypes.FEATURE -> extractBlockName(semanticElement, "feature")
                    BerryCrushElementTypes.STEP -> extractStepName(semanticElement)
                    BerryCrushElementTypes.CALL_DIRECTIVE ->
                        semanticElement.text.trim().takeWhile { it != '\n' }.take(40)
                    BerryCrushElementTypes.ASSERT_DIRECTIVE ->
                        semanticElement.text.trim().takeWhile { it != '\n' }.take(40)
                    BerryCrushElementTypes.INCLUDE_DIRECTIVE ->
                        semanticElement.text.trim().takeWhile { it != '\n' }.take(40)
                    else -> semanticElement.text.trim().takeWhile { it != '\n' }.take(30)
                }
            }
        }
    }

    private fun extractBlockName(element: PsiElement, prefix: String): String {
        val text = element.text
        val colonIndex = text.indexOf(':')
        return if (colonIndex >= 0) {
            val name = text.substring(colonIndex + 1).trim().takeWhile { it != '\n' && it != '\r' }
            "$prefix: ${name.take(30)}"
        } else {
            prefix
        }
    }

    private fun extractStepName(element: PsiElement): String {
        val text = element.text.trim()
        val spaceIndex = text.indexOf(' ')
        return if (spaceIndex >= 0) {
            text.take(40)
        } else {
            text.take(30)
        }
    }

    override fun getElementTooltip(element: PsiElement): String? {
        val text = element.text.trim()
        return if (text.length > 50) {
            text.take(100) + "..."
        } else {
            text
        }
    }

    override fun getParent(element: PsiElement): PsiElement? {
        // First try to find true PSI parent
        val parent = element.parent
        if (parent != null && parent !is BerryCrushFile) {
            // If parent is a valid breadcrumb element, use it
            if (acceptElement(parent)) {
                return parent
            }
        }

        // Directives -> find parent step (check both parent and siblings)
        if (isDirectiveElement(element)) {
            // First check if nested inside a step
            val stepParent = PsiTreeUtil.getParentOfType(element, BerryCrushStepElement::class.java)
            if (stepParent != null) {
                return stepParent
            }
            // Otherwise search siblings
            return findParentElement(element, ::isStepElement)
        }
        // Steps -> find parent block
        if (isStepElement(element)) {
            // First check if nested inside a fragment
            val fragmentParent = PsiTreeUtil.getParentOfType(element, BerryCrushFragmentElement::class.java)
            if (fragmentParent != null) {
                return fragmentParent
            }
            // Otherwise search siblings for scenario
            return findParentElement(element, ::isBlockElement)
        }
        // Blocks -> no parent (file level)
        return null
    }

    /**
     * Find the nearest ancestor element matching the predicate.
     * Since BerryCrush PSI is flat (siblings), we search backwards through siblings.
     */
    private fun findParentElement(element: PsiElement, predicate: (PsiElement) -> Boolean): PsiElement? {
        // Search backwards through siblings
        var current: PsiElement? = element.prevSibling
        while (current != null) {
            if (predicate(current)) {
                return current
            }
            current = current.prevSibling
        }
        return null
    }

    private fun isBlockElement(element: PsiElement): Boolean =
        element is BerryCrushScenarioElement ||
            element is BerryCrushFragmentElement ||
            element is BerryCrushFeatureElement ||
            element.node?.elementType in BLOCK_ELEMENT_TYPES

    private fun isStepElement(element: PsiElement): Boolean =
        element is BerryCrushStepElement ||
            element.node?.elementType == BerryCrushElementTypes.STEP

    private fun isDirectiveElement(element: PsiElement): Boolean =
        element is BerryCrushCallElement ||
            element is BerryCrushAssertElement ||
            element is BerryCrushIncludeElement ||
            element.node?.elementType in DIRECTIVE_ELEMENT_TYPES

    companion object {
        private val BLOCK_ELEMENT_TYPES = setOf(
            BerryCrushElementTypes.FEATURE,
            BerryCrushElementTypes.SCENARIO,
            BerryCrushElementTypes.OUTLINE,
            BerryCrushElementTypes.FRAGMENT,
            BerryCrushElementTypes.BACKGROUND,
            BerryCrushElementTypes.EXAMPLES,
        )

        private val DIRECTIVE_ELEMENT_TYPES = setOf(
            BerryCrushElementTypes.CALL_DIRECTIVE,
            BerryCrushElementTypes.ASSERT_DIRECTIVE,
            BerryCrushElementTypes.EXTRACT_DIRECTIVE,
            BerryCrushElementTypes.INCLUDE_DIRECTIVE,
        )
    }
}

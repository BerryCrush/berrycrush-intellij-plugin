package com.berrycrush.intellij.highlighting

import com.berrycrush.intellij.psi.BerryCrushDirectiveElement
import com.berrycrush.intellij.psi.BerryCrushFeatureElement
import com.berrycrush.intellij.psi.BerryCrushIncludeParameterElement
import com.berrycrush.intellij.psi.BerryCrushJsonPathElement
import com.berrycrush.intellij.psi.BerryCrushOperatorElement
import com.berrycrush.intellij.psi.BerryCrushScenarioElement
import com.berrycrush.intellij.psi.BerryCrushStepElement
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement

/**
 * Annotator for BerryCrush-specific highlighting.
 *
 * Provides context-aware highlighting for elements like parameter keys
 * that can't be distinguished at the lexer level.
 */
class BerryCrushAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is BerryCrushFeatureElement -> annotateKeyword("feature:", element, holder, BerryCrushHighlightingColors.BLOCK_KEYWORD)
            is BerryCrushScenarioElement -> annotateKeyword("scenario:", element, holder, BerryCrushHighlightingColors.BLOCK_KEYWORD)
            is BerryCrushStepElement -> element.keyword?.let {
                annotateKeyword(it, element, holder, BerryCrushHighlightingColors.STEP_KEYWORD)
            }
            is BerryCrushDirectiveElement -> annotateKeyword(element.directiveName, element, holder, BerryCrushHighlightingColors.DIRECTIVE)
            is BerryCrushOperatorElement -> element.operatorName?.let {
                annotateKeyword(it, element, holder, BerryCrushHighlightingColors.OPERATOR)
            }
            is BerryCrushJsonPathElement -> element.jsonPathText?.let {
                annotateKeyword(it, element, holder, BerryCrushHighlightingColors.JSON_PATH)
            }
            is BerryCrushIncludeParameterElement -> annotateParameter(element, holder)
        }
    }

    private fun annotateKeyword(keyword: String, element: PsiElement, holder: AnnotationHolder, attribute: TextAttributesKey) {
        val range = TextRange.create(element.textRange.startOffset, element.textRange.startOffset + keyword.length)
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(range)
            .enforcedTextAttributes(attribute.defaultAttributes)
            .create()
    }

    /**
     * Highlight the parameter name (key before colon) in include parameters.
     */
    private fun annotateParameter(element: BerryCrushIncludeParameterElement, holder: AnnotationHolder) {
        val text = element.text
        val colonIndex = text.indexOf(':')
        if (colonIndex <= 0) return

        // Find the start of the parameter name (skip leading whitespace)
        var start = 0
        while (start < colonIndex && text[start].isWhitespace()) {
            start++
        }

        if (start < colonIndex) {
            val range = TextRange(element.textRange.startOffset + start, element.textRange.startOffset + colonIndex)
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(range)
                .enforcedTextAttributes(
                    BerryCrushHighlightingColors.PARAMETER_KEY.defaultAttributes
                )
                .create()
        }
    }
}

package com.berrycrush.intellij.highlighting

import com.berrycrush.intellij.psi.BerryCrushIncludeParameterElement
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
            is BerryCrushIncludeParameterElement -> annotateParameter(element, holder)
        }
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

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
 */
class BerryCrushAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        annotateLeafToken(element, holder)
        when (element) {
            is BerryCrushIncludeParameterElement -> annotateParameter(element, holder)
        }
    }

    private fun annotateLeafToken(element: PsiElement, holder: AnnotationHolder) {
        if (element.firstChild != null || element.node == null) return

        val tokenKey = BerryCrushTokenHighlighting.keyForToken(element.node.elementType) ?: return
        if (isInNarrativeDescriptionSegment(element)) return
        val range = annotationRange(element, tokenKey)

        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(range)
            .textAttributes(tokenKey)
            .create()
    }

    private fun annotationRange(element: PsiElement, tokenKey: TextAttributesKey): TextRange {
        val keywordRange = keywordOnlyRange(
            text = element.text,
            baseOffset = element.textRange.startOffset,
            tokenKey = tokenKey,
        )
        return keywordRange ?: element.textRange
    }

    private fun isInNarrativeDescriptionSegment(element: PsiElement): Boolean {
        val lineInfo = lineInfo(element) ?: return false
        val descriptionStart = descriptionStartOffset(lineInfo.lineText, lineInfo.lineStartOffset) ?: return false
        return element.textRange.startOffset >= descriptionStart
    }

    private fun lineInfo(element: PsiElement): LineInfo? {
        val fileText = element.containingFile?.text ?: return null
        val elementOffset = element.textRange.startOffset

        val lineStart = fileText.lastIndexOf('\n', startIndex = (elementOffset - 1).coerceAtLeast(0))
            .let { if (it < 0) 0 else it + 1 }
        val lineEndExclusive = fileText.indexOf('\n', startIndex = elementOffset)
            .let { if (it < 0) fileText.length else it }

        return LineInfo(
            lineText = fileText.substring(lineStart, lineEndExclusive),
            lineStartOffset = lineStart,
        )
    }

    private fun descriptionStartOffset(lineText: String, lineStartOffset: Int): Int? {
        val titleMatch = TITLE_LINE_PREFIX_REGEX.find(lineText)
        if (titleMatch != null) {
            return lineStartOffset + titleMatch.range.last + 1
        }

        val stepMatch = STEP_LINE_PREFIX_REGEX.find(lineText)
        if (stepMatch != null) {
            return lineStartOffset + stepMatch.range.last + 1
        }

        return null
    }

    private fun keywordOnlyRange(text: String, baseOffset: Int, tokenKey: TextAttributesKey): TextRange? {
        val match = when (tokenKey) {
            BerryCrushHighlightingColors.BLOCK_KEYWORD -> BLOCK_KEYWORD_REGEX.find(text)
            BerryCrushHighlightingColors.STEP_KEYWORD -> STEP_KEYWORD_REGEX.find(text)
            else -> null
        } ?: return null

        return TextRange(baseOffset + match.range.first, baseOffset + match.range.last + 1)
    }

    private fun annotateParameter(element: BerryCrushIncludeParameterElement, holder: AnnotationHolder) {
        val range = parameterKeyRange(element) ?: return

        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(range)
            .textAttributes(BerryCrushHighlightingColors.PARAMETER_KEY)
            .create()
    }

    /**
     * Highlight the parameter name (key before colon) in include parameters.
     */
    internal fun parameterKeyRange(element: BerryCrushIncludeParameterElement): TextRange? {
        val name = element.parameterName ?: return null
        val offset = element.text.indexOf("$name:")
        if (offset == -1) return null

        return TextRange(element.textRange.startOffset + offset, element.textRange.startOffset + offset + name.length)
    }

    companion object {
        private val BLOCK_KEYWORD_REGEX = Regex("^\\s*(feature:|scenario:|outline:)")
        private val STEP_KEYWORD_REGEX = Regex("^\\s*(given|when|then|and|but):?(?=\\s|$)")
        private val TITLE_LINE_PREFIX_REGEX = BLOCK_KEYWORD_REGEX
        private val STEP_LINE_PREFIX_REGEX = STEP_KEYWORD_REGEX
    }

    private data class LineInfo(
        val lineText: String,
        val lineStartOffset: Int,
    )
}

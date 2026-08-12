package com.berrycrush.intellij.psi

import com.berrycrush.intellij.reference.BerryCrushStringParameterReference
import com.berrycrush.intellij.reference.BerryCrushStringVariableReference
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiReference

/**
 * PSI wrapper for a STRING token with interpolation-aware segmentation.
 */
class BerryCrushStringLiteralElement(
    node: ASTNode,
) : BerryCrushPsiElement(node) {

    val segments: List<BerryCrushStringSegment>
        get() = parseSegments(text)

    override fun getReferences(): Array<PsiReference> = segments
        .filterIsInstance<BerryCrushStringVariableSegment>()
        .mapNotNull { segment ->
            val variableText = segment.rawText.removePrefix("{{").removeSuffix("}}")
            if (variableText.startsWith("param.")) {
                val path = variableText.removePrefix("param.").split(".").filter { it.isNotBlank() }
                if (path.isEmpty()) {
                    null
                } else {
                    BerryCrushStringParameterReference(
                        this,
                        TextRange(segment.contentRange.startOffset + "param.".length, segment.contentRange.endOffset),
                        path,
                    )
                }
            } else if (variableText.isNotBlank()) {
                BerryCrushStringVariableReference(this, segment.contentRange, variableText)
            } else {
                null
            }
        }
        .toTypedArray()

    companion object {
        private const val VARIABLE_START = "{{"
        private const val VARIABLE_END = "}}"

        internal fun parseSegments(sourceText: String): List<BerryCrushStringSegment> {
            val contentStart = openingDelimiterLength(sourceText)
            val contentEnd = sourceText.length - closingDelimiterLength(sourceText)
            if (contentStart >= contentEnd) return emptyList()

            val segments = mutableListOf<BerryCrushStringSegment>()
            var cursor = contentStart

            while (cursor < contentEnd) {
                val variableStart = sourceText.indexOf(VARIABLE_START, cursor)
                if (variableStart == -1 || variableStart >= contentEnd) {
                    appendTextSegments(sourceText, cursor, contentEnd, segments)
                    return segments
                }

                if (variableStart > cursor) {
                    appendTextSegments(sourceText, cursor, variableStart, segments)
                }

                val variableEnd = sourceText.indexOf(VARIABLE_END, variableStart + VARIABLE_START.length)
                if (variableEnd == -1 || variableEnd + VARIABLE_END.length > contentEnd) {
                    appendTextSegments(sourceText, variableStart, contentEnd, segments)
                    return segments
                }

                val rawRange = TextRange(variableStart, variableEnd + VARIABLE_END.length)
                val contentRange = TextRange(variableStart + VARIABLE_START.length, variableEnd)
                val rawText = sourceText.substring(rawRange.startOffset, rawRange.endOffset)
                segments += BerryCrushStringVariableSegment(rawText, rawRange, contentRange)
                cursor = variableEnd + VARIABLE_END.length
            }

            return segments
        }

        private fun appendTextSegments(
            sourceText: String,
            start: Int,
            end: Int,
            target: MutableList<BerryCrushStringSegment>,
        ) {
            if (start >= end) return
            var cursor = start
            var lineStart = start == openingDelimiterLength(sourceText)

            while (cursor < end) {
                if (lineStart && (sourceText[cursor] == ' ' || sourceText[cursor] == '\t')) {
                    val indentStart = cursor
                    while (cursor < end && (sourceText[cursor] == ' ' || sourceText[cursor] == '\t')) {
                        cursor++
                    }
                    target += BerryCrushStringIndentSegment(
                        sourceText.substring(indentStart, cursor),
                        TextRange(indentStart, cursor),
                    )
                    lineStart = false
                } else {
                    val textStart = cursor
                    val run = consumeTextRun(sourceText, cursor, end)
                    cursor = run.nextCursor
                    lineStart = run.nextLineStart
                    if (textStart < cursor) {
                        target += BerryCrushStringTextSegment(
                            sourceText.substring(textStart, cursor),
                            TextRange(textStart, cursor),
                        )
                    }
                }
            }
        }

        private fun consumeTextRun(
            sourceText: String,
            start: Int,
            end: Int,
        ): TextRun {
            var cursor = start
            while (cursor < end && sourceText[cursor] != '\n' && sourceText[cursor] != '\r') {
                cursor++
            }

            if (cursor < end) {
                if (sourceText[cursor] == '\r' && cursor + 1 < end && sourceText[cursor + 1] == '\n') {
                    cursor += 2
                } else {
                    cursor++
                }
                return TextRun(cursor, true)
            }

            return TextRun(cursor, false)
        }

        private fun openingDelimiterLength(text: String): Int = when {
            text.startsWith("\"\"\"") -> 3
            text.startsWith("\"") || text.startsWith("'") -> 1
            else -> 0
        }

        private fun closingDelimiterLength(text: String): Int = when {
            text.endsWith("\"\"\"") -> 3
            text.endsWith("\"") || text.endsWith("'") -> 1
            else -> 0
        }

        private data class TextRun(
            val nextCursor: Int,
            val nextLineStart: Boolean,
        )
    }
}

sealed interface BerryCrushStringSegment {
    val text: String
    val rangeInElement: TextRange
}

data class BerryCrushStringTextSegment(
    override val text: String,
    override val rangeInElement: TextRange,
) : BerryCrushStringSegment

data class BerryCrushStringIndentSegment(
    override val text: String,
    override val rangeInElement: TextRange,
) : BerryCrushStringSegment

data class BerryCrushStringVariableSegment(
    val rawText: String,
    override val rangeInElement: TextRange,
    val contentRange: TextRange,
) : BerryCrushStringSegment {
    override val text: String
        get() = rawText
}

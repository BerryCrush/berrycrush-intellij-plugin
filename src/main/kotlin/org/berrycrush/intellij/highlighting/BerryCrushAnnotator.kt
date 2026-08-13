package org.berrycrush.intellij.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import org.berrycrush.intellij.lexer.BerryCrushTokenTypes
import org.berrycrush.intellij.psi.BerryCrushAssertOperationElement
import org.berrycrush.intellij.psi.BerryCrushBlockElement
import org.berrycrush.intellij.psi.BerryCrushConditionElement
import org.berrycrush.intellij.psi.BerryCrushConditionHolderElement
import org.berrycrush.intellij.psi.BerryCrushDirectiveElement
import org.berrycrush.intellij.psi.BerryCrushExtractElement
import org.berrycrush.intellij.psi.BerryCrushJsonPathElement
import org.berrycrush.intellij.psi.BerryCrushOperatorElement
import org.berrycrush.intellij.psi.BerryCrushParameterKeyElement
import org.berrycrush.intellij.psi.BerryCrushParametersElement
import org.berrycrush.intellij.psi.BerryCrushStepElement
import org.berrycrush.intellij.psi.BerryCrushStringLiteralElement
import org.berrycrush.intellij.psi.BerryCrushTextElement
import org.berrycrush.intellij.psi.BerryCrushVariableRefElement

class BerryCrushAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) = when (element) {
        is BerryCrushBlockElement -> annotateBlockElement(element, holder)
        is BerryCrushParametersElement -> annotateParametersElement(element, holder)
        is BerryCrushStringLiteralElement -> holder.annotate(element, BerryCrushHighlightingColors.STRING)
        is BerryCrushParameterKeyElement -> holder.annotate(element, BerryCrushHighlightingColors.PARAMETER_KEY)
        is BerryCrushStepElement -> element.keyword?.let {
            element.annotateKeyword(holder, it, BerryCrushHighlightingColors.STEP_KEYWORD)
        } ?: Unit
        is BerryCrushVariableRefElement -> holder.annotate(element, BerryCrushHighlightingColors.VARIABLE)
        is BerryCrushDirectiveElement -> annotateDirectiveElement(element, holder)
        is PsiComment -> holder.annotate(element, DefaultLanguageHighlighterColors.LINE_COMMENT)
        else -> checkLiterals(element, holder)
    }
    private fun annotateBlockElement(element: BerryCrushBlockElement, holder: AnnotationHolder) {
        element.annotateKeyword(holder, "${element.keyword}:", BerryCrushHighlightingColors.BLOCK_KEYWORD)
    }

    private fun annotateParametersElement(element: BerryCrushParametersElement, holder: AnnotationHolder) {
        element.annotateKeyword(holder, "parameter:", BerryCrushHighlightingColors.BLOCK_KEYWORD)
    }

    private fun annotateDirectiveElement(element: BerryCrushDirectiveElement, holder: AnnotationHolder) {
        element.annotateKeyword(holder, element.directiveName, BerryCrushHighlightingColors.DIRECTIVE)
        when (element) {
            is BerryCrushConditionHolderElement -> element.condition?.let { annotateCondition(it, holder) }
            is BerryCrushExtractElement -> element.children.forEach { annotateExtractPart(it, holder) }
        }
    }

    private fun annotateCondition(condition: BerryCrushConditionElement, holder: AnnotationHolder) {
        condition.children.forEach {
            when (it) {
                is BerryCrushJsonPathElement -> holder.annotate(it, BerryCrushHighlightingColors.JSON_PATH)
                is BerryCrushOperatorElement -> holder.annotate(it, BerryCrushHighlightingColors.OPERATOR)
                is BerryCrushAssertOperationElement -> holder.annotate(it, BerryCrushHighlightingColors.ASSERTION_KEYWORD)
            }
        }
    }

    private fun annotateExtractPart(element: PsiElement, holder: AnnotationHolder) {
        when (element) {
            is BerryCrushJsonPathElement -> holder.annotate(element, BerryCrushHighlightingColors.JSON_PATH)
            is BerryCrushTextElement -> holder.annotate(element, DefaultLanguageHighlighterColors.IDENTIFIER)
        }
    }

    private fun checkLiterals(element: PsiElement, holder: AnnotationHolder) {
        if (element is BerryCrushTextElement && element.text matches Regex("\\dxx")) {
            holder.annotate(element, BerryCrushHighlightingColors.NUMBER)
        } else {
            when (element.node.elementType) {
                BerryCrushTokenTypes.NUMBER -> holder.annotate(element, BerryCrushHighlightingColors.NUMBER)
            }
        }
    }

    private fun PsiElement.annotateKeyword(holder: AnnotationHolder, keyword: String, textAttributes: TextAttributesKey) {
        val range = TextRange.create(textRange.startOffset, textRange.startOffset + keyword.length)
        holder.annotate(range, textAttributes)
    }

    private fun AnnotationHolder.annotate(element: PsiElement, textAttributes: TextAttributesKey) {
        annotate(element.textRange, textAttributes)
    }

    private fun AnnotationHolder.annotate(range: TextRange, textAttributes: TextAttributesKey) {
        newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(range)
            .textAttributes(textAttributes)
            .create()
    }
}

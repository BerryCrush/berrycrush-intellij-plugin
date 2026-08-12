package org.berrycrush.intellij.parser.impl

import com.intellij.lang.PsiBuilder
import org.berrycrush.intellij.lexer.BerryCrushTokenTypes
import org.berrycrush.intellij.psi.BerryCrushElementTypes

internal fun PsiBuilder.parseStep(stepIndent: Int?) {
    val marker = mark()
    advanceLexer() // consume step keyword (Given/When/Then/And/But)
    skipToEndOfLine()
    stepIndent?.let { parseStepNestedContent(it) }
    marker.done(BerryCrushElementTypes.STEP)
}

internal fun PsiBuilder.parseStepNestedContent(stepIndent: Int) {
    while (!eof()) {
        skipNewlines()

        val indent = currentLineIndent()
        if (indent <= stepIndent) {
            return
        }

        consumeLineIndent()
        when (tokenType) {
            BerryCrushTokenTypes.CALL -> parseCallDirective(indent)
            BerryCrushTokenTypes.WEBHOOK -> parseWebhookDirective(indent)
            BerryCrushTokenTypes.INCLUDE -> parseIncludeDirective(indent)
            BerryCrushTokenTypes.IF -> parseConditionalDirective(indent, BerryCrushElementTypes.IF_DIRECTIVE)
            BerryCrushTokenTypes.ELSE -> parseConditionalDirective(indent, BerryCrushElementTypes.ELSE_DIRECTIVE)
            BerryCrushTokenTypes.ASSERT -> parseAssertDirective()
            BerryCrushTokenTypes.EXTRACT -> parseExtractDirective()
            BerryCrushTokenTypes.GIVEN,
            BerryCrushTokenTypes.WHEN,
            BerryCrushTokenTypes.THEN,
            BerryCrushTokenTypes.AND,
            BerryCrushTokenTypes.BUT,
            -> parseStep(indent)
            else -> skipToEndOfLine()
        }
    }
}

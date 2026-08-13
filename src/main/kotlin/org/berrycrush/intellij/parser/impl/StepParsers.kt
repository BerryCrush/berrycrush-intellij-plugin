package org.berrycrush.intellij.parser.impl

import com.intellij.lang.PsiBuilder
import org.berrycrush.intellij.lexer.BerryCrushTokenTypes
import org.berrycrush.intellij.psi.BerryCrushElementTypes

internal fun PsiBuilder.parseStep(stepIndent: Int?) {
    val marker = mark()
    advanceLexer() // consume step keyword (Given/When/Then/And/But)
    skipToEndOfLine(BerryCrushElementTypes.STEP_DESCRIPTION)
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
            BerryCrushTokenTypes.IF -> parseIfDirective(indent)
            BerryCrushTokenTypes.ASSERT -> parseAssertDirective()
            BerryCrushTokenTypes.EXTRACT -> parseExtractDirective()
            BerryCrushTokenTypes.FAIL -> parseFailDirective()
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

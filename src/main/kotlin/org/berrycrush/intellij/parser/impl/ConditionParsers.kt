package org.berrycrush.intellij.parser.impl

import com.intellij.lang.PsiBuilder
import org.berrycrush.intellij.lexer.BerryCrushTokenTypes
import org.berrycrush.intellij.psi.BerryCrushElementTypes

internal fun PsiBuilder.parseCondition() {
    val marker = mark()
    parseAssertCondition()
    marker.done(BerryCrushElementTypes.CONDITION)
}

private fun PsiBuilder.parseAssertCondition() {
    while (!eof() && tokenType != BerryCrushTokenTypes.NEWLINE) {
        when (tokenType) {
            BerryCrushTokenTypes.JSON_PATH -> markAs(BerryCrushElementTypes.JSON_PATH)
            BerryCrushTokenTypes.NOT -> markAs(BerryCrushElementTypes.NOT) // handle not before keyword
            in BerryCrushTokenTypes.ASSERTION_KEYWORDS -> markAs(BerryCrushElementTypes.ASSERTION_OPERATION)
            BerryCrushTokenTypes.VARIABLE -> markAs(BerryCrushElementTypes.VARIABLE_REF)
            in BerryCrushTokenTypes.TEXTS -> markAs(BerryCrushElementTypes.TEXT)
            in BerryCrushTokenTypes.OPERATORS -> markAs(BerryCrushElementTypes.OPERATOR)
            BerryCrushTokenTypes.STRING -> markAs(BerryCrushElementTypes.STRING_LITERAL)
            else -> markAs(BerryCrushElementTypes.TEXT)
        }
    }
}

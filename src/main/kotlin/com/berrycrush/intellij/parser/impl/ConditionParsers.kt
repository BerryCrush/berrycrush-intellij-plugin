package com.berrycrush.intellij.parser.impl

import com.berrycrush.intellij.lexer.BerryCrushTokenTypes
import com.berrycrush.intellij.psi.BerryCrushElementTypes
import com.intellij.lang.PsiBuilder

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
            else -> markAs(BerryCrushElementTypes.TEXT)
        }
    }
}

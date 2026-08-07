package com.berrycrush.intellij.parser.impl

import com.berrycrush.intellij.lexer.BerryCrushTokenTypes
import com.berrycrush.intellij.psi.BerryCrushElementTypes
import com.berrycrush.intellij.psi.BerryCrushPsiElementType
import com.intellij.lang.PsiBuilder
import com.intellij.psi.tree.IElementType

internal fun PsiBuilder.parseIndentedEntries(
    parentIndent: Int,
    parseEntry: PsiBuilder.(Int) -> Boolean,
) {
    while (!eof()) {
        skipNewlines()

        if (tokenType != BerryCrushTokenTypes.INDENT) {
            return
        }
        // handling comment line
        if (lookAhead(1) == BerryCrushTokenTypes.NEWLINE) {
            advanceLexer()
            continue
        }

        if (currentLineIndent() <= parentIndent) {
            return
        }

        if (!parseEntry(parentIndent)) {
            return
        }
    }
}

internal fun PsiBuilder.skipToEndOfLine(
    type: BerryCrushPsiElementType = BerryCrushElementTypes.TEXT,
    checkVariable: Boolean = true,
) {
    val marker = mark()
    var count = 0
    while (!eof() && tokenType != BerryCrushTokenTypes.NEWLINE) {
        if (checkVariable && tokenType == BerryCrushTokenTypes.VARIABLE) {
            parseVariableRef()
        } else {
            advanceLexer()
        }
        count++
    }
    if (count > 0) {
        marker.done(type)
    } else {
        marker.rollbackTo()
    }
    skipNewlines()
}

internal fun PsiBuilder.markAs(type: BerryCrushPsiElementType) {
    val marker = mark()
    advanceLexer()
    marker.done(type)
}

internal fun PsiBuilder.parseVariableRef() = markAs(BerryCrushElementTypes.VARIABLE_REF)

internal fun PsiBuilder.skipNewlines() {
    tailrec fun check(index: Int): Boolean = when (lookAhead(index)) {
        BerryCrushTokenTypes.NEWLINE -> true
        BerryCrushTokenTypes.WHITE_SPACE -> check(index + 1)
        BerryCrushTokenTypes.INDENT -> check(index + 1)
        else -> false
    }
    while (!eof() && check(0)) {
        while (!eof() && this.tokenType != BerryCrushTokenTypes.NEWLINE) {
            advanceLexer()
        }
        advanceLexer()
    }
}

internal fun PsiBuilder.skipWhiteSpaces() {
    while (tokenType == BerryCrushTokenTypes.WHITE_SPACE) {
        advanceLexer()
    }
}

internal fun PsiBuilder.currentLineIndent(): Int {
    if (tokenType != BerryCrushTokenTypes.INDENT) {
        return 0
    }

    return tokenText?.length ?: 0
}

internal fun PsiBuilder.consumeLineIndent(): Int {
    if (tokenType != BerryCrushTokenTypes.INDENT) {
        return 0
    }

    val indent = tokenText?.length ?: 0
    advanceLexer()
    skipWhiteSpaces()
    return indent
}

internal fun isLineEnd(tokenType: IElementType?): Boolean = tokenType == BerryCrushTokenTypes.NEWLINE || tokenType == null

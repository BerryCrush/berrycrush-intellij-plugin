package com.berrycrush.intellij.parser.impl

import com.berrycrush.intellij.lexer.BerryCrushTokenTypes
import com.berrycrush.intellij.psi.BerryCrushElementTypes
import com.intellij.lang.PsiBuilder

internal fun PsiBuilder.parseFeature(featureIndent: Int) {
    val marker = mark()
    advanceLexer()
    skipToEndOfLine(BerryCrushElementTypes.BLOCK_NAME)

    // Parse optional parameters block after feature name
    tryParseParametersBlock(featureIndent)

    // Parse nested background/scenario blocks
    parseFeatureChildren(featureIndent)

    marker.done(BerryCrushElementTypes.FEATURE)
}

internal fun PsiBuilder.parseTag() {
    markAs(BerryCrushElementTypes.TAG)
    // Tag can be multiple in one line, e.g. @api @get
    // Additionally, other keyword can also be right after the tag
    // e.g. @api @get scenario: bla
    // If the trailing white space exists, then the parseFeatureChildren
    // would stop parsing
    skipWhiteSpaces()
}

private fun PsiBuilder.parseFeatureChildren(featureIndent: Int) {
    fun PsiBuilder.lookAheadBeyondTags(): Boolean {
        tailrec fun check(index: Int): Boolean = !eof() &&
            when (lookAhead(index)) {
                BerryCrushTokenTypes.WHITE_SPACE,
                BerryCrushTokenTypes.NEWLINE,
                BerryCrushTokenTypes.TAG,
                -> check(index + 1)
                BerryCrushTokenTypes.FEATURE -> false
                else -> true
            }
        return check(0)
    }
    while (!eof()) {
        skipNewlines()

        val indent = currentLineIndent()
        if (indent <= featureIndent && tokenType != BerryCrushTokenTypes.TAG) return

        consumeLineIndent()
        // first check if the next token is one of background, scenario or outline
        if (!lookAheadBeyondTags()) return
        // parse tag
        while (!eof() && tokenType == BerryCrushTokenTypes.TAG) {
            parseTag()
        }

        when (tokenType) {
            BerryCrushTokenTypes.BACKGROUND -> parseBackground(indent)
            BerryCrushTokenTypes.SCENARIO -> parseScenario(indent)
            BerryCrushTokenTypes.OUTLINE -> parseOutline(indent)
            else -> skipToEndOfLine()
        }
    }
}

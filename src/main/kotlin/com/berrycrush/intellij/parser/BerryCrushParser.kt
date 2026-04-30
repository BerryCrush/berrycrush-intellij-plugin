package com.berrycrush.intellij.parser

import com.berrycrush.intellij.lexer.BerryCrushTokenTypes
import com.berrycrush.intellij.psi.BerryCrushElementTypes
import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType

/**
 * Parser for BerryCrush language.
 *
 * Creates PSI elements for navigation support (Cmd+Click).
 */
class BerryCrushParser : PsiParser {

    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val rootMarker = builder.mark()

        while (!builder.eof()) {
            parseTopLevel(builder)
        }

        rootMarker.done(root)
        return builder.treeBuilt
    }

    private fun parseTopLevel(builder: PsiBuilder) {
        val tokenType = builder.tokenType

        when (tokenType) {
            BerryCrushTokenTypes.FEATURE -> parseFeature(builder)
            BerryCrushTokenTypes.SCENARIO -> parseScenario(builder)
            BerryCrushTokenTypes.OUTLINE -> parseOutline(builder)
            BerryCrushTokenTypes.FRAGMENT -> parseFragment(builder)
            BerryCrushTokenTypes.BACKGROUND -> parseBackground(builder)
            BerryCrushTokenTypes.CALL -> parseCallDirective(builder)
            BerryCrushTokenTypes.INCLUDE -> parseIncludeDirective(builder)
            BerryCrushTokenTypes.OPERATION_REF -> parseOperationRef(builder)
            else -> builder.advanceLexer()
        }
    }

    private fun parseFeature(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer()
        skipToEndOfLine(builder)
        marker.done(BerryCrushElementTypes.FEATURE)
    }

    private fun parseScenario(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer()
        skipToEndOfLine(builder)
        marker.done(BerryCrushElementTypes.SCENARIO)
    }

    private fun parseOutline(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer()
        skipToEndOfLine(builder)
        marker.done(BerryCrushElementTypes.OUTLINE)
    }

    private fun parseFragment(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer()
        skipToEndOfLine(builder)
        marker.done(BerryCrushElementTypes.FRAGMENT)
    }

    private fun parseBackground(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer()
        skipToEndOfLine(builder)
        marker.done(BerryCrushElementTypes.BACKGROUND)
    }

    private fun parseCallDirective(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer() // consume "call"

        // Look for operation reference
        while (!builder.eof() && !isLineEnd(builder.tokenType)) {
            if (builder.tokenType == BerryCrushTokenTypes.OPERATION_REF) {
                parseOperationRef(builder)
            } else {
                builder.advanceLexer()
            }
        }
        skipNewlines(builder)
        marker.done(BerryCrushElementTypes.CALL_DIRECTIVE)
    }

    private fun parseIncludeDirective(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer() // consume "include"

        // Skip whitespace
        while (builder.tokenType == BerryCrushTokenTypes.WHITE_SPACE) {
            builder.advanceLexer()
        }

        // Parse fragment reference
        val tokenType = builder.tokenType
        if (tokenType == BerryCrushTokenTypes.IDENTIFIER ||
            tokenType == BerryCrushTokenTypes.OPERATION_REF
        ) {
            parseFragmentRef(builder)
        }

        skipToEndOfLine(builder)
        marker.done(BerryCrushElementTypes.INCLUDE_DIRECTIVE)
    }

    private fun parseOperationRef(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer()
        marker.done(BerryCrushElementTypes.OPERATION_REF)
    }

    private fun parseFragmentRef(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer()
        marker.done(BerryCrushElementTypes.FRAGMENT_REF)
    }

    private fun isLineEnd(tokenType: IElementType?): Boolean {
        return tokenType == BerryCrushTokenTypes.NEWLINE || tokenType == null
    }

    private fun skipToEndOfLine(builder: PsiBuilder) {
        while (!builder.eof() && builder.tokenType != BerryCrushTokenTypes.NEWLINE) {
            builder.advanceLexer()
        }
        skipNewlines(builder)
    }

    private fun skipNewlines(builder: PsiBuilder) {
        while (builder.tokenType == BerryCrushTokenTypes.NEWLINE) {
            builder.advanceLexer()
        }
    }
}

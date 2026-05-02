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
            // Step keywords
            BerryCrushTokenTypes.GIVEN,
            BerryCrushTokenTypes.WHEN,
            BerryCrushTokenTypes.THEN,
            BerryCrushTokenTypes.AND,
            BerryCrushTokenTypes.BUT -> parseStep(builder)
            // Assert directive
            BerryCrushTokenTypes.ASSERT -> parseAssertDirective(builder)
            else -> builder.advanceLexer()
        }
    }

    private fun parseStep(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer() // consume step keyword (Given/When/Then/And/But)
        skipToEndOfLine(builder)
        marker.done(BerryCrushElementTypes.STEP)
    }

    private fun parseAssertDirective(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer() // consume "assert"
        skipToEndOfLine(builder)
        marker.done(BerryCrushElementTypes.ASSERT_DIRECTIVE)
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
        builder.advanceLexer() // consume "fragment"
        skipToEndOfLine(builder)

        // Parse all content until next top-level block
        while (!builder.eof() && !isTopLevelKeyword(builder.tokenType)) {
            parseFragmentContent(builder)
        }

        marker.done(BerryCrushElementTypes.FRAGMENT)
    }

    /**
     * Checks if the token type represents a top-level block keyword.
     * These keywords mark the start of a new block and end the current fragment.
     */
    private fun isTopLevelKeyword(tokenType: IElementType?): Boolean =
        tokenType in TOP_LEVEL_KEYWORDS

    /**
     * Parses content within a fragment block (steps, directives, etc.).
     */
    private fun parseFragmentContent(builder: PsiBuilder) {
        when (builder.tokenType) {
            BerryCrushTokenTypes.GIVEN,
            BerryCrushTokenTypes.WHEN,
            BerryCrushTokenTypes.THEN,
            BerryCrushTokenTypes.AND,
            BerryCrushTokenTypes.BUT -> parseStep(builder)
            BerryCrushTokenTypes.CALL -> parseCallDirective(builder)
            BerryCrushTokenTypes.INCLUDE -> parseIncludeDirective(builder)
            BerryCrushTokenTypes.ASSERT -> parseAssertDirective(builder)
            BerryCrushTokenTypes.OPERATION_REF -> parseOperationRef(builder)
            else -> builder.advanceLexer()
        }
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

    companion object {
        /**
         * Keywords that mark the start of a new top-level block.
         * Used to determine fragment boundaries.
         */
        private val TOP_LEVEL_KEYWORDS = setOf(
            BerryCrushTokenTypes.FEATURE,
            BerryCrushTokenTypes.SCENARIO,
            BerryCrushTokenTypes.OUTLINE,
            BerryCrushTokenTypes.FRAGMENT,
        )
    }
}

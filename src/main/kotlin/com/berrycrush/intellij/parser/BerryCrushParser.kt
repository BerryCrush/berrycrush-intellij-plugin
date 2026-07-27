package com.berrycrush.intellij.parser

import com.berrycrush.intellij.lexer.BerryCrushTokenTypes
import com.berrycrush.intellij.psi.BerryCrushElementTypes
import com.berrycrush.intellij.psi.BerryCrushPsiElementType
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
            BerryCrushTokenTypes.WEBHOOK -> parseWebhookDirective(builder)
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
        parseAssertCondition(builder)
        marker.done(BerryCrushElementTypes.ASSERT_DIRECTIVE)
    }

    private fun parseAssertCondition(builder: PsiBuilder) {
        fun mark(type: BerryCrushPsiElementType) {
            val marker = builder.mark()
            marker.done(type)
        }
        while (!builder.eof() && builder.tokenType != BerryCrushTokenTypes.NEWLINE) {
            val tokenType = builder.tokenType
            when (tokenType) {
                BerryCrushTokenTypes.JSON_PATH -> mark(BerryCrushElementTypes.JSON_PATH)
                BerryCrushTokenTypes.NOT -> mark(BerryCrushElementTypes.NOT) // handle not before keyword
                in BerryCrushTokenTypes.ASSERTION_KEYWORDS -> mark(BerryCrushElementTypes.ASSERTION_OPERATION)
                BerryCrushTokenTypes.VARIABLE -> mark(BerryCrushElementTypes.VARIABLE_REF)
                in BerryCrushTokenTypes.TEXTS -> mark(BerryCrushElementTypes.TEXT)
                in BerryCrushTokenTypes.OPERATORS -> mark(BerryCrushElementTypes.OPERATOR)
                else -> mark(BerryCrushElementTypes.TEXT)
            }
            builder.advanceLexer()
        }
    }

    private fun parseFeature(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer()
        skipToEndOfLine(builder)

        // Parse optional parameters block after feature name
        tryParseParametersBlock(builder)

        marker.done(BerryCrushElementTypes.FEATURE)
    }

    private fun parseScenario(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer()
        skipToEndOfLine(builder)

        // Parse optional parameters block after scenario name
        tryParseParametersBlock(builder)

        marker.done(BerryCrushElementTypes.SCENARIO)
    }

    private fun parseOutline(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer()
        skipToEndOfLine(builder)

        // Parse optional parameters block after outline name
        tryParseParametersBlock(builder)

        marker.done(BerryCrushElementTypes.OUTLINE)
    }

    /**
     * Try to parse an optional parameters block.
     * Format: `  parameters:\n    key: value\n    key2: value2`
     */
    private fun tryParseParametersBlock(builder: PsiBuilder) {
        // Check for INDENT followed by PARAMETERS keyword
        if (builder.tokenType != BerryCrushTokenTypes.INDENT) return

        // Look ahead to check if next non-whitespace is PARAMETERS
        val marker = builder.mark()
        builder.advanceLexer() // consume INDENT

        // Skip whitespace
        while (builder.tokenType == BerryCrushTokenTypes.WHITE_SPACE) {
            builder.advanceLexer()
        }

        if (builder.tokenType != BerryCrushTokenTypes.PARAMETERS) {
            // Not a parameters block, rollback
            marker.rollbackTo()
            return
        }

        // It's a parameters block
        val blockMarker = builder.mark()

        builder.advanceLexer() // consume PARAMETERS
        skipToEndOfLine(builder)

        // Parse parameter entries
        while (builder.tokenType == BerryCrushTokenTypes.INDENT) {
            if (!tryParseParameterEntry(builder)) {
                break
            }
        }

        blockMarker.done(BerryCrushElementTypes.PARAMETERS_BLOCK)
        marker.drop()
    }

    /**
     * Try to parse a single parameter entry: `  key: value`
     * Returns true if successfully parsed.
     */
    private fun tryParseParameterEntry(builder: PsiBuilder): Boolean {
        val entryMarker = builder.mark()

        builder.advanceLexer() // consume INDENT

        // Skip whitespace
        while (builder.tokenType == BerryCrushTokenTypes.WHITE_SPACE) {
            builder.advanceLexer()
        }

        // Check for parameter name
        if (builder.tokenType != BerryCrushTokenTypes.IDENTIFIER &&
            builder.tokenType != BerryCrushTokenTypes.TEXT
        ) {
            entryMarker.rollbackTo()
            return false
        }

        builder.advanceLexer() // consume parameter name

        // Skip whitespace before colon
        while (builder.tokenType == BerryCrushTokenTypes.WHITE_SPACE) {
            builder.advanceLexer()
        }

        // Expect colon
        if (builder.tokenType != BerryCrushTokenTypes.COLON) {
            entryMarker.rollbackTo()
            return false
        }

        builder.advanceLexer() // consume colon

        // Parse the value (may contain variable interpolations)
        parseParameterValue(builder)

        skipToEndOfLine(builder)
        entryMarker.done(BerryCrushElementTypes.PARAMETER_ENTRY)
        return true
    }

    /**
     * Parse parameter value which may contain variable interpolations.
     */
    private fun parseParameterValue(builder: PsiBuilder) {
        while (!builder.eof() && builder.tokenType != BerryCrushTokenTypes.NEWLINE) {
            if (builder.tokenType == BerryCrushTokenTypes.VARIABLE) {
                val marker = builder.mark()
                builder.advanceLexer()
                marker.done(BerryCrushElementTypes.VARIABLE_REF)
            } else {
                builder.advanceLexer()
            }
        }
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
            BerryCrushTokenTypes.WEBHOOK -> parseWebhookDirective(builder)
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

        // Parse optional parameter block (same format as include parameters)
        parseIncludeParameters(builder)

        marker.done(BerryCrushElementTypes.CALL_DIRECTIVE)
    }

    private fun parseWebhookDirective(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer() // advance webhook
        while (!builder.eof() && !isLineEnd(builder.tokenType)) {
            if (builder.tokenType == BerryCrushTokenTypes.TEXT) {
                parseWebhookName(builder)
            } else {
                builder.advanceLexer()
            }
        }
        skipNewlines(builder)
        // Parse webhook parameter block (same format as include parameters)
        parseIncludeParameters(builder)
        marker.done(BerryCrushElementTypes.WEBHOOK_DIRECTIVE)
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
            tokenType == BerryCrushTokenTypes.OPERATION_REF ||
            tokenType == BerryCrushTokenTypes.TEXT
        ) {
            parseFragmentRef(builder)
        }

        skipToEndOfLine(builder)

        // Parse optional parameter block (indented key: value pairs)
        parseIncludeParameters(builder)

        marker.done(BerryCrushElementTypes.INCLUDE_DIRECTIVE)
    }

    /**
     * Parse parameter block for include directive.
     * Parameters are indented key: value pairs following the include line.
     */
    private fun parseIncludeParameters(builder: PsiBuilder) {
        // Parse parameters while they exist
        while (builder.tokenType == BerryCrushTokenTypes.INDENT && tryParseParameter(builder)) {
            // Continue parsing parameters
        }
    }

    /**
     * Try to parse a single parameter entry.
     * Returns true if a parameter was successfully parsed, false otherwise.
     */
    private fun tryParseParameter(builder: PsiBuilder): Boolean {
        val paramMarker = builder.mark()
        builder.advanceLexer() // consume INDENT

        // Skip whitespace after indent
        while (builder.tokenType == BerryCrushTokenTypes.WHITE_SPACE) {
            builder.advanceLexer()
        }

        // Check if this looks like a parameter (identifier/text followed by colon)
        val hasParamName =
            builder.tokenType == BerryCrushTokenTypes.IDENTIFIER ||
                builder.tokenType == BerryCrushTokenTypes.TEXT

        if (!hasParamName) {
            paramMarker.rollbackTo()
            return false
        }

        // Parse parameter name
        builder.advanceLexer()

        // Skip whitespace before colon
        while (builder.tokenType == BerryCrushTokenTypes.WHITE_SPACE) {
            builder.advanceLexer()
        }

        // Look for colon
        if (builder.tokenType != BerryCrushTokenTypes.COLON) {
            paramMarker.rollbackTo()
            return false
        }

        builder.advanceLexer() // consume colon

        // Parse the rest of the line as parameter value
        skipToEndOfLine(builder)
        paramMarker.done(BerryCrushElementTypes.PARAMETER)
        return true
    }

    private fun parseWebhookName(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer()
        marker.done(BerryCrushElementTypes.WEBHOOK_NAME)
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
            val marker = builder.mark()
            marker.done(BerryCrushElementTypes.TEXT)
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

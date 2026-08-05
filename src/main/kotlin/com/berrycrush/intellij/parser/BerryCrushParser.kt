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
@Suppress("TooManyFunctions")
class BerryCrushParser : PsiParser {
    override fun parse(
        root: IElementType,
        builder: PsiBuilder,
    ): ASTNode {
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
            BerryCrushTokenTypes.FEATURE -> parseFeature(builder, 0)
            BerryCrushTokenTypes.SCENARIO -> parseScenario(builder, 0)
            BerryCrushTokenTypes.OUTLINE -> parseOutline(builder, 0)
            BerryCrushTokenTypes.FRAGMENT -> parseFragment(builder)
            BerryCrushTokenTypes.BACKGROUND -> parseBackground(builder, 0)
            BerryCrushTokenTypes.PARAMETERS -> parseParameters(builder, 0)
            BerryCrushTokenTypes.CALL -> parseCallDirective(builder, 0)
            BerryCrushTokenTypes.WEBHOOK -> parseWebhookDirective(builder, 0)
            BerryCrushTokenTypes.INCLUDE -> parseIncludeDirective(builder, 0)
            BerryCrushTokenTypes.OPERATION_REF -> parseOperationRef(builder)
            BerryCrushTokenTypes.EXTRACT -> parseExtractDirective(builder)
            // Step keywords
            BerryCrushTokenTypes.GIVEN,
            BerryCrushTokenTypes.WHEN,
            BerryCrushTokenTypes.THEN,
            BerryCrushTokenTypes.AND,
            BerryCrushTokenTypes.BUT,
            -> parseStep(builder, 0)
            // Assert directive
            BerryCrushTokenTypes.ASSERT -> parseAssertDirective(builder)
            BerryCrushTokenTypes.INDENT -> {
                builder.advanceLexer()
                skipToEndOfLine(builder)
            }
            BerryCrushTokenTypes.COMMENT -> skipToEndOfLine(builder, BerryCrushElementTypes.COMMENT, false)
            else -> builder.advanceLexer()
        }
    }

    private fun parseStep(
        builder: PsiBuilder,
        stepIndent: Int?,
    ) {
        val marker = builder.mark()
        builder.advanceLexer() // consume step keyword (Given/When/Then/And/But)
        skipToEndOfLine(builder)
        stepIndent?.let { parseStepNestedContent(builder, it) }
        marker.done(BerryCrushElementTypes.STEP)
    }

    private fun parseStepNestedContent(
        builder: PsiBuilder,
        stepIndent: Int,
    ) {
        while (!builder.eof()) {
            skipNewlines(builder)

            val indent = currentLineIndent(builder)
            if (indent <= stepIndent) {
                return
            }

            consumeLineIndent(builder)
            when (builder.tokenType) {
                BerryCrushTokenTypes.CALL -> parseCallDirective(builder, indent)
                BerryCrushTokenTypes.WEBHOOK -> parseWebhookDirective(builder, indent)
                BerryCrushTokenTypes.INCLUDE -> parseIncludeDirective(builder, indent)
                BerryCrushTokenTypes.ASSERT -> parseAssertDirective(builder)
                BerryCrushTokenTypes.EXTRACT -> parseExtractDirective(builder)
                BerryCrushTokenTypes.GIVEN,
                BerryCrushTokenTypes.WHEN,
                BerryCrushTokenTypes.THEN,
                BerryCrushTokenTypes.AND,
                BerryCrushTokenTypes.BUT,
                -> parseStep(builder, indent)
                BerryCrushTokenTypes.COMMENT -> skipToEndOfLine(builder, BerryCrushElementTypes.COMMENT, false)
                else -> skipToEndOfLine(builder)
            }
        }
    }

    private fun parseExtractDirective(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer() // consume "extract"
        parseExtractVariable(builder)
        marker.done(BerryCrushElementTypes.EXTRACT_DIRECTIVE)
    }

    private fun parseExtractVariable(builder: PsiBuilder) {
        while (!builder.eof() && builder.tokenType != BerryCrushTokenTypes.NEWLINE) {
            val tokenType = builder.tokenType
            when (tokenType) {
                BerryCrushTokenTypes.JSON_PATH -> builder.mark().done(BerryCrushElementTypes.JSON_PATH)
                BerryCrushTokenTypes.VARIABLE -> builder.mark().done(BerryCrushElementTypes.VARIABLE_REF)
                BerryCrushTokenTypes.ARRAY_SIZE -> builder.mark().done(BerryCrushElementTypes.ARROW)
            }
            builder.advanceLexer()
        }
    }

    private fun parseAssertDirective(builder: PsiBuilder) {
        val marker = builder.mark()
        builder.advanceLexer() // consume "assert"
        parseAssertCondition(builder)
        marker.done(BerryCrushElementTypes.ASSERT_DIRECTIVE)
    }

    private fun parseAssertCondition(builder: PsiBuilder) {
        fun mark(type: BerryCrushPsiElementType) {
            builder.mark().done(type)
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

    private fun parseFeature(
        builder: PsiBuilder,
        featureIndent: Int,
    ) {
        val marker = builder.mark()
        builder.advanceLexer()
        skipToEndOfLine(builder)

        // Parse optional parameters block after feature name
        tryParseParametersBlock(builder, featureIndent)

        // Parse nested background/scenario blocks
        parseFeatureChildren(builder, featureIndent)

        marker.done(BerryCrushElementTypes.FEATURE)
    }

    private fun parseFeatureChildren(
        builder: PsiBuilder,
        featureIndent: Int,
    ) {
        while (!builder.eof()) {
            skipNewlines(builder)

            val indent = currentLineIndent(builder)
            if (indent <= featureIndent) {
                return
            }

            consumeLineIndent(builder)
            when (builder.tokenType) {
                BerryCrushTokenTypes.BACKGROUND -> parseBackground(builder, indent)
                BerryCrushTokenTypes.SCENARIO -> parseScenario(builder, indent)
                BerryCrushTokenTypes.OUTLINE -> parseOutline(builder, indent)
                BerryCrushTokenTypes.COMMENT -> skipToEndOfLine(builder, BerryCrushElementTypes.COMMENT, false)
                else -> skipToEndOfLine(builder)
            }
        }
    }

    private fun parseScenario(
        builder: PsiBuilder,
        scenarioIndent: Int,
    ) {
        val marker = builder.mark()
        builder.advanceLexer()
        skipToEndOfLine(builder)

        // Parse optional parameters block after scenario name
        tryParseParametersBlock(builder, scenarioIndent)
        parseScenarioContent(builder, scenarioIndent)

        marker.done(BerryCrushElementTypes.SCENARIO)
    }

    private fun parseOutline(
        builder: PsiBuilder,
        outlineIndent: Int,
    ) {
        val marker = builder.mark()
        builder.advanceLexer()
        skipToEndOfLine(builder)

        // Parse optional parameters block after outline name
        tryParseParametersBlock(builder, outlineIndent)
        parseScenarioContent(builder, outlineIndent)

        marker.done(BerryCrushElementTypes.OUTLINE)
    }

    private fun parseBackground(
        builder: PsiBuilder,
        backgroundIndent: Int,
    ) {
        val marker = builder.mark()
        builder.advanceLexer()
        skipToEndOfLine(builder)
        parseScenarioContent(builder, backgroundIndent)
        marker.done(BerryCrushElementTypes.BACKGROUND)
    }

    private fun parseParameters(
        builder: PsiBuilder,
        indent: Int,
    ) {
        // It's a parameters block
        val blockMarker = builder.mark()

        builder.advanceLexer() // consume PARAMETERS
        skipToEndOfLine(builder)

        // Parse parameter entries
        parseIndentedEntries(builder, indent, ::tryParseParameterEntry)
        blockMarker.done(BerryCrushElementTypes.PARAMETERS)
    }

    private fun parseScenarioContent(
        builder: PsiBuilder,
        parentIndent: Int,
    ) {
        while (!builder.eof()) {
            skipNewlines(builder)

            val indent = currentLineIndent(builder)
            if (indent <= parentIndent) {
                return
            }

            consumeLineIndent(builder)
            when (builder.tokenType) {
                BerryCrushTokenTypes.GIVEN,
                BerryCrushTokenTypes.WHEN,
                BerryCrushTokenTypes.THEN,
                BerryCrushTokenTypes.AND,
                BerryCrushTokenTypes.BUT,
                -> parseStep(builder, indent)
                BerryCrushTokenTypes.CALL -> parseCallDirective(builder, indent)
                BerryCrushTokenTypes.WEBHOOK -> parseWebhookDirective(builder, indent)
                BerryCrushTokenTypes.INCLUDE -> parseIncludeDirective(builder, indent)
                BerryCrushTokenTypes.ASSERT -> parseAssertDirective(builder)
                BerryCrushTokenTypes.BACKGROUND -> parseBackground(builder, indent)
                BerryCrushTokenTypes.SCENARIO -> parseScenario(builder, indent)
                BerryCrushTokenTypes.COMMENT -> skipToEndOfLine(builder, BerryCrushElementTypes.COMMENT, false)
                else -> skipToEndOfLine(builder)
            }
        }
    }

    /**
     * Try to parse an optional parameters block.
     * Format:
     * ```
     * parameters:
     *   key: value
     *   key2: value2
     * ```
     * possibly indentation before
     */
    private fun tryParseParametersBlock(
        builder: PsiBuilder,
        parentIndent: Int,
    ) {
        // Check for INDENT followed by PARAMETERS keyword
        if (builder.tokenType != BerryCrushTokenTypes.INDENT) return

        val indent = currentLineIndent(builder)
        if (indent <= parentIndent) return

        // Look ahead to check if next non-whitespace is PARAMETERS
        val marker = builder.mark()
        consumeLineIndent(builder)

        // Skip whitespace
        while (builder.tokenType == BerryCrushTokenTypes.WHITE_SPACE) {
            builder.advanceLexer()
        }

        if (builder.tokenType != BerryCrushTokenTypes.PARAMETERS) {
            // Not a parameters block, rollback
            marker.rollbackTo()
            return
        }
        parseParameters(builder, parentIndent)
        marker.drop()
    }

    /**
     * Try to parse a single parameter entry: `  key: value`
     * Returns true if successfully parsed.
     */
    private fun tryParseParameterEntry(
        builder: PsiBuilder,
        parentIndent: Int,
    ): Boolean = tryParseParameterLike(
        builder,
        parentIndent,
        allowBody = false,
    )

    /**
     * Parse parameter value which may contain variable interpolations.
     */
    private fun parseParameterValue(
        builder: PsiBuilder,
        indent: Int,
        allowBody: Boolean,
    ) {
        val hasInlineValue = builder.tokenType != BerryCrushTokenTypes.NEWLINE && !builder.eof()
        val valueMarker = builder.mark()
        if (hasInlineValue) {
            while (!builder.eof() && builder.tokenType != BerryCrushTokenTypes.NEWLINE) {
                when (builder.tokenType) {
                    BerryCrushTokenTypes.VARIABLE -> {
                        val marker = builder.mark()
                        builder.advanceLexer()
                        marker.done(BerryCrushElementTypes.VARIABLE_REF)
                    }
                    else -> {
                        builder.advanceLexer()
                    }
                }
            }
        } else {
            skipNewlines(builder)
            // check nested
            parseIndentedEntries(builder, indent) { b, childParentIndent ->
                tryParseParameterLike(
                    b,
                    childParentIndent,
                    allowBody,
                )
            }
        }

        valueMarker.done(BerryCrushElementTypes.PARAMETER_VALUE)
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
    private fun isTopLevelKeyword(tokenType: IElementType?): Boolean = tokenType in TOP_LEVEL_KEYWORDS

    /**
     * Parses content within a fragment block (steps, directives, etc.).
     */
    private fun parseFragmentContent(builder: PsiBuilder) {
        when (builder.tokenType) {
            BerryCrushTokenTypes.GIVEN,
            BerryCrushTokenTypes.WHEN,
            BerryCrushTokenTypes.THEN,
            BerryCrushTokenTypes.AND,
            BerryCrushTokenTypes.BUT,
            -> parseStep(builder, null)
            BerryCrushTokenTypes.CALL -> parseCallDirective(builder, 0)
            BerryCrushTokenTypes.WEBHOOK -> parseWebhookDirective(builder, 0)
            BerryCrushTokenTypes.INCLUDE -> parseIncludeDirective(builder, 0)
            BerryCrushTokenTypes.ASSERT -> parseAssertDirective(builder)
            BerryCrushTokenTypes.OPERATION_REF -> parseOperationRef(builder)
            else -> builder.advanceLexer()
        }
    }

    private fun parseCallDirective(
        builder: PsiBuilder,
        parentIndent: Int,
    ) {
        val marker = builder.mark()
        builder.advanceLexer() // consume "call"

        // Look for operation reference
        while (!builder.eof() && !isLineEnd(builder.tokenType)) {
            if (builder.tokenType == BerryCrushTokenTypes.OPERATION_REF) {
                parseOperationRef(builder)
            } else if (builder.tokenType == BerryCrushTokenTypes.VARIABLE) {
                parseVariableRef(builder)
            } else {
                builder.advanceLexer()
            }
        }
        skipNewlines(builder)

        // Parse optional parameter block (same format as include parameters)
        parseIncludeParameters(builder, parentIndent)

        marker.done(BerryCrushElementTypes.CALL_DIRECTIVE)
    }

    private fun parseWebhookDirective(
        builder: PsiBuilder,
        parentIndent: Int,
    ) {
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
        parseIncludeParameters(builder, parentIndent)
        marker.done(BerryCrushElementTypes.WEBHOOK_DIRECTIVE)
    }

    private fun parseIncludeDirective(
        builder: PsiBuilder,
        parentIndent: Int,
    ) {
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
        parseIncludeParameters(builder, parentIndent)

        marker.done(BerryCrushElementTypes.INCLUDE_DIRECTIVE)
    }

    private fun parseIndentedEntries(
        builder: PsiBuilder,
        parentIndent: Int,
        parseEntry: (PsiBuilder, Int) -> Boolean,
    ) {
        while (builder.tokenType == BerryCrushTokenTypes.INDENT && parseEntry(builder, parentIndent)) {
            skipNewlines(builder)
        }
    }

    /**
     * Parse parameter block for include directive.
     * Parameters are indented key: value pairs following the include line.
     */
    private fun parseIncludeParameters(
        builder: PsiBuilder,
        parentIndent: Int,
    ) {
        if (builder.tokenType == BerryCrushTokenTypes.INDENT) {
            val marker = builder.mark()
            parseIndentedEntries(builder, parentIndent, ::tryParseParameter)
            marker.done(BerryCrushElementTypes.INCLUDED_PARAMETER)
        }
    }

    /**
     * Try to parse a single parameter entry.
     * Returns true if a parameter was successfully parsed, false otherwise.
     */
    private fun tryParseParameter(
        builder: PsiBuilder,
        parentIndent: Int,
    ): Boolean = tryParseParameterLike(
        builder,
        parentIndent,
        allowBody = true,
    )

    private fun tryParseParameterLike(
        builder: PsiBuilder,
        parentIndent: Int,
        allowBody: Boolean,
    ): Boolean {
        val indent = currentLineIndent(builder)
        if (indent <= parentIndent) {
            return false
        }

        val marker = builder.mark()
        consumeLineIndent(builder)

        when (builder.tokenType) {
            BerryCrushTokenTypes.BODY -> {
                if (!allowBody) {
                    marker.rollbackTo()
                    return false
                }
                val keyMarker = builder.mark()
                builder.advanceLexer()
                // BODY token already includes the trailing colon ("body:")
                keyMarker.done(BerryCrushElementTypes.PARAMETER_KEY)
            }

            BerryCrushTokenTypes.IDENTIFIER,
            BerryCrushTokenTypes.TEXT,
            -> {
                val keyMarker = builder.mark()
                builder.advanceLexer()
                while (builder.tokenType == BerryCrushTokenTypes.WHITE_SPACE) {
                    builder.advanceLexer()
                }

                if (builder.tokenType != BerryCrushTokenTypes.COLON) {
                    keyMarker.rollbackTo()
                    marker.rollbackTo()
                    return false
                }
                builder.advanceLexer() // consume ':'
                keyMarker.done(BerryCrushElementTypes.PARAMETER_KEY)
            }

            else -> {
                marker.rollbackTo()
                return false
            }
        }

        while (builder.tokenType == BerryCrushTokenTypes.WHITE_SPACE) {
            builder.advanceLexer()
        }

        parseParameterValue(builder, indent, allowBody)
        marker.done(BerryCrushElementTypes.PARAMETER_ENTRY)
        return true
    }

    private fun parseWebhookName(builder: PsiBuilder) = builder.markAs(BerryCrushElementTypes.WEBHOOK_NAME)

    private fun parseVariableRef(builder: PsiBuilder) = builder.markAs(BerryCrushElementTypes.VARIABLE_REF)

    private fun parseOperationRef(builder: PsiBuilder) = builder.markAs(BerryCrushElementTypes.OPERATION_REF)

    private fun parseFragmentRef(builder: PsiBuilder) = builder.markAs(BerryCrushElementTypes.FRAGMENT_REF)

    private fun PsiBuilder.markAs(type: BerryCrushPsiElementType) {
        val marker = mark()
        advanceLexer()
        marker.done(type)
    }

    private fun currentLineIndent(builder: PsiBuilder): Int {
        if (builder.tokenType != BerryCrushTokenTypes.INDENT) {
            return 0
        }

        return builder.tokenText?.length ?: 0
    }

    private fun consumeLineIndent(builder: PsiBuilder): Int {
        if (builder.tokenType != BerryCrushTokenTypes.INDENT) {
            return 0
        }

        val indent = builder.tokenText?.length ?: 0
        builder.advanceLexer()
        while (builder.tokenType == BerryCrushTokenTypes.WHITE_SPACE) {
            builder.advanceLexer()
        }
        return indent
    }

    private fun skipToEndOfLine(
        builder: PsiBuilder,
        type: BerryCrushPsiElementType = BerryCrushElementTypes.TEXT,
        checkVariable: Boolean = true,
    ) {
        val marker = builder.mark()
        var count = 0
        while (!builder.eof() && builder.tokenType != BerryCrushTokenTypes.NEWLINE) {
            if (checkVariable && builder.tokenType == BerryCrushTokenTypes.VARIABLE) {
                parseVariableRef(builder)
            } else {
                builder.advanceLexer()
            }
            count++
        }
        if (count > 0) {
            marker.done(type)
        } else {
            marker.rollbackTo()
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
        private val TOP_LEVEL_KEYWORDS =
            setOf(
                BerryCrushTokenTypes.FEATURE,
                BerryCrushTokenTypes.SCENARIO,
                BerryCrushTokenTypes.OUTLINE,
                BerryCrushTokenTypes.FRAGMENT,
            )
    }
}

private fun isLineEnd(tokenType: IElementType?): Boolean = tokenType == BerryCrushTokenTypes.NEWLINE || tokenType == null

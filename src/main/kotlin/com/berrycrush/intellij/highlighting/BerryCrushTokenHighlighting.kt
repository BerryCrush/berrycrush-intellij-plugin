package com.berrycrush.intellij.highlighting

import com.berrycrush.intellij.lexer.BerryCrushTokenTypes
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.tree.IElementType

/**
 * Shared token-to-color mapping used by both syntax highlighter and annotator.
 */
object BerryCrushTokenHighlighting {
    private val emptyKeys = emptyArray<TextAttributesKey>()

    private val keyByToken: Map<IElementType, TextAttributesKey> =
        buildMap {
            listOf(
                BerryCrushTokenTypes.FEATURE,
                BerryCrushTokenTypes.SCENARIO,
                BerryCrushTokenTypes.OUTLINE,
                BerryCrushTokenTypes.FRAGMENT,
                BerryCrushTokenTypes.PARAMETERS,
                BerryCrushTokenTypes.BACKGROUND,
                BerryCrushTokenTypes.EXAMPLES,
            ).forEach { put(it, BerryCrushHighlightingColors.BLOCK_KEYWORD) }

            listOf(
                BerryCrushTokenTypes.GIVEN,
                BerryCrushTokenTypes.WHEN,
                BerryCrushTokenTypes.THEN,
                BerryCrushTokenTypes.AND,
                BerryCrushTokenTypes.BUT,
            ).forEach { put(it, BerryCrushHighlightingColors.STEP_KEYWORD) }

            listOf(
                BerryCrushTokenTypes.CALL,
                BerryCrushTokenTypes.WEBHOOK,
                BerryCrushTokenTypes.ASSERT,
                BerryCrushTokenTypes.EXTRACT,
                BerryCrushTokenTypes.INCLUDE,
                BerryCrushTokenTypes.BODY,
                BerryCrushTokenTypes.IF,
                BerryCrushTokenTypes.ELSE,
                BerryCrushTokenTypes.FAIL,
            ).forEach { put(it, BerryCrushHighlightingColors.DIRECTIVE) }

            listOf(
                BerryCrushTokenTypes.STATUS,
                BerryCrushTokenTypes.HEADER,
                BerryCrushTokenTypes.CONTAINS,
                BerryCrushTokenTypes.SCHEMA,
                BerryCrushTokenTypes.RESPONSE_TIME,
                BerryCrushTokenTypes.EXISTS,
                BerryCrushTokenTypes.NOT,
                BerryCrushTokenTypes.IN,
                BerryCrushTokenTypes.HAS_SIZE,
                BerryCrushTokenTypes.ARRAY_SIZE,
                BerryCrushTokenTypes.EMPTY,
                BerryCrushTokenTypes.NOT_EMPTY,
            ).forEach { put(it, BerryCrushHighlightingColors.ASSERTION_KEYWORD) }

            listOf(
                BerryCrushTokenTypes.EQUALS,
                BerryCrushTokenTypes.NOT_EQUALS,
                BerryCrushTokenTypes.GREATER_THAN,
                BerryCrushTokenTypes.LESS_THAN,
                BerryCrushTokenTypes.GREATER_OR_EQUAL,
                BerryCrushTokenTypes.LESS_OR_EQUAL,
                BerryCrushTokenTypes.MATCHES,
                BerryCrushTokenTypes.STARTS_WITH,
                BerryCrushTokenTypes.ENDS_WITH,
                BerryCrushTokenTypes.SIZE,
            ).forEach { put(it, BerryCrushHighlightingColors.OPERATOR) }

            put(BerryCrushTokenTypes.TAG, BerryCrushHighlightingColors.TAG)
            put(BerryCrushTokenTypes.OPERATION_REF, BerryCrushHighlightingColors.OPERATION_REF)
            put(BerryCrushTokenTypes.VARIABLE, BerryCrushHighlightingColors.VARIABLE)
            put(BerryCrushTokenTypes.JSON_PATH, BerryCrushHighlightingColors.JSON_PATH)
            put(BerryCrushTokenTypes.STRING, BerryCrushHighlightingColors.STRING)
            put(BerryCrushTokenTypes.NUMBER, BerryCrushHighlightingColors.NUMBER)
            put(BerryCrushTokenTypes.BOOLEAN, BerryCrushHighlightingColors.NUMBER)
            put(BerryCrushTokenTypes.NULL, BerryCrushHighlightingColors.NUMBER)
            put(BerryCrushTokenTypes.COMMENT, BerryCrushHighlightingColors.COMMENT)
            put(BerryCrushTokenTypes.LBRACE, BerryCrushHighlightingColors.BRACES)
            put(BerryCrushTokenTypes.RBRACE, BerryCrushHighlightingColors.BRACES)
            put(BerryCrushTokenTypes.LBRACKET, BerryCrushHighlightingColors.BRACES)
            put(BerryCrushTokenTypes.RBRACKET, BerryCrushHighlightingColors.BRACES)
            put(BerryCrushTokenTypes.PIPE, BerryCrushHighlightingColors.PIPE)
            put(BerryCrushTokenTypes.BAD_CHARACTER, BerryCrushHighlightingColors.BAD_CHARACTER)
        }

    private val keysByToken: Map<IElementType, Array<TextAttributesKey>> =
        keyByToken.mapValues { arrayOf(it.value) }

    fun keyForToken(tokenType: IElementType?): TextAttributesKey? = tokenType?.let { keyByToken[it] }

    fun keysForToken(tokenType: IElementType?): Array<TextAttributesKey> = tokenType?.let { keysByToken[it] } ?: emptyKeys
}

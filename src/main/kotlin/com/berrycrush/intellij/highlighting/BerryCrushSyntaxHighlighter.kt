package com.berrycrush.intellij.highlighting

import com.berrycrush.intellij.lexer.BerryCrushLexer
import com.berrycrush.intellij.lexer.BerryCrushTokenTypes
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

/**
 * Syntax highlighter for BerryCrush language.
 */
class BerryCrushSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer = BerryCrushLexer()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        if (tokenType == null) return EMPTY_KEYS

        return when (tokenType) {
            // Block keywords
            BerryCrushTokenTypes.FEATURE,
            BerryCrushTokenTypes.SCENARIO,
            BerryCrushTokenTypes.OUTLINE,
            BerryCrushTokenTypes.FRAGMENT,
            BerryCrushTokenTypes.PARAMETERS,
            BerryCrushTokenTypes.BACKGROUND,
            BerryCrushTokenTypes.EXAMPLES -> BLOCK_KEYWORD_KEYS

            // Step keywords
            BerryCrushTokenTypes.GIVEN,
            BerryCrushTokenTypes.WHEN,
            BerryCrushTokenTypes.THEN,
            BerryCrushTokenTypes.AND,
            BerryCrushTokenTypes.BUT -> STEP_KEYWORD_KEYS

            // Directives
            BerryCrushTokenTypes.CALL,
            BerryCrushTokenTypes.ASSERT,
            BerryCrushTokenTypes.EXTRACT,
            BerryCrushTokenTypes.INCLUDE,
            BerryCrushTokenTypes.BODY,
            BerryCrushTokenTypes.IF,
            BerryCrushTokenTypes.ELSE,
            BerryCrushTokenTypes.FAIL -> DIRECTIVE_KEYS

            // Assertion keywords
            BerryCrushTokenTypes.STATUS,
            BerryCrushTokenTypes.HEADER,
            BerryCrushTokenTypes.CONTAINS,
            BerryCrushTokenTypes.SCHEMA,
            BerryCrushTokenTypes.RESPONSE_TIME,
            BerryCrushTokenTypes.EXISTS,
            BerryCrushTokenTypes.NOT -> ASSERTION_KEYWORD_KEYS

            // Operators
            BerryCrushTokenTypes.EQUALS,
            BerryCrushTokenTypes.NOT_EQUALS,
            BerryCrushTokenTypes.GREATER_THAN,
            BerryCrushTokenTypes.LESS_THAN,
            BerryCrushTokenTypes.GREATER_OR_EQUAL,
            BerryCrushTokenTypes.LESS_OR_EQUAL,
            BerryCrushTokenTypes.MATCHES,
            BerryCrushTokenTypes.STARTS_WITH,
            BerryCrushTokenTypes.ENDS_WITH -> OPERATOR_KEYS

            // References
            BerryCrushTokenTypes.TAG -> TAG_KEYS
            BerryCrushTokenTypes.OPERATION_REF -> OPERATION_REF_KEYS
            BerryCrushTokenTypes.VARIABLE -> VARIABLE_KEYS
            BerryCrushTokenTypes.JSON_PATH -> JSON_PATH_KEYS

            // Literals
            BerryCrushTokenTypes.STRING -> STRING_KEYS
            BerryCrushTokenTypes.NUMBER,
            BerryCrushTokenTypes.BOOLEAN,
            BerryCrushTokenTypes.NULL -> NUMBER_KEYS

            // Comments
            BerryCrushTokenTypes.COMMENT -> COMMENT_KEYS

            // Braces
            BerryCrushTokenTypes.LBRACE,
            BerryCrushTokenTypes.RBRACE,
            BerryCrushTokenTypes.LBRACKET,
            BerryCrushTokenTypes.RBRACKET -> BRACES_KEYS

            // Pipe
            BerryCrushTokenTypes.PIPE -> PIPE_KEYS

            // Bad character
            BerryCrushTokenTypes.BAD_CHARACTER -> BAD_CHARACTER_KEYS

            else -> EMPTY_KEYS
        }
    }

    companion object {
        private val EMPTY_KEYS = emptyArray<TextAttributesKey>()
        private val BLOCK_KEYWORD_KEYS = arrayOf(BerryCrushHighlightingColors.BLOCK_KEYWORD)
        private val STEP_KEYWORD_KEYS = arrayOf(BerryCrushHighlightingColors.STEP_KEYWORD)
        private val DIRECTIVE_KEYS = arrayOf(BerryCrushHighlightingColors.DIRECTIVE)
        private val ASSERTION_KEYWORD_KEYS = arrayOf(BerryCrushHighlightingColors.ASSERTION_KEYWORD)
        private val OPERATOR_KEYS = arrayOf(BerryCrushHighlightingColors.OPERATOR)
        private val TAG_KEYS = arrayOf(BerryCrushHighlightingColors.TAG)
        private val OPERATION_REF_KEYS = arrayOf(BerryCrushHighlightingColors.OPERATION_REF)
        private val VARIABLE_KEYS = arrayOf(BerryCrushHighlightingColors.VARIABLE)
        private val JSON_PATH_KEYS = arrayOf(BerryCrushHighlightingColors.JSON_PATH)
        private val STRING_KEYS = arrayOf(BerryCrushHighlightingColors.STRING)
        private val NUMBER_KEYS = arrayOf(BerryCrushHighlightingColors.NUMBER)
        private val COMMENT_KEYS = arrayOf(BerryCrushHighlightingColors.COMMENT)
        private val BRACES_KEYS = arrayOf(BerryCrushHighlightingColors.BRACES)
        private val PIPE_KEYS = arrayOf(BerryCrushHighlightingColors.PIPE)
        private val BAD_CHARACTER_KEYS = arrayOf(BerryCrushHighlightingColors.BAD_CHARACTER)
    }
}

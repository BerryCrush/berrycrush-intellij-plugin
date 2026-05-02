package com.berrycrush.intellij.lexer

import com.berrycrush.intellij.language.BerryCrushLanguage
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

/**
 * Token types for the BerryCrush lexer.
 */
object BerryCrushTokenTypes {
    // Block keywords
    @JvmField val FEATURE = BerryCrushElementType("FEATURE")
    @JvmField val SCENARIO = BerryCrushElementType("SCENARIO")
    @JvmField val OUTLINE = BerryCrushElementType("OUTLINE")
    @JvmField val FRAGMENT = BerryCrushElementType("FRAGMENT")
    @JvmField val PARAMETERS = BerryCrushElementType("PARAMETERS")
    @JvmField val BACKGROUND = BerryCrushElementType("BACKGROUND")
    @JvmField val EXAMPLES = BerryCrushElementType("EXAMPLES")

    // Step keywords
    @JvmField val GIVEN = BerryCrushElementType("GIVEN")
    @JvmField val WHEN = BerryCrushElementType("WHEN")
    @JvmField val THEN = BerryCrushElementType("THEN")
    @JvmField val AND = BerryCrushElementType("AND")
    @JvmField val BUT = BerryCrushElementType("BUT")

    // Directives
    @JvmField val CALL = BerryCrushElementType("CALL")
    @JvmField val ASSERT = BerryCrushElementType("ASSERT")
    @JvmField val EXTRACT = BerryCrushElementType("EXTRACT")
    @JvmField val INCLUDE = BerryCrushElementType("INCLUDE")
    @JvmField val BODY = BerryCrushElementType("BODY")
    @JvmField val IF = BerryCrushElementType("IF")
    @JvmField val ELSE = BerryCrushElementType("ELSE")
    @JvmField val FAIL = BerryCrushElementType("FAIL")

    // Assertion keywords
    @JvmField val STATUS = BerryCrushElementType("STATUS")
    @JvmField val HEADER = BerryCrushElementType("HEADER")
    @JvmField val CONTAINS = BerryCrushElementType("CONTAINS")
    @JvmField val SCHEMA = BerryCrushElementType("SCHEMA")
    @JvmField val RESPONSE_TIME = BerryCrushElementType("RESPONSE_TIME")
    @JvmField val EXISTS = BerryCrushElementType("EXISTS")
    @JvmField val NOT = BerryCrushElementType("NOT")

    // Operators
    @JvmField val EQUALS = BerryCrushElementType("EQUALS")
    @JvmField val NOT_EQUALS = BerryCrushElementType("NOT_EQUALS")
    @JvmField val GREATER_THAN = BerryCrushElementType("GREATER_THAN")
    @JvmField val LESS_THAN = BerryCrushElementType("LESS_THAN")
    @JvmField val GREATER_OR_EQUAL = BerryCrushElementType("GREATER_OR_EQUAL")
    @JvmField val LESS_OR_EQUAL = BerryCrushElementType("LESS_OR_EQUAL")
    @JvmField val MATCHES = BerryCrushElementType("MATCHES")
    @JvmField val STARTS_WITH = BerryCrushElementType("STARTS_WITH")
    @JvmField val ENDS_WITH = BerryCrushElementType("ENDS_WITH")

    // Literals
    @JvmField val STRING = BerryCrushElementType("STRING")
    @JvmField val NUMBER = BerryCrushElementType("NUMBER")
    @JvmField val BOOLEAN = BerryCrushElementType("BOOLEAN")
    @JvmField val NULL = BerryCrushElementType("NULL")

    // Identifiers and references
    @JvmField val IDENTIFIER = BerryCrushElementType("IDENTIFIER")
    @JvmField val OPERATION_REF = BerryCrushElementType("OPERATION_REF")
    @JvmField val TAG = BerryCrushElementType("TAG")
    @JvmField val VARIABLE = BerryCrushElementType("VARIABLE")
    @JvmField val JSON_PATH = BerryCrushElementType("JSON_PATH")

    // Syntax elements
    @JvmField val COLON = BerryCrushElementType("COLON")
    @JvmField val PIPE = BerryCrushElementType("PIPE")
    @JvmField val CARET = BerryCrushElementType("CARET")
    @JvmField val AT = BerryCrushElementType("AT")
    @JvmField val DOT = BerryCrushElementType("DOT")
    @JvmField val COMMA = BerryCrushElementType("COMMA")
    @JvmField val LPAREN = BerryCrushElementType("LPAREN")
    @JvmField val RPAREN = BerryCrushElementType("RPAREN")
    @JvmField val LBRACKET = BerryCrushElementType("LBRACKET")
    @JvmField val RBRACKET = BerryCrushElementType("RBRACKET")
    @JvmField val LBRACE = BerryCrushElementType("LBRACE")
    @JvmField val RBRACE = BerryCrushElementType("RBRACE")
    @JvmField val VARIABLE_START = BerryCrushElementType("VARIABLE_START")
    @JvmField val VARIABLE_END = BerryCrushElementType("VARIABLE_END")

    // Whitespace and structure
    @JvmField val NEWLINE = BerryCrushElementType("NEWLINE")
    @JvmField val INDENT = BerryCrushElementType("INDENT")
    @JvmField val DEDENT = BerryCrushElementType("DEDENT")
    @JvmField val WHITE_SPACE = BerryCrushElementType("WHITE_SPACE")

    // Comments
    @JvmField val COMMENT = BerryCrushElementType("COMMENT")

    // Text content
    @JvmField val TEXT = BerryCrushElementType("TEXT")

    // Error
    @JvmField val BAD_CHARACTER = BerryCrushElementType("BAD_CHARACTER")

    // Token sets for grouping
    @JvmField
    val BLOCK_KEYWORDS = TokenSet.create(FEATURE, SCENARIO, OUTLINE, FRAGMENT, PARAMETERS, BACKGROUND, EXAMPLES)

    @JvmField
    val STEP_KEYWORDS = TokenSet.create(GIVEN, WHEN, THEN, AND, BUT)

    @JvmField
    val DIRECTIVES = TokenSet.create(CALL, ASSERT, EXTRACT, INCLUDE, BODY, IF, ELSE, FAIL)

    @JvmField
    val ASSERTION_KEYWORDS = TokenSet.create(STATUS, HEADER, CONTAINS, SCHEMA, RESPONSE_TIME, EXISTS, NOT)

    @JvmField
    val OPERATORS = TokenSet.create(
        EQUALS, NOT_EQUALS, GREATER_THAN, LESS_THAN,
        GREATER_OR_EQUAL, LESS_OR_EQUAL, MATCHES, STARTS_WITH, ENDS_WITH
    )

    @JvmField
    val LITERALS = TokenSet.create(STRING, NUMBER, BOOLEAN, NULL)

    @JvmField
    val BRACES = TokenSet.create(LBRACE, RBRACE, LBRACKET, RBRACKET)

    @JvmField
    val COMMENTS = TokenSet.create(COMMENT)

    @JvmField
    val WHITESPACES = TokenSet.create(WHITE_SPACE)

    @JvmField
    val STRINGS = TokenSet.create(STRING)

    @JvmField
    val IDENTIFIERS = TokenSet.create(IDENTIFIER, OPERATION_REF, TAG, VARIABLE, TEXT)
}

/**
 * Custom element type for BerryCrush tokens.
 */
class BerryCrushElementType(debugName: String) : IElementType(debugName, BerryCrushLanguage) {
    override fun toString(): String = "BerryCrush:${super.toString()}"
}

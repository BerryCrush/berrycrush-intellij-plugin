package com.berrycrush.intellij.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey

/**
 * Text attribute keys for BerryCrush syntax highlighting.
 */
object BerryCrushHighlightingColors {

    // Block keywords (feature:, scenario:, fragment:, etc.)
    @JvmField
    val BLOCK_KEYWORD: TextAttributesKey = createTextAttributesKey(
        "BERRYCRUSH_BLOCK_KEYWORD",
        DefaultLanguageHighlighterColors.KEYWORD
    )

    // Step keywords (given, when, then, and, but)
    @JvmField
    val STEP_KEYWORD: TextAttributesKey = createTextAttributesKey(
        "BERRYCRUSH_STEP_KEYWORD",
        DefaultLanguageHighlighterColors.FUNCTION_DECLARATION
    )

    // Directives (call, assert, extract, include, etc.)
    @JvmField
    val DIRECTIVE: TextAttributesKey = createTextAttributesKey(
        "BERRYCRUSH_DIRECTIVE",
        DefaultLanguageHighlighterColors.INSTANCE_METHOD
    )

    // Assertion keywords (status, header, contains, etc.)
    @JvmField
    val ASSERTION_KEYWORD: TextAttributesKey = createTextAttributesKey(
        "BERRYCRUSH_ASSERTION_KEYWORD",
        DefaultLanguageHighlighterColors.STATIC_METHOD
    )

    // Operators (==, !=, >, <, etc.)
    @JvmField
    val OPERATOR: TextAttributesKey = createTextAttributesKey(
        "BERRYCRUSH_OPERATOR",
        DefaultLanguageHighlighterColors.OPERATION_SIGN
    )

    // Tags (@tag)
    @JvmField
    val TAG: TextAttributesKey = createTextAttributesKey(
        "BERRYCRUSH_TAG",
        DefaultLanguageHighlighterColors.METADATA
    )

    // Operation references (^operationId)
    @JvmField
    val OPERATION_REF: TextAttributesKey = createTextAttributesKey(
        "BERRYCRUSH_OPERATION_REF",
        DefaultLanguageHighlighterColors.FUNCTION_CALL
    )

    // Variables ({{variable}})
    @JvmField
    val VARIABLE: TextAttributesKey = createTextAttributesKey(
        "BERRYCRUSH_VARIABLE",
        DefaultLanguageHighlighterColors.LOCAL_VARIABLE
    )

    // JSON path ($.)
    @JvmField
    val JSON_PATH: TextAttributesKey = createTextAttributesKey(
        "BERRYCRUSH_JSON_PATH",
        DefaultLanguageHighlighterColors.INSTANCE_FIELD
    )

    // Strings
    @JvmField
    val STRING: TextAttributesKey = createTextAttributesKey(
        "BERRYCRUSH_STRING",
        DefaultLanguageHighlighterColors.STRING
    )

    // Numbers
    @JvmField
    val NUMBER: TextAttributesKey = createTextAttributesKey(
        "BERRYCRUSH_NUMBER",
        DefaultLanguageHighlighterColors.NUMBER
    )

    // Comments
    @JvmField
    val COMMENT: TextAttributesKey = createTextAttributesKey(
        "BERRYCRUSH_COMMENT",
        DefaultLanguageHighlighterColors.LINE_COMMENT
    )

    // Braces and brackets
    @JvmField
    val BRACES: TextAttributesKey = createTextAttributesKey(
        "BERRYCRUSH_BRACES",
        DefaultLanguageHighlighterColors.BRACES
    )

    // Pipe (|) for tables
    @JvmField
    val PIPE: TextAttributesKey = createTextAttributesKey(
        "BERRYCRUSH_PIPE",
        DefaultLanguageHighlighterColors.OPERATION_SIGN
    )

    // Parameter keys in include directives (paramName:)
    @JvmField
    val PARAMETER_KEY: TextAttributesKey = createTextAttributesKey(
        "BERRYCRUSH_PARAMETER_KEY",
        DefaultLanguageHighlighterColors.INSTANCE_FIELD
    )

    // Bad character
    @JvmField
    val BAD_CHARACTER: TextAttributesKey = createTextAttributesKey(
        "BERRYCRUSH_BAD_CHARACTER",
        HighlighterColors.BAD_CHARACTER
    )
}

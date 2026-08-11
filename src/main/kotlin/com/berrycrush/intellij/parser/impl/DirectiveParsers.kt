package com.berrycrush.intellij.parser.impl

import com.berrycrush.intellij.lexer.BerryCrushTokenTypes
import com.berrycrush.intellij.psi.BerryCrushElementTypes
import com.berrycrush.intellij.psi.BerryCrushPsiElementType
import com.intellij.lang.PsiBuilder

internal fun PsiBuilder.parseIncludeDirective(parentIndent: Int) {
    val marker = mark()
    advanceLexer() // consume "include"

    skipNewlines()
    // Parse fragment reference
    if (tokenType == BerryCrushTokenTypes.IDENTIFIER ||
        tokenType == BerryCrushTokenTypes.OPERATION_REF ||
        tokenType == BerryCrushTokenTypes.TEXT
    ) {
        parseFragmentRef()
    }

    skipToEndOfLine()

    // Parse optional parameter block (indented key: value pairs)
    parseIncludeParameters(parentIndent)

    marker.done(BerryCrushElementTypes.INCLUDE_DIRECTIVE)
}

internal fun PsiBuilder.parseCallDirective(parentIndent: Int) {
    val marker = mark()
    advanceLexer() // consume "call"

    // Look for operation reference
    while (!eof() && !isLineEnd(tokenType)) {
        when (tokenType) {
            BerryCrushTokenTypes.OPERATION_REF -> parseOperationRef()
            BerryCrushTokenTypes.VARIABLE -> parseVariableRef()
            else -> advanceLexer()
        }
    }
    skipNewlines()

    // Parse optional parameter block (same format as include parameters)
    parseIncludeParameters(parentIndent)

    marker.done(BerryCrushElementTypes.CALL_DIRECTIVE)
}

internal fun PsiBuilder.parseWebhookDirective(parentIndent: Int) {
    val marker = mark()
    advanceLexer() // advance webhook
    while (!eof() && !isLineEnd(tokenType)) {
        if (tokenType == BerryCrushTokenTypes.TEXT) {
            parseWebhookName()
        } else {
            advanceLexer()
        }
    }
    skipNewlines()
    // Parse webhook parameter block (same format as include parameters)
    parseIncludeParameters(parentIndent)
    marker.done(BerryCrushElementTypes.WEBHOOK_DIRECTIVE)
}

internal fun PsiBuilder.parseExtractDirective() {
    val marker = mark()
    advanceLexer() // consume "extract"
    parseExtractVariable()
    marker.done(BerryCrushElementTypes.EXTRACT_DIRECTIVE)
}

internal fun PsiBuilder.parseConditionalDirective(
    parentIndent: Int,
    elementType: BerryCrushPsiElementType,
) {
    val marker = mark()
    val type = tokenType
    advanceLexer() // consume if/else
    skipWhiteSpaces()
    if (type == BerryCrushTokenTypes.IF) {
        parseCondition()
        // Parse nested branch content at deeper indentation.
        parseStepNestedContent(parentIndent)
    } else {
        if (type == BerryCrushTokenTypes.IF) {
            parseConditionalDirective(parentIndent, BerryCrushElementTypes.IF_DIRECTIVE)
        } else {
            skipToEndOfLine()
            parseStepNestedContent(parentIndent)
        }
    }
    marker.done(elementType)
}

internal fun PsiBuilder.parseAssertDirective() {
    val marker = mark()
    advanceLexer() // consume "assert"
    parseCondition()
    marker.done(BerryCrushElementTypes.ASSERT_DIRECTIVE)
}

internal fun PsiBuilder.parseOperationRef() = markAs(BerryCrushElementTypes.OPERATION_REF)

private fun PsiBuilder.parseExtractVariable() {
    while (!eof() && tokenType != BerryCrushTokenTypes.NEWLINE) {
        when (tokenType) {
            BerryCrushTokenTypes.JSON_PATH -> markAs(BerryCrushElementTypes.JSON_PATH)
            BerryCrushTokenTypes.VARIABLE -> markAs(BerryCrushElementTypes.VARIABLE_REF)
            BerryCrushTokenTypes.ARROW -> markAs(BerryCrushElementTypes.ARROW)
            BerryCrushTokenTypes.TEXT -> markAs(BerryCrushElementTypes.TEXT)
            else -> advanceLexer()
        }
    }
}

private fun PsiBuilder.parseFragmentRef() = markAs(BerryCrushElementTypes.FRAGMENT_REF)

private fun PsiBuilder.parseWebhookName() = markAs(BerryCrushElementTypes.WEBHOOK_NAME)

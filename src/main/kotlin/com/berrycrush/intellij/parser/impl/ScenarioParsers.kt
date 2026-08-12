package com.berrycrush.intellij.parser.impl

import com.berrycrush.intellij.lexer.BerryCrushTokenTypes
import com.berrycrush.intellij.psi.BerryCrushElementTypes
import com.berrycrush.intellij.psi.BerryCrushPsiElementType
import com.intellij.lang.PsiBuilder

internal fun PsiBuilder.parseScenario(scenarioIndent: Int) {
    val marker = mark()
    advanceLexer()
    skipToEndOfLine(BerryCrushElementTypes.BLOCK_NAME)

    // Parse optional parameters block after scenario name
    tryParseParametersBlock(scenarioIndent)
    parseScenarioContent(scenarioIndent)

    marker.done(BerryCrushElementTypes.SCENARIO)
}

internal fun PsiBuilder.parseOutline(outlineIndent: Int) {
    val marker = mark()
    advanceLexer()
    skipToEndOfLine(BerryCrushElementTypes.BLOCK_NAME)

    // Parse optional parameters block after outline name
    tryParseParametersBlock(outlineIndent)
    parseScenarioContent(outlineIndent, true)
    marker.done(BerryCrushElementTypes.OUTLINE)
}

internal fun PsiBuilder.parseBackground(backgroundIndent: Int) {
    val marker = mark()
    advanceLexer()
    skipToEndOfLine()
    parseScenarioContent(backgroundIndent)
    marker.done(BerryCrushElementTypes.BACKGROUND)
}

internal fun PsiBuilder.parseScenarioContent(parentIndent: Int, isOutline: Boolean = false) {
    while (!eof()) {
        skipNewlines()

        val indent = currentLineIndent()
        if (indent <= parentIndent) {
            return
        }

        consumeLineIndent()
        when (tokenType) {
            BerryCrushTokenTypes.GIVEN,
            BerryCrushTokenTypes.WHEN,
            BerryCrushTokenTypes.THEN,
            BerryCrushTokenTypes.AND,
            BerryCrushTokenTypes.BUT,
            -> parseStep(indent)
            BerryCrushTokenTypes.CALL -> parseCallDirective(indent)
            BerryCrushTokenTypes.WEBHOOK -> parseWebhookDirective(indent)
            BerryCrushTokenTypes.INCLUDE -> parseIncludeDirective(indent)
            BerryCrushTokenTypes.ASSERT -> parseAssertDirective()
            BerryCrushTokenTypes.BACKGROUND -> parseBackground(indent)
            BerryCrushTokenTypes.SCENARIO -> parseScenario(indent)
            BerryCrushTokenTypes.EXAMPLES -> if (isOutline) parseExamples(indent) else skipToEndOfLine()
            else -> skipToEndOfLine()
        }
    }
}

/**
 * Parse `example:` clause
 * ```
 * examples:
 * | name0    | name1    |
 * | value0.0 | value1.0 |
 * ```
 */
private fun PsiBuilder.parseExamples(indent: Int) {
    val marker = mark()
    advanceLexer() // consume `examples:`
    skipToEndOfLine()

    val parsed = parseExampleRow(indent, BerryCrushElementTypes.EXAMPLES_HEADER)
    if (parsed) {
        parseIndentedEntries(indent) { parentIndent -> this.parseExampleRow(parentIndent) }
    }
    marker.done(BerryCrushElementTypes.EXAMPLES)
}

private fun PsiBuilder.parseExampleRow(
    parentIndent: Int,
    cellType: BerryCrushPsiElementType = BerryCrushElementTypes.EXAMPLES_VALUE,
): Boolean {
    val indent = currentLineIndent()
    if (indent <= parentIndent) {
        return false
    }

    consumeLineIndent()

    if (tokenType == BerryCrushTokenTypes.NEWLINE) {
        skipNewlines()
        return true
    }

    if (tokenType != BerryCrushTokenTypes.PIPE) {
        skipToEndOfLine()
        return true
    }

    val marker = mark()
    advanceLexer() // consume leading `|`
    var cellMarker = mark()

    while (!eof() && tokenType != BerryCrushTokenTypes.NEWLINE) {
        when (tokenType) {
            BerryCrushTokenTypes.PIPE -> {
                cellMarker.done(cellType)
                advanceLexer()
                cellMarker = mark()
            }
            BerryCrushTokenTypes.VARIABLE -> parseVariableRef()
            else -> advanceLexer()
        }
    }

    cellMarker.rollbackTo()
    marker.done(BerryCrushElementTypes.EXAMPLE_ROW)
    skipNewlines()
    return true
}

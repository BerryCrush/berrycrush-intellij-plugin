package com.berrycrush.intellij.parser.impl

import com.berrycrush.intellij.lexer.BerryCrushTokenTypes
import com.berrycrush.intellij.psi.BerryCrushElementTypes
import com.intellij.lang.PsiBuilder

fun PsiBuilder.parseParameters(indent: Int) {
    // It's a parameters block
    val blockMarker = mark()

    advanceLexer() // consume PARAMETERS
    skipToEndOfLine()

    // Parse parameter entries
    parseIndentedEntries(indent) { tryParseParameterEntry(it) }
    blockMarker.done(BerryCrushElementTypes.PARAMETERS)
}

/**
 * Try to parse a single parameter entry.
 * Returns true if a parameter was successfully parsed, false otherwise.
 */
internal fun PsiBuilder.tryParseParameter(parentIndent: Int): Boolean = tryParseParameterLike(
    parentIndent,
    allowBody = true,
)

/**
 * Parse parameter block for include directive.
 * Parameters are indented key: value pairs following the include line.
 */
internal fun PsiBuilder.parseIncludeParameters(parentIndent: Int) {
    if (tokenType == BerryCrushTokenTypes.INDENT) {
        val marker = mark()
        parseIndentedEntries(parentIndent) { tryParseParameter(it) }
        marker.done(BerryCrushElementTypes.INCLUDED_PARAMETER)
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
internal fun PsiBuilder.tryParseParametersBlock(parentIndent: Int) {
    // Check for INDENT followed by PARAMETERS keyword
    if (tokenType != BerryCrushTokenTypes.INDENT) return

    val indent = currentLineIndent()
    if (indent <= parentIndent) return

    // Look ahead to check if next non-whitespace is PARAMETERS
    val marker = mark()
    consumeLineIndent()
    skipWhiteSpaces()
    if (tokenType != BerryCrushTokenTypes.PARAMETERS) {
        // Not a parameters block, rollback
        marker.rollbackTo()
        return
    }
    parseParameters(parentIndent)
    marker.drop()
}

/**
 * Try to parse a single parameter entry: `  key: value`
 * Returns true if successfully parsed.
 */
private fun PsiBuilder.tryParseParameterEntry(parentIndent: Int): Boolean = tryParseParameterLike(
    parentIndent,
    allowBody = false,
)

private fun PsiBuilder.tryParseParameterLike(
    parentIndent: Int,
    allowBody: Boolean,
): Boolean {
    val indent = currentLineIndent()
    if (indent <= parentIndent) {
        return false
    }

    val marker = mark()
    consumeLineIndent()

    when (tokenType) {
        BerryCrushTokenTypes.BODY -> {
            if (!allowBody) {
                marker.rollbackTo()
                return false
            }
            markAs(BerryCrushElementTypes.PARAMETER_KEY)
        }

        BerryCrushTokenTypes.IDENTIFIER,
        BerryCrushTokenTypes.TEXT,
        -> if (parseParameterKey(marker)) return false
        else -> {
            if (!allowBody) {
                marker.rollbackTo()
                return false
            }

            val tokenType = tokenType
            if (tokenType == null || tokenType == BerryCrushTokenTypes.NEWLINE) {
                marker.rollbackTo()
                return false
            }

            if (parseParameterKey(marker)) return false
        }
    }
    skipWhiteSpaces()

    parseParameterValue(indent, allowBody)
    marker.done(BerryCrushElementTypes.PARAMETER_ENTRY)
    return true
}

private fun PsiBuilder.parseParameterKey(marker: PsiBuilder.Marker): Boolean {
    val keyMarker = mark()
    val keyText = tokenText.orEmpty()
    advanceLexer()
    skipWhiteSpaces()

    val hasInlineColon = keyText.endsWith(":")
    if (tokenType == BerryCrushTokenTypes.COLON) {
        advanceLexer() // consume ':'
    } else if (!hasInlineColon) {
        keyMarker.rollbackTo()
        marker.rollbackTo()
        return true
    }
    keyMarker.done(BerryCrushElementTypes.PARAMETER_KEY)
    return false
}

/**
 * Parse parameter value which may contain variable interpolations.
 */
private fun PsiBuilder.parseParameterValue(indent: Int, allowBody: Boolean) {
    val hasInlineValue = tokenType != BerryCrushTokenTypes.NEWLINE && !eof()
    val valueMarker = mark()
    if (hasInlineValue) {
        while (!eof() && tokenType != BerryCrushTokenTypes.NEWLINE) {
            when (tokenType) {
                BerryCrushTokenTypes.VARIABLE -> parseVariableRef()
                BerryCrushTokenTypes.STRING -> parseStringLiteral()
                else -> advanceLexer()
            }
        }
    } else {
        skipNewlines()
        // check nested
        parseIndentedEntries(indent) { childParentIndent ->
            if (currentLineIndent() > childParentIndent && lookAhead(1) in BerryCrushTokenTypes.LITERALS) {
                advanceLexer() // advance indent
                if (tokenType == BerryCrushTokenTypes.STRING) {
                    parseStringLiteral()
                } else {
                    advanceLexer() // advance literal
                }
                false
            } else {
                tryParseParameterLike(
                    childParentIndent,
                    allowBody,
                )
            }
        }
    }

    valueMarker.done(BerryCrushElementTypes.PARAMETER_VALUE)
}

package com.berrycrush.intellij.lexer

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType

/**
 * Lexer for BerryCrush scenario and fragment files.
 *
 * Handles indentation-sensitive syntax and BerryCrush-specific tokens.
 */
class BerryCrushLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var bufferEnd: Int = 0
    private var tokenStart: Int = 0
    private var tokenEnd: Int = 0
    private var tokenType: IElementType? = null
    private var position: Int = 0
    private var lineStart: Boolean = true

    companion object {
        private val KEYWORDS =
            mapOf(
                "feature:" to BerryCrushTokenTypes.FEATURE,
                "scenario:" to BerryCrushTokenTypes.SCENARIO,
                "outline:" to BerryCrushTokenTypes.OUTLINE,
                "fragment:" to BerryCrushTokenTypes.FRAGMENT,
                "parameters:" to BerryCrushTokenTypes.PARAMETERS,
                "background:" to BerryCrushTokenTypes.BACKGROUND,
                "examples:" to BerryCrushTokenTypes.EXAMPLES,
                "given" to BerryCrushTokenTypes.GIVEN,
                "when" to BerryCrushTokenTypes.WHEN,
                "then" to BerryCrushTokenTypes.THEN,
                "and" to BerryCrushTokenTypes.AND,
                "but" to BerryCrushTokenTypes.BUT,
                "call" to BerryCrushTokenTypes.CALL,
                "webhook:" to BerryCrushTokenTypes.WEBHOOK,
                "assert" to BerryCrushTokenTypes.ASSERT,
                "extract" to BerryCrushTokenTypes.EXTRACT,
                "include" to BerryCrushTokenTypes.INCLUDE,
                "body:" to BerryCrushTokenTypes.BODY,
                "bodyFile:" to BerryCrushTokenTypes.BODY,
                "if" to BerryCrushTokenTypes.IF,
                "else" to BerryCrushTokenTypes.ELSE,
                "fail" to BerryCrushTokenTypes.FAIL,
                "status" to BerryCrushTokenTypes.STATUS,
                "statusCode" to BerryCrushTokenTypes.STATUS,
                "header" to BerryCrushTokenTypes.HEADER,
                "contains" to BerryCrushTokenTypes.CONTAINS,
                "bodyContains" to BerryCrushTokenTypes.CONTAINS,
                "schema" to BerryCrushTokenTypes.SCHEMA,
                "responseTime" to BerryCrushTokenTypes.RESPONSE_TIME,
                "exists" to BerryCrushTokenTypes.EXISTS,
                "not" to BerryCrushTokenTypes.NOT,
                "=" to BerryCrushTokenTypes.EQUALS,
                "equals" to BerryCrushTokenTypes.EQUALS,
                "!=" to BerryCrushTokenTypes.NOT_EQUALS,
                ">=" to BerryCrushTokenTypes.GREATER_OR_EQUAL,
                "<=" to BerryCrushTokenTypes.LESS_OR_EQUAL,
                ">" to BerryCrushTokenTypes.GREATER_THAN,
                "greaterThan" to BerryCrushTokenTypes.GREATER_THAN,
                "<" to BerryCrushTokenTypes.LESS_THAN,
                "lessThan" to BerryCrushTokenTypes.LESS_THAN,
                "matches " to BerryCrushTokenTypes.MATCHES,
                "startsWith " to BerryCrushTokenTypes.STARTS_WITH,
                "endsWith " to BerryCrushTokenTypes.ENDS_WITH,
                "in" to BerryCrushTokenTypes.IN,
                "size" to BerryCrushTokenTypes.SIZE,
                "hasSize" to BerryCrushTokenTypes.HAS_SIZE,
                "arraySize" to BerryCrushTokenTypes.ARRAY_SIZE,
                "empty" to BerryCrushTokenTypes.EMPTY,
                "notEmpty" to BerryCrushTokenTypes.NOT_EMPTY,
            )
    }

    override fun start(
        buffer: CharSequence,
        startOffset: Int,
        endOffset: Int,
        initialState: Int,
    ) {
        this.buffer = buffer
        this.bufferEnd = endOffset
        this.position = startOffset
        this.tokenStart = startOffset
        this.tokenEnd = startOffset
        this.tokenType = null
        this.lineStart = true
        advance()
    }

    override fun getState(): Int = 0

    override fun getTokenType(): IElementType? = tokenType

    override fun getTokenStart(): Int = tokenStart

    override fun getTokenEnd(): Int = tokenEnd

    override fun getBufferSequence(): CharSequence = buffer

    override fun getBufferEnd(): Int = bufferEnd

    override fun advance() {
        tokenStart = position
        tokenType = computeNextToken()
        tokenEnd = position
    }

    private fun computeNextToken(): IElementType? {
        if (position >= bufferEnd) return null
        val c = buffer[position]
        return when {
            c == '\n' || c == '\r' -> scanNewline(c)
            c == '#' -> scanComment()
            c == ' ' && lineStart -> scanIndent()
            (c == '"' || c == '\'') && checkString(c) -> scanString(c)
            c.isWhitespace() -> scanWhitespace()
            else -> {
                lineStart = false
                when {
                    c == '@' -> scanTag()
                    c == '^' -> scanOperationRef()
                    c == '{' && peek() == '{' -> scanVariable()
                    c.isDigit() || (c == '-' && peek()?.isDigit() == true) -> scanNumber()
                    c == '$' -> scanJsonPath()
                    c == '=' && peek() == '>' -> {
                        position += 2
                        BerryCrushTokenTypes.ARROW
                    }
                    c == ':' -> {
                        position++
                        BerryCrushTokenTypes.COLON
                    }
                    c == '|' -> {
                        position++
                        BerryCrushTokenTypes.PIPE
                    }
                    c == '.' -> {
                        position++
                        BerryCrushTokenTypes.DOT
                    }
                    c == ',' -> {
                        position++
                        BerryCrushTokenTypes.COMMA
                    }
                    c == '(' -> {
                        position++
                        BerryCrushTokenTypes.LPAREN
                    }
                    c == ')' -> {
                        position++
                        BerryCrushTokenTypes.RPAREN
                    }
                    c == '[' -> {
                        position++
                        BerryCrushTokenTypes.LBRACKET
                    }
                    c == ']' -> {
                        position++
                        BerryCrushTokenTypes.RBRACKET
                    }
                    c == '{' -> {
                        position++
                        BerryCrushTokenTypes.LBRACE
                    }
                    c == '}' -> {
                        position++
                        BerryCrushTokenTypes.RBRACE
                    }
                    else -> {
                        tryMatchKeyword() ?: scanIdentifierOrText()
                    }
                }
            }
        }
    }

    private fun scanNewline(c: Char): BerryCrushElementType {
        position++
        if (c == '\r' && position < bufferEnd && buffer[position] == '\n') {
            position++
        }
        lineStart = true
        return BerryCrushTokenTypes.NEWLINE
    }

    private fun Map<String, BerryCrushElementType>.findKeyword(): IElementType? = entries
        .find {
            matchesAt(it.key) && isTokenBoundary(position + it.key.length)
        }?.let {
            position += it.key.length
            it.value
        }

    private fun tryMatchKeyword(): IElementType? {
        // Try block keywords (strict lowercase per BerryCrush DSL spec)
        return KEYWORDS.findKeyword()
            ?: tryJsonLiterals()
    }

    private fun tryJsonLiterals(): IElementType? {
        // Boolean literals are case-insensitive (JSON convention)
        return when {
            matchesAt("true") && !isIdentifierChar(position + 4) -> {
                position += 4
                BerryCrushTokenTypes.BOOLEAN
            }
            matchesAt("false") && !isIdentifierChar(position + 5) -> {
                position += 5
                BerryCrushTokenTypes.BOOLEAN
            }
            matchesAt("null") && !isIdentifierChar(position + 4) -> {
                position += 4
                BerryCrushTokenTypes.NULL
            }
            else -> null
        }
    }

    private fun scanComment(): IElementType {
        while (position < bufferEnd && buffer[position] != '\n' && buffer[position] != '\r') {
            position++
        }
        return BerryCrushTokenTypes.COMMENT
    }

    private fun scanIndent(): IElementType {
        while (position < bufferEnd && buffer[position] == ' ') {
            position++
        }
        return BerryCrushTokenTypes.INDENT
    }

    private fun scanWhitespace(): IElementType {
        while (position < bufferEnd &&
            buffer[position].isWhitespace() &&
            buffer[position] != '\n' &&
            buffer[position] != '\r'
        ) {
            position++
        }
        return BerryCrushTokenTypes.WHITE_SPACE
    }

    private fun scanTag(): IElementType {
        position++ // Skip '@'
        while (position < bufferEnd && buffer[position].isTagChar()) {
            position++
        }
        return BerryCrushTokenTypes.TAG
    }

    private fun scanOperationRef(): IElementType {
        position++ // Skip '^'
        while (position < bufferEnd && buffer[position].isIdentifierChar()) {
            position++
        }
        return BerryCrushTokenTypes.OPERATION_REF
    }

    private fun scanVariable(): IElementType {
        position += 2 // Skip '{{'
        while (position < bufferEnd) {
            if (buffer[position] == '}' && position + 1 < bufferEnd && buffer[position + 1] == '}') {
                position += 2
                break
            }
            position++
        }
        return BerryCrushTokenTypes.VARIABLE
    }

    private fun checkString(quote: Char): Boolean {
        for (i in (position + 1) until bufferEnd) {
            if (buffer[i] == quote) {
                return true
            }
        }
        return false
    }

    private fun scanString(quote: Char): IElementType {
        position++ // Skip opening quote
        while (position < bufferEnd && buffer[position] != quote) {
            if (buffer[position] == '\\' && position + 1 < bufferEnd) {
                position += 2 // Skip escape sequence
            } else {
                position++
            }
        }
        if (position < bufferEnd) {
            position++ // Skip closing quote
        }
        return BerryCrushTokenTypes.STRING
    }

    private fun scanNumber(): IElementType {
        if (buffer[position] == '-') position++
        while (position < bufferEnd && buffer[position].isDigit()) {
            position++
        }
        // Handle decimal
        if (position < bufferEnd &&
            buffer[position] == '.' &&
            position + 1 < bufferEnd &&
            buffer[position + 1].isDigit()
        ) {
            position++
            while (position < bufferEnd && buffer[position].isDigit()) {
                position++
            }
        }
        return BerryCrushTokenTypes.NUMBER
    }

    private fun scanJsonPath(): IElementType {
        position++ // Skip '$'
        while (position < bufferEnd && buffer[position].isJsonPathChar()) {
            position++
        }
        return BerryCrushTokenTypes.JSON_PATH
    }

    private fun scanIdentifierOrText(): IElementType {
        while (position < bufferEnd && !buffer[position].isTokenBoundary()) {
            position++
        }
        return if (tokenStart == position) {
            position++
            BerryCrushTokenTypes.BAD_CHARACTER
        } else {
            BerryCrushTokenTypes.TEXT
        }
    }

    private fun matchesAt(str: String): Boolean {
        if (position + str.length > bufferEnd) return false
        for (i in str.indices) {
            if (buffer[position + i] != str[i]) return false
        }
        return true
    }

    private fun isIdentifierChar(pos: Int): Boolean = pos < bufferEnd && (buffer[pos].isLetterOrDigit() || buffer[pos] == '_' || buffer[pos] == '-')

    private fun isTokenBoundary(pos: Int): Boolean = pos >= bufferEnd || buffer[pos].isTokenBoundary()

    private fun peek(n: Int = 1) = if (position + n < bufferEnd) buffer[position + n] else null
}

private fun Char.isIdentifierChar(): Boolean = isLetterOrDigit() || this == '_' || this == '-'

private fun Char.isTagChar(): Boolean = isLetterOrDigit() || this == '_' || this == '-'

private fun Char.isJsonPathChar(): Boolean = isLetterOrDigit() || this in ".[]*@_()"

private fun Char.isTokenBoundary(): Boolean = isWhitespace() || this in ":#@^|,()[]{}\"'"

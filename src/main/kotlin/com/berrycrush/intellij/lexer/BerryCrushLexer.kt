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
        private val BLOCK_KEYWORDS = mapOf(
            "feature:" to BerryCrushTokenTypes.FEATURE,
            "scenario:" to BerryCrushTokenTypes.SCENARIO,
            "outline:" to BerryCrushTokenTypes.OUTLINE,
            "fragment:" to BerryCrushTokenTypes.FRAGMENT,
            "parameters:" to BerryCrushTokenTypes.PARAMETERS,
            "background:" to BerryCrushTokenTypes.BACKGROUND,
            "examples:" to BerryCrushTokenTypes.EXAMPLES,
        )

        private val STEP_KEYWORDS = mapOf(
            "given " to BerryCrushTokenTypes.GIVEN,
            "given:" to BerryCrushTokenTypes.GIVEN,
            "when " to BerryCrushTokenTypes.WHEN,
            "when:" to BerryCrushTokenTypes.WHEN,
            "then " to BerryCrushTokenTypes.THEN,
            "then:" to BerryCrushTokenTypes.THEN,
            "and " to BerryCrushTokenTypes.AND,
            "and:" to BerryCrushTokenTypes.AND,
            "but " to BerryCrushTokenTypes.BUT,
            "but:" to BerryCrushTokenTypes.BUT,
        )

        private val DIRECTIVES = mapOf(
            "call " to BerryCrushTokenTypes.CALL,
            "assert " to BerryCrushTokenTypes.ASSERT,
            "extract " to BerryCrushTokenTypes.EXTRACT,
            "include " to BerryCrushTokenTypes.INCLUDE,
            "body:" to BerryCrushTokenTypes.BODY,
            "if " to BerryCrushTokenTypes.IF,
            "else if " to BerryCrushTokenTypes.ELSE,
            "else" to BerryCrushTokenTypes.ELSE,
            "fail " to BerryCrushTokenTypes.FAIL,
        )

        private val ASSERTION_KEYWORDS = mapOf(
            "status " to BerryCrushTokenTypes.STATUS,
            "header " to BerryCrushTokenTypes.HEADER,
            "contains " to BerryCrushTokenTypes.CONTAINS,
            "schema" to BerryCrushTokenTypes.SCHEMA,
            "responseTime " to BerryCrushTokenTypes.RESPONSE_TIME,
            "exists" to BerryCrushTokenTypes.EXISTS,
            "not " to BerryCrushTokenTypes.NOT,
        )

        private val OPERATORS = mapOf(
            "==" to BerryCrushTokenTypes.EQUALS,
            "!=" to BerryCrushTokenTypes.NOT_EQUALS,
            ">=" to BerryCrushTokenTypes.GREATER_OR_EQUAL,
            "<=" to BerryCrushTokenTypes.LESS_OR_EQUAL,
            ">" to BerryCrushTokenTypes.GREATER_THAN,
            "<" to BerryCrushTokenTypes.LESS_THAN,
            "matches " to BerryCrushTokenTypes.MATCHES,
            "startsWith " to BerryCrushTokenTypes.STARTS_WITH,
            "endsWith " to BerryCrushTokenTypes.ENDS_WITH,
        )
    }

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
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

        // Handle newlines
        if (c == '\n' || c == '\r') {
            position++
            if (c == '\r' && position < bufferEnd && buffer[position] == '\n') {
                position++
            }
            lineStart = true
            return BerryCrushTokenTypes.NEWLINE
        }

        // Handle comments
        if (c == '#') {
            return scanComment()
        }

        // Handle whitespace at line start (indentation)
        if (lineStart && c == ' ') {
            return scanIndent()
        }

        // Handle whitespace
        if (c.isWhitespace()) {
            return scanWhitespace()
        }

        lineStart = false

        // Handle tags
        if (c == '@') {
            return scanTag()
        }

        // Handle operation references
        if (c == '^') {
            return scanOperationRef()
        }

        // Handle variables
        if (c == '{' && position + 1 < bufferEnd && buffer[position + 1] == '{') {
            return scanVariable()
        }

        // Handle strings
        if (c == '"' || c == '\'') {
            return scanString(c)
        }

        // Handle numbers
        if (c.isDigit() || (c == '-' && position + 1 < bufferEnd && buffer[position + 1].isDigit())) {
            return scanNumber()
        }

        // Handle JSON path
        if (c == '$') {
            return scanJsonPath()
        }

        // Handle single-character syntax
        when (c) {
            ':' -> { position++; return BerryCrushTokenTypes.COLON }
            '|' -> { position++; return BerryCrushTokenTypes.PIPE }
            '.' -> { position++; return BerryCrushTokenTypes.DOT }
            ',' -> { position++; return BerryCrushTokenTypes.COMMA }
            '(' -> { position++; return BerryCrushTokenTypes.LPAREN }
            ')' -> { position++; return BerryCrushTokenTypes.RPAREN }
            '[' -> { position++; return BerryCrushTokenTypes.LBRACKET }
            ']' -> { position++; return BerryCrushTokenTypes.RBRACKET }
            '{' -> { position++; return BerryCrushTokenTypes.LBRACE }
            '}' -> { position++; return BerryCrushTokenTypes.RBRACE }
        }

        // Try to match operators
        for ((keyword, token) in OPERATORS) {
            if (matchesAt(keyword)) {
                position += keyword.length
                return token
            }
        }

        // Try to match keywords
        val keywordToken = tryMatchKeyword()
        if (keywordToken != null) {
            return keywordToken
        }

        // Handle identifiers and text
        return scanIdentifierOrText()
    }

    private fun tryMatchKeyword(): IElementType? {
        // Try block keywords
        for ((keyword, token) in BLOCK_KEYWORDS) {
            if (matchesAtCaseInsensitive(keyword)) {
                position += keyword.length
                return token
            }
        }

        // Try step keywords
        for ((keyword, token) in STEP_KEYWORDS) {
            if (matchesAtCaseInsensitive(keyword)) {
                position += keyword.length
                return token
            }
        }

        // Try directives
        for ((keyword, token) in DIRECTIVES) {
            if (matchesAtCaseInsensitive(keyword)) {
                position += keyword.length
                return token
            }
        }

        // Try assertion keywords
        for ((keyword, token) in ASSERTION_KEYWORDS) {
            if (matchesAtCaseInsensitive(keyword)) {
                position += keyword.length
                return token
            }
        }

        // Try boolean literals
        if (matchesAtCaseInsensitive("true") && !isIdentifierChar(position + 4)) {
            position += 4
            return BerryCrushTokenTypes.BOOLEAN
        }
        if (matchesAtCaseInsensitive("false") && !isIdentifierChar(position + 5)) {
            position += 5
            return BerryCrushTokenTypes.BOOLEAN
        }
        if (matchesAtCaseInsensitive("null") && !isIdentifierChar(position + 4)) {
            position += 4
            return BerryCrushTokenTypes.NULL
        }

        return null
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
        while (position < bufferEnd && buffer[position].isWhitespace() &&
               buffer[position] != '\n' && buffer[position] != '\r') {
            position++
        }
        return BerryCrushTokenTypes.WHITE_SPACE
    }

    private fun scanTag(): IElementType {
        position++ // Skip '@'
        while (position < bufferEnd && isTagChar(buffer[position])) {
            position++
        }
        return BerryCrushTokenTypes.TAG
    }

    private fun scanOperationRef(): IElementType {
        position++ // Skip '^'
        while (position < bufferEnd && isIdentifierChar(buffer[position])) {
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
        if (position < bufferEnd && buffer[position] == '.' &&
            position + 1 < bufferEnd && buffer[position + 1].isDigit()) {
            position++
            while (position < bufferEnd && buffer[position].isDigit()) {
                position++
            }
        }
        return BerryCrushTokenTypes.NUMBER
    }

    private fun scanJsonPath(): IElementType {
        position++ // Skip '$'
        while (position < bufferEnd && isJsonPathChar(buffer[position])) {
            position++
        }
        return BerryCrushTokenTypes.JSON_PATH
    }

    private fun scanIdentifierOrText(): IElementType {
        while (position < bufferEnd && !isTokenBoundary(buffer[position])) {
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

    private fun matchesAtCaseInsensitive(str: String): Boolean {
        if (position + str.length > bufferEnd) return false
        for (i in str.indices) {
            if (buffer[position + i].lowercaseChar() != str[i].lowercaseChar()) return false
        }
        return true
    }

    private fun isIdentifierChar(pos: Int): Boolean =
        pos < bufferEnd && (buffer[pos].isLetterOrDigit() || buffer[pos] == '_' || buffer[pos] == '-')

    private fun isIdentifierChar(c: Char): Boolean =
        c.isLetterOrDigit() || c == '_' || c == '-'

    private fun isTagChar(c: Char): Boolean =
        c.isLetterOrDigit() || c == '_' || c == '-'

    private fun isJsonPathChar(c: Char): Boolean =
        c.isLetterOrDigit() || c == '.' || c == '[' || c == ']' || c == '*' || c == '@' || c == '_'

    private fun isTokenBoundary(c: Char): Boolean =
        c.isWhitespace() || c in ":#@^|,()[]{}\"'"
}

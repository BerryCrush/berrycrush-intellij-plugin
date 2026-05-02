package com.berrycrush.intellij

import com.berrycrush.intellij.lexer.BerryCrushLexer
import com.berrycrush.intellij.lexer.BerryCrushTokenTypes
import com.intellij.psi.tree.IElementType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Unit tests for BerryCrush lexer.
 */
class BerryCrushLexerTest {

    @Test
    fun `test lexer tokenizes feature keyword`() {
        val tokens = tokenize("feature: User login")
        assertContains(tokens, BerryCrushTokenTypes.FEATURE)
    }

    @Test
    fun `test lexer tokenizes scenario keyword`() {
        val tokens = tokenize("scenario: Basic login")
        assertContains(tokens, BerryCrushTokenTypes.SCENARIO)
    }

    @Test
    fun `test lexer tokenizes step keywords`() {
        val input = """
            given user is on login page
            when user enters credentials
            then user is logged in
            and session is created
            but no errors shown
        """.trimIndent()
        val tokens = tokenize(input)

        assertContains(tokens, BerryCrushTokenTypes.GIVEN)
        assertContains(tokens, BerryCrushTokenTypes.WHEN)
        assertContains(tokens, BerryCrushTokenTypes.THEN)
        assertContains(tokens, BerryCrushTokenTypes.AND)
        assertContains(tokens, BerryCrushTokenTypes.BUT)
    }

    @Test
    fun `test lexer tokenizes call directive`() {
        val tokens = tokenize("call GET /api/users")
        assertContains(tokens, BerryCrushTokenTypes.CALL)
        assertContains(tokens, BerryCrushTokenTypes.TEXT)
    }

    @Test
    fun `test lexer tokenizes assert directive`() {
        val tokens = tokenize("assert status 200")
        assertContains(tokens, BerryCrushTokenTypes.ASSERT)
        assertContains(tokens, BerryCrushTokenTypes.STATUS)
        assertContains(tokens, BerryCrushTokenTypes.NUMBER)
    }

    @Test
    fun `test lexer tokenizes string literal`() {
        val tokens = tokenize("""body: "hello world"""")
        assertContains(tokens, BerryCrushTokenTypes.STRING)
    }

    @Test
    fun `test lexer tokenizes variable reference`() {
        val tokens = tokenize("call GET /api/users/{{userId}}")
        assertContains(tokens, BerryCrushTokenTypes.VARIABLE)
    }

    @Test
    fun `test lexer tokenizes JSON path`() {
        val tokens = tokenize("extract userId from $.data.id")
        assertContains(tokens, BerryCrushTokenTypes.EXTRACT)
        assertContains(tokens, BerryCrushTokenTypes.JSON_PATH)
    }

    @Test
    fun `test lexer tokenizes tags`() {
        val tokens = tokenize("@api @critical")
        assertContains(tokens, BerryCrushTokenTypes.TAG)
    }

    @Test
    fun `test lexer tokenizes comment`() {
        val tokens = tokenize("# This is a comment")
        assertContains(tokens, BerryCrushTokenTypes.COMMENT)
    }

    @Test
    fun `test lexer tokenizes include directive`() {
        val tokens = tokenize("include ^auth.login")
        assertContains(tokens, BerryCrushTokenTypes.INCLUDE)
        assertContains(tokens, BerryCrushTokenTypes.OPERATION_REF)
    }

    @Test
    fun `test lexer tokenizes call with operation reference`() {
        val tokens = tokenize("call ^listPets")
        assertContains(tokens, BerryCrushTokenTypes.CALL)
        assertContains(tokens, BerryCrushTokenTypes.OPERATION_REF)
    }

    @Test
    fun `test lexer tokenizes step keywords with colon syntax`() {
        val input = """
            given: user is on login page
            when: user enters credentials
            then: user is logged in
        """.trimIndent()
        val tokens = tokenize(input)

        assertContains(tokens, BerryCrushTokenTypes.GIVEN)
        assertContains(tokens, BerryCrushTokenTypes.WHEN)
        assertContains(tokens, BerryCrushTokenTypes.THEN)
    }

    @Test
    fun `test lexer tokenizes fragment keyword`() {
        val tokens = tokenize("fragment: common_auth")
        assertContains(tokens, BerryCrushTokenTypes.FRAGMENT)
    }

    @Test
    fun `test lexer tokenizes HTTP methods`() {
        val methods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS")
        for (method in methods) {
            val tokens = tokenize("call $method /api")
            assertContains(tokens, BerryCrushTokenTypes.TEXT, "HTTP method $method not tokenized")
        }
    }

    @Test
    fun `test lexer tokenizes conditional keywords`() {
        val input = """
            if status == 200
            else if status == 404
            else
        """.trimIndent()
        val tokens = tokenize(input)
        assertContains(tokens, BerryCrushTokenTypes.IF)
        assertContains(tokens, BerryCrushTokenTypes.ELSE)
    }

    @Test
    fun `test lexer tokenizes numbers`() {
        val tokens = tokenize("assert responseTime <= 5000")
        assertContains(tokens, BerryCrushTokenTypes.NUMBER)
    }

    @Test
    fun `test full scenario tokenization`() {
        val scenario = """
            @api @smoke
            feature: User API
            
            scenario: Get user by ID
                given API is available
                    call GET /api/users/1
                then response is successful
                    assert status 200
                    assert header Content-Type contains application/json
                    extract userId from $.id
        """.trimIndent()

        val tokens = tokenize(scenario)

        assertContains(tokens, BerryCrushTokenTypes.TAG)
        assertContains(tokens, BerryCrushTokenTypes.FEATURE)
        assertContains(tokens, BerryCrushTokenTypes.SCENARIO)
        assertContains(tokens, BerryCrushTokenTypes.GIVEN)
        assertContains(tokens, BerryCrushTokenTypes.CALL)
        assertContains(tokens, BerryCrushTokenTypes.TEXT)
        assertContains(tokens, BerryCrushTokenTypes.THEN)
        assertContains(tokens, BerryCrushTokenTypes.ASSERT)
        assertContains(tokens, BerryCrushTokenTypes.STATUS)
        assertContains(tokens, BerryCrushTokenTypes.NUMBER)
        assertContains(tokens, BerryCrushTokenTypes.EXTRACT)
        assertContains(tokens, BerryCrushTokenTypes.JSON_PATH)
    }

    @Test
    fun `test lexer tokenizes capitalized step keywords`() {
        val input = """
            Given user is logged in
            When user clicks button
            Then result is shown
        """.trimIndent()
        val tokens = tokenize(input)

        assertContains(tokens, BerryCrushTokenTypes.GIVEN, "Given should be tokenized")
        assertContains(tokens, BerryCrushTokenTypes.WHEN, "When should be tokenized")
        assertContains(tokens, BerryCrushTokenTypes.THEN, "Then should be tokenized")
    }

    @Test
    fun `test lexer tokenizes capitalized Fragment keyword`() {
        val input = """
            Fragment: my-fragment
            Given step one
            When step two
        """.trimIndent()
        val tokens = tokenize(input)

        assertContains(tokens, BerryCrushTokenTypes.FRAGMENT, "Fragment should be tokenized")
        assertContains(tokens, BerryCrushTokenTypes.NEWLINE, "Should have newline tokens")
        assertContains(tokens, BerryCrushTokenTypes.GIVEN, "Given should be tokenized")
        assertContains(tokens, BerryCrushTokenTypes.WHEN, "When should be tokenized")
    }

    // --- Helper functions ---

    private fun tokenize(text: String): List<Pair<IElementType?, String>> {
        val lexer = BerryCrushLexer()
        lexer.start(text)

        val tokens = mutableListOf<Pair<IElementType?, String>>()
        while (lexer.tokenType != null) {
            tokens.add(Pair(lexer.tokenType, lexer.tokenText))
            lexer.advance()
        }
        return tokens
    }

    private fun assertContains(
        tokens: List<Pair<IElementType?, String>>,
        expectedType: IElementType,
        message: String = "Expected token type $expectedType not found"
    ) {
        val found = tokens.any { it.first == expectedType }
        assert(found) { "$message\nTokens: ${tokens.map { "${it.first} '${it.second}'" }}" }
    }
}

package org.berrycrush.intellij.formatting

import org.junit.jupiter.api.Test

class BerryCrushIfDirectiveFormattingTest : BerryCrushFormattingTestCase() {
    @Test
    fun `if-else directive formatting`() {
        val expected = """
            scenario: if-else directive formatting bug
              when I call operation
                call using api ^operation
              then extract values
                if status 200
                  extract $.id => id
                else
                  extract $[0].id => id
        """.trimIndent()
        doIdempotencyTest(expected, expected)
    }

    @Test
    fun `malformed else indentation is normalized`() {
        val input = """
            scenario: if-else directive formatting bug
              when I call operation
                call using api ^operation
              then extract values
                if status 200
                  extract $.id => id
                  else
                    extract $[0].id => id
        """.trimIndent()

        val expected = """
            scenario: if-else directive formatting bug
              when I call operation
                call using api ^operation
              then extract values
                if status 200
                  extract $.id => id
                else
                  extract $[0].id => id
        """.trimIndent()

        doIdempotencyTest(input, expected)
    }

    @Test
    fun `extract branch formatting keeps else aligned`() {
        val input = """
            scenario: extract conditional
            when I call
            call ^op
            then extract values
            if status 2xx
            extract $.id => id
            else
            extract $[0].id => id
        """.trimIndent()

        val expected = """
            scenario: extract conditional
              when I call
                call ^op
              then extract values
                if status 2xx
                  extract $.id => id
                else
                  extract $[0].id => id
        """.trimIndent()

        doIdempotencyTest(input, expected)
    }

    @Test
    fun `assert branch formatting keeps else aligned`() {
        val input = """
            scenario: assert conditional
            then verify values
            if status 2xx
            assert $.id exists
            else
            assert status 5xx
        """.trimIndent()

        val expected = """
            scenario: assert conditional
              then verify values
                if status 2xx
                  assert $.id exists
                else
                  assert status 5xx
        """.trimIndent()

        doIdempotencyTest(input, expected)
    }

    @Test
    fun `else-if chain remains aligned`() {
        val input = """
            scenario: chained conditional
            then validate status
            if status 200
            assert $.id exists
            else if status 201
            assert $.created exists
            else
            fail "unexpected status"
        """.trimIndent()

        val expected = """
            scenario: chained conditional
              then validate status
                if status 200
                  assert $.id exists
                else if status 201
                  assert $.created exists
                else
                  fail "unexpected status"
        """.trimIndent()

        doIdempotencyTest(input, expected)
    }
}

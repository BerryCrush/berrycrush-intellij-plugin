package org.berrycrush.intellij.formatting

import org.junit.jupiter.api.Test

class BerryCrushCallDirectiveFormattingTest: BerryCrushFormattingTestCase() {
    @Test
    fun `call directive should be indented one level down`() {
        val input = """
            scenario: call directive
            call: "GET /pets"
            bodyFile: "request.json"
        """.trimIndent()
        val expected = """
            scenario: call directive
              call: "GET /pets"
                bodyFile: "request.json"
        """.trimIndent()
        doIdempotencyTest(input, expected)
    }
}
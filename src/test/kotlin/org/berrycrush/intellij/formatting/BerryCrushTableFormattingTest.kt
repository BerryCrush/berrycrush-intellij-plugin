package org.berrycrush.intellij.formatting

import org.junit.jupiter.api.Test

class BerryCrushTableFormattingTest : BerryCrushFormattingTestCase() {

    // === Table Alignment ===

    @Test
    fun testTableColumnAlignment() {
        val input =
            """
            examples:
            | name | value |
            | foo| 1 |
            """.trimIndent()

        val expected =
            """
            examples:
              | name | value |
              | foo  | 1     |
            """.trimIndent()

        doFormattingTest(input, expected)
    }

    @Test
    fun testTableAlignmentWithUnevenColumns() {
        val input =
            """
            examples:
            |petId|value|
            |1|fluffy|
            |123|short|
            """.trimIndent()

        val expected =
            """
            examples:
              | petId | value  |
              | 1     | fluffy |
              | 123   | short  |
            """.trimIndent()

        doFormattingTest(input, expected)
    }

    // === Complex Scenarios ===

    @Test
    fun testComplexScenarioFormatting() {
        val input =
            """
            scenario: test file for copilot
                    include  verify_pet_by_id
                   petId: 1

            outline: foo
                when  I do something
                    call  ^getPetById
             petId: {{petId}}
              examples:
             | petId | value  |
             | 1     | fluffy |
            """.trimIndent()

        val expected =
            """
            scenario: test file for copilot
              include verify_pet_by_id
                petId: 1

            outline: foo
              when I do something
                call ^getPetById
                  petId: {{petId}}
              examples:
                | petId | value  |
                | 1     | fluffy |
            """.trimIndent()

        doFormattingTest(input, expected)
    }

    @Test
    fun testOutlineExamplesIndentation() {
        val input =
            listOf(
                "outline: outline name",
                "given prerequisite",
                "when {{label1}} is {{label2}}",
                "examples:",
                "| label1 | label2 |",
                "| value1 | value2 |",
            ).joinToString("\n")

        val expected =
            listOf(
                "outline: outline name",
                "  given prerequisite",
                "  when {{label1}} is {{label2}}",
                "  examples:",
                "    | label1 | label2 |",
                "    | value1 | value2 |",
            ).joinToString("\n")

        doFormattingTest(input, expected)
    }
}

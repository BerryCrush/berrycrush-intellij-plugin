package org.berrycrush.intellij.formatting

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class BerryCrushCommentFormattingTest: BerryCrushFormattingTestCase() {
    @Disabled("comment formatting issue, but I'm currently working on other things")
    @Test
    fun testCommentAlignment() {
        val input =
            """
            scenario: test comment alignment
            # This is a comment
            # that should be aligned
            # with the scenario keyword
            given something
            """.trimIndent()

        val expected =
            """
            scenario: test comment alignment
              # This is a comment
              # that should be aligned
              # with the scenario keyword
              given something
            """.trimIndent()

        doFormattingTest(input, expected)
    }

    @Test
    fun testDetachedMultiLineCommentsStayAtRootLevel() {
        val input =
            listOf(
                "feature: bla",
                "  scenario: in feature",
                "    given end",
                "#-- comment 1",
                "#-- comment 2",
                "#-- comment 3",
                "",
                "scenario: bla",
            ).joinToString("\n")

        val expected =
            listOf(
                "feature: bla",
                "  scenario: in feature",
                "    given end",
                "#-- comment 1",
                "#-- comment 2",
                "#-- comment 3",
                "",
                "scenario: bla",
            ).joinToString("\n")

        doFormattingTest(input, expected)
    }

}
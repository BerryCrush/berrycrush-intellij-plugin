package com.berrycrush.intellij.inspection

class SyntaxCheckInspectionTest : BerryCrushInspectionTestCase(SyntaxCheckInspection()) {
    fun testInvalidFeatureWithUnknownKeyword() {
        // Step followed by directive should not be flagged
        val psiFile =
            myFixture.addFileToProject(
                "test.scenario",
                """
                feature: feature0
                  description: blabla
                  scenario: scenario0
                """.trimIndent(),
            )

        val problems = runInspection(psiFile)
        assertTrue(
            "unknown keyword with directive should be flagged",
            problems.isNotEmpty(),
        )
    }

    fun testCommentShouldNotCauseError() {
        // Step followed by directive should not be flagged
        val psiFile =
            myFixture.addFileToProject(
                "test.scenario",
                """
                # comment
                feature: feature0
                  # comment description: blabla
                  scenario: scenario0
                    # comment
                    given boo
                """.trimIndent(),
            )

        val problems = runInspection(psiFile)
        assertTrue(
            "unknown keyword with directive should be flagged",
            problems.isEmpty(),
        )
    }

    fun testParameterShouldNotCauseError() {
        // Step followed by directive should not be flagged
        val psiFile =
            myFixture.addFileToProject(
                "test.scenario",
                """
                parameters:
                  id: ok
                feature: feature0
                  parameters:
                    foo: bar
                  scenario: scenario0
                    parameters:
                      buz: buz
                    given boo
                """.trimIndent(),
            )

        val problems = runInspection(psiFile)
        assertTrue(
            "unknown keyword with directive should be flagged",
            problems.isEmpty(),
        )
    }
}

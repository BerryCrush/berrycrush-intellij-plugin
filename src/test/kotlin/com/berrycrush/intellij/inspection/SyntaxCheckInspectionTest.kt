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

    fun testExamplesShouldNotCauseError() {
        // Step followed by directive should not be flagged
        val psiFile =
            myFixture.addFileToProject(
                "test.scenario",
                """
                outline: bla
                  examples:
                  | name | value |
                  | foo  | bar   |
                """.trimIndent(),
            )

        val problems = runInspection(psiFile)
        assertTrue(
            "unknown keyword with directive should be flagged",
            problems.isEmpty(),
        )
    }

    fun testBackgroundShouldNotCauseError() {
        // Step followed by directive should not be flagged
        val psiFile =
            myFixture.addFileToProject(
                "test.scenario",
                """
                feature: feature0
                  background:
                    given background call
                      call ^operatorId
                      extract $.id => id
                  # scenario
                  scenario: scenario0
                    given depends on background
                      call ^operatorId1
                        id: {{id}}
                """.trimIndent(),
            )

        val problems = runInspection(psiFile)
        assertTrue(
            "unknown keyword with directive should be flagged",
            problems.isEmpty(),
        )
    }

    fun testValidFragmentShouldNotCauseError() {
        val psiFile =
            myFixture.addFileToProject(
                "auth.fragment",
                """
                fragment: authenticate
                  given setup auth
                    call ^login
                      username: demo
                      password: secret
                  include common_headers
                    token: {{token}}
                """.trimIndent(),
            )

        val problems = runInspection(psiFile)
        assertTrue(
            "valid fragment should not be flagged",
            problems.isEmpty(),
        )
    }

    fun testScenarioBlockInFragmentFileShouldCauseError() {
        val psiFile =
            myFixture.addFileToProject(
                "invalid.fragment",
                """
                scenario: not allowed here
                  given anything
                """.trimIndent(),
            )

        val problems = runInspection(psiFile)
        assertTrue(
            "scenario block in fragment file should be flagged",
            problems.isNotEmpty(),
        )
    }

    fun testStepConditionalIfElseShouldNotCauseError() {
        val psiFile =
            myFixture.addFileToProject(
                "conditional.scenario",
                """
                scenario: conditional assertions
                  then validate status
                    if status 2xx
                      assert $.id exists
                    else
                      assert status 5xx
                """.trimIndent(),
            )

        val problems = runInspection(psiFile)
        assertTrue(
            "valid step-level if/else conditional assertions should not be flagged",
            problems.isEmpty(),
        )
    }
}

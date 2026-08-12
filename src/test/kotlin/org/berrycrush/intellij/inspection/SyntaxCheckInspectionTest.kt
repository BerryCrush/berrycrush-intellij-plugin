package org.berrycrush.intellij.inspection

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
                @api @experimental
                feature: feature0
                  @api @smoke
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

    fun testCommentsInDirectivePayloadAndExamplesShouldNotCauseError() {
        val psiFile =
            myFixture.addFileToProject(
                "commented-structures.scenario",
                """
                outline: commented outline
                  given call with comments
                    call ^operationId
                      id: {{petId}}
                      # comment between payload fields
                      body:
                        # nested body comment
                        name: foo
                  examples:
                    # comment before header row
                    | id | name |
                    # comment before value row
                    | 1  | foo  |
                """.trimIndent(),
            )

        val problems = runInspection(psiFile)
        assertTrue(
            "valid comment-heavy outline/directive content should not be flagged",
            problems.isEmpty(),
        )
    }

    fun testCommentBetweenCallAndBodyShouldNotCauseError() {
        val psiFile =
            myFixture.addFileToProject(
                "comment-between-call-and-body.scenario",
                """
                scenario: Body syntax - structured body
                  when I create a pet with structured body
                    call ^createPet
                    # Uses OpenAPI schema defaults for unspecified fields
                      body:
                        name: StructuredBodyPet
                        status: pending
                """.trimIndent(),
            )

        val problems = runInspection(psiFile)
        assertTrue(
            "body payload following an inline comment should stay in call block",
            problems.isEmpty(),
        )
    }

    fun testCommentsInFragmentShouldNotCauseError() {
        val psiFile =
            myFixture.addFileToProject(
                "commented.fragment",
                """
                fragment: commented
                  # comment before first step
                  given setup
                    # comment before directive
                    call ^login
                      # comment between parameters
                      username: demo
                """.trimIndent(),
            )

        val problems = runInspection(psiFile)
        assertTrue(
            "valid comment-heavy fragment content should not be flagged",
            problems.isEmpty(),
        )
    }

    fun testNonIdentifierFragmentNameShouldCauseError() {
        val psiFile =
            myFixture.addFileToProject(
                "test.fragment",
                """
                fragment: part1 part2
                  given setup
                    call ^login
                      username: demo
                """.trimIndent(),
            )

        val problems = runInspection(psiFile)
        assertTrue(
            "non identifer fragment name should be flagged",
            problems.isNotEmpty(),
        )
    }

    fun testCallDirectiveInStep() {
        // Step followed by directive should not be flagged
        val psiFile =
            myFixture.addFileToProject(
                "test.scenario",
                """
                scenario: Body syntax - multi-line triple-quoted
                  when: I create a pet with multi-line body
                    call ^createPet
                      body:
                        ""${'"'}
                        {
                           "name": "MultiLinePet",
                           "status": "available",
                           "tags": ["cute", "friendly"]
                        }
                        ""${'"'}
                 then: the pet is created
                   assert status 2xx
                   assert $.name equals "MultiLinePet"
                """.trimIndent(),
            )

        val problems = runInspection(psiFile)
        assertTrue(
            "body with multiline should not be flagged",
            problems.isEmpty(),
        )
    }
}

package com.berrycrush.intellij.inspection

import com.berrycrush.intellij.BerryCrushTestCase
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiManager

/**
 * Tests for UndefinedStepInspection.
 *
 * Tests the actual inspection behavior using the IntelliJ testing framework.
 */
class UndefinedStepInspectionTest : BerryCrushTestCase() {

    private val inspection = UndefinedStepInspection()

    // ========== Inspection Properties Tests ==========

    fun testInspectionDisplayName() {
        assertEquals("Undefined custom step", inspection.displayName)
    }

    fun testInspectionShortName() {
        assertEquals("BerryCrushUndefinedStep", inspection.shortName)
    }

    fun testInspectionGroupDisplayName() {
        assertEquals("BerryCrush", inspection.groupDisplayName)
    }

    fun testInspectionEnabledByDefault() {
        assertTrue("Should be enabled by default", inspection.isEnabledByDefault)
    }

    // ========== Step Detection Tests ==========

    fun testNoProblemsForStepWithDirective() {
        // Step followed by directive should not be flagged
        val psiFile = myFixture.addFileToProject("test.scenario", """
            scenario: test
              given: setup
                call GET /api/users
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertTrue(
            "Step with directive should not be flagged",
            problems.isEmpty()
        )
    }

    fun testNoProblemsForEmptyStepText() {
        // Empty step text should not be flagged
        val psiFile = myFixture.addFileToProject("test2.scenario", """
            scenario: test
              given:
                call GET /api/users
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertTrue(
            "Empty step should not be flagged",
            problems.isEmpty()
        )
    }

    fun testProblemForUndefinedStepWithoutDirective() {
        // Step without directive and no matching @Step should be flagged
        val psiFile = myFixture.addFileToProject("test3.scenario", """
            scenario: test
              given: I have a pet named Fluffy
              when: user action
                call GET /api/users
        """.trimIndent())

        val problems = runInspection(psiFile)
        // Note: The step "I have a pet named Fluffy" has no following directive
        // and no @Step definition, so it should be flagged
        val undefinedStepProblems = problems.filter {
            it.descriptionTemplate.contains("has no matching @Step definition")
        }
        assertTrue(
            "Step without directive should be flagged",
            undefinedStepProblems.isNotEmpty()
        )
    }

    fun testAllStepKeywordsRecognized() {
        // All step keywords should be recognized
        val psiFile = myFixture.addFileToProject("test4.scenario", """
            scenario: test
              given: first step
                call GET /api
              when: second step
                call POST /api
              then: third step
                assert status 200
              and: fourth step
                assert header Content-Type
              but: fifth step
                extract $.id
        """.trimIndent())

        val problems = runInspection(psiFile)
        // All steps have directives, so no problems
        assertTrue(
            "Steps with directives should not be flagged",
            problems.isEmpty()
        )
    }

    fun testCaseInsensitiveStepKeywords() {
        // Step keywords should be case insensitive
        val psiFile = myFixture.addFileToProject("test5.scenario", """
            scenario: test
              Given: setup with directive
                call GET /api
              WHEN: action with directive
                call POST /api
              Then: verification with directive
                assert status 200
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertTrue(
            "Capitalized step keywords should be recognized",
            problems.isEmpty()
        )
    }

    // ========== Comment Skipping Tests ==========

    fun testNoProblemsForStepWithCommentThenDirective() {
        // Step followed by comment then directive should not be flagged
        val psiFile = myFixture.addFileToProject("test_comment1.scenario", """
            scenario: test
              then: I get this
                # comment
                assert status 200
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertTrue(
            "Step with comment then directive should not be flagged",
            problems.isEmpty()
        )
    }

    fun testNoProblemsForStepWithMultipleCommentsThenDirective() {
        // Step followed by multiple comments then directive should not be flagged
        val psiFile = myFixture.addFileToProject("test_comment2.scenario", """
            scenario: test
              then: verify response
                # first comment
                # second comment
                assert status 200
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertTrue(
            "Step with multiple comments then directive should not be flagged",
            problems.isEmpty()
        )
    }

    fun testNoProblemsForStepWithBlankLinesThenDirective() {
        // Step followed by blank lines then directive should not be flagged
        val psiFile = myFixture.addFileToProject("test_blank.scenario", """
            scenario: test
              then: verify response
            
                assert status 200
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertTrue(
            "Step with blank lines then directive should not be flagged",
            problems.isEmpty()
        )
    }

    fun testProblemForStepWithOnlyComments() {
        // Step followed by only comments (no directive) should be flagged
        val psiFile = myFixture.addFileToProject("test_onlycomments.scenario", """
            scenario: test
              then: I expect something
                # just a comment
                # another comment
              when: next step
                call GET /api
        """.trimIndent())

        val problems = runInspection(psiFile)
        val undefinedStepProblems = problems.filter {
            it.descriptionTemplate.contains("has no matching @Step definition")
        }
        assertTrue(
            "Step with only comments should be flagged",
            undefinedStepProblems.isNotEmpty()
        )
    }

    // ========== Non-BerryCrush Files Tests ==========

    fun testIgnoresNonBerryCrushFiles() {
        // Inspection should skip non-BerryCrush files
        val psiFile = myFixture.addFileToProject("test.txt", """
            given: this is plain text
            when: not a scenario
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertTrue(
            "Non-BerryCrush files should be ignored",
            problems.isEmpty()
        )
    }

    // ========== Helper Methods ==========

    private fun runInspection(file: com.intellij.psi.PsiFile): List<ProblemDescriptor> {
        val manager = InspectionManager.getInstance(project)
        val holder = ProblemsHolder(manager, file, false)
        val visitor = inspection.buildVisitor(holder, false)
        visitor.visitFile(file)
        return holder.results
    }
}


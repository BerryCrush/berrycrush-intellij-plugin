package com.berrycrush.intellij.inspection

import com.berrycrush.intellij.BerryCrushTestCase
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder

/**
 * Tests for UndefinedAssertionInspection.
 *
 * Tests the actual inspection behavior using the IntelliJ testing framework.
 */
class UndefinedAssertionInspectionTest : BerryCrushTestCase() {

    private val inspection = UndefinedAssertionInspection()

    // ========== Inspection Properties Tests ==========

    fun testInspectionDisplayName() {
        assertEquals("Undefined assertion", inspection.displayName)
    }

    fun testInspectionShortName() {
        assertEquals("BerryCrushUndefinedAssertion", inspection.shortName)
    }

    fun testInspectionGroupDisplayName() {
        assertEquals("BerryCrush", inspection.groupDisplayName)
    }

    fun testInspectionEnabledByDefault() {
        assertTrue("Should be enabled by default", inspection.isEnabledByDefault)
    }

    // ========== Assertion Detection Tests ==========

    fun testProblemForUndefinedAssertion() {
        // Assert without matching @Assertion should be flagged
        val psiFile = myFixture.addFileToProject("test.scenario", """
            scenario: test
              then: verify
                assert response is valid
        """.trimIndent())

        val problems = runInspection(psiFile)
        val undefinedAssertionProblems = problems.filter {
            it.descriptionTemplate.contains("has no matching @Assertion definition")
        }
        assertTrue(
            "Undefined assertion should be flagged",
            undefinedAssertionProblems.isNotEmpty()
        )
    }

    fun testEmptyAssertionTextNotFlagged() {
        // Empty assertion text should not be flagged
        val psiFile = myFixture.addFileToProject("test2.scenario", """
            scenario: test
              then: verify
                assert
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertTrue(
            "Empty assertion should not be flagged",
            problems.isEmpty()
        )
    }

    fun testCaseInsensitiveAssertKeyword() {
        // Assert keyword should be case insensitive
        val psiFile = myFixture.addFileToProject("test3.scenario", """
            scenario: test
              then: verify
                ASSERT response is valid
        """.trimIndent())

        val problems = runInspection(psiFile)
        val undefinedAssertionProblems = problems.filter {
            it.descriptionTemplate.contains("has no matching @Assertion definition")
        }
        assertTrue(
            "ASSERT keyword should be recognized",
            undefinedAssertionProblems.isNotEmpty()
        )
    }

    fun testAssertInProseNotFlagged() {
        // "assert" in prose (not at line start) should not be flagged
        val psiFile = myFixture.addFileToProject("test4.scenario", """
            scenario: test
              given: user will assert their rights
                call GET /api/users
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertTrue(
            "Assert in prose should not be flagged",
            problems.isEmpty()
        )
    }

    fun testMultipleAssertionsEachFlagged() {
        // Each undefined assertion should be flagged
        val psiFile = myFixture.addFileToProject("test5.scenario", """
            scenario: test
              then: verify
                assert first condition
                assert second condition
                assert third condition
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertEquals(
            "All undefined assertions should be flagged",
            3,
            problems.size
        )
    }

    fun testAssertionWithQuickFix() {
        // Undefined assertion should have a quick fix
        val psiFile = myFixture.addFileToProject("test6.scenario", """
            scenario: test
              then: verify
                assert custom check
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertTrue("Should have problems", problems.isNotEmpty())

        val problem = problems.first()
        val fixes = problem.fixes
        assertNotNull("Should have quick fix", fixes)
        assertTrue("Should have at least one fix", fixes?.isNotEmpty() == true)
    }

    // ========== Built-in Assertion Tests ==========

    fun testBuiltInStatusAssertionNotFlagged() {
        val psiFile = myFixture.addFileToProject("builtin1.scenario", """
            scenario: test
              then: verify
                assert status 200
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertTrue(
            "Built-in status assertion should not be flagged",
            problems.isEmpty()
        )
    }

    fun testBuiltInStatusCodeAssertionNotFlagged() {
        val psiFile = myFixture.addFileToProject("builtin2.scenario", """
            scenario: test
              then: verify
                assert statusCode 404
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertTrue(
            "Built-in statusCode assertion should not be flagged",
            problems.isEmpty()
        )
    }

    fun testBuiltInContainsAssertionNotFlagged() {
        val psiFile = myFixture.addFileToProject("builtin3.scenario", """
            scenario: test
              then: verify
                assert contains "hello"
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertTrue(
            "Built-in contains assertion should not be flagged",
            problems.isEmpty()
        )
    }

    fun testBuiltInNotContainsAssertionNotFlagged() {
        val psiFile = myFixture.addFileToProject("builtin4.scenario", """
            scenario: test
              then: verify
                assert not contains "error"
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertTrue(
            "Built-in not contains assertion should not be flagged",
            problems.isEmpty()
        )
    }

    fun testBuiltInJsonPathAssertionNotFlagged() {
        val psiFile = myFixture.addFileToProject("builtin5.scenario", """
            scenario: test
              then: verify
                assert $.name equals "John"
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertTrue(
            "Built-in JSONPath equals assertion should not be flagged",
            problems.isEmpty()
        )
    }

    fun testBuiltInJsonPathShorthandAssertionNotFlagged() {
        val psiFile = myFixture.addFileToProject("builtin6.scenario", """
            scenario: test
              then: verify
                assert $.id = 123
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertTrue(
            "Built-in JSONPath shorthand assertion should not be flagged",
            problems.isEmpty()
        )
    }

    fun testBuiltInHeaderAssertionNotFlagged() {
        val psiFile = myFixture.addFileToProject("builtin7.scenario", """
            scenario: test
              then: verify
                assert header Content-Type
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertTrue(
            "Built-in header assertion should not be flagged",
            problems.isEmpty()
        )
    }

    fun testBuiltInHeaderValueAssertionNotFlagged() {
        val psiFile = myFixture.addFileToProject("builtin8.scenario", """
            scenario: test
              then: verify
                assert header Content-Type = "application/json"
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertTrue(
            "Built-in header value assertion should not be flagged",
            problems.isEmpty()
        )
    }

    fun testBuiltInResponseTimeAssertionNotFlagged() {
        val psiFile = myFixture.addFileToProject("builtin9.scenario", """
            scenario: test
              then: verify
                assert responseTime 1000
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertTrue(
            "Built-in responseTime assertion should not be flagged",
            problems.isEmpty()
        )
    }

    fun testBuiltInSchemaAssertionNotFlagged() {
        val psiFile = myFixture.addFileToProject("builtin10.scenario", """
            scenario: test
              then: verify
                assert schema
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertTrue(
            "Built-in schema assertion should not be flagged",
            problems.isEmpty()
        )
    }

    fun testBuiltInJsonPathNotExistsAssertionNotFlagged() {
        val psiFile = myFixture.addFileToProject("builtin11.scenario", """
            scenario: test
              then: verify
                assert $.error not exists
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertTrue(
            "Built-in JSONPath not exists assertion should not be flagged",
            problems.isEmpty()
        )
    }

    fun testCustomAssertionStillFlagged() {
        // Custom assertions that don't match built-in patterns should still be flagged
        val psiFile = myFixture.addFileToProject("custom1.scenario", """
            scenario: test
              then: verify
                assert my custom validation
        """.trimIndent())

        val problems = runInspection(psiFile)
        val undefinedAssertionProblems = problems.filter {
            it.descriptionTemplate.contains("has no matching @Assertion definition")
        }
        assertTrue(
            "Custom assertion should still be flagged",
            undefinedAssertionProblems.isNotEmpty()
        )
    }

    // ========== Non-BerryCrush Files Tests ==========

    fun testIgnoresNonBerryCrushFiles() {
        val psiFile = myFixture.addFileToProject("test.txt", """
            assert something
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


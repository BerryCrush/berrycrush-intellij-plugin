package com.berrycrush.intellij.inspection

import com.berrycrush.intellij.BerryCrushTestCase
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder

/**
 * Tests for MissingFragmentInspection.
 *
 * Tests the actual inspection behavior using the IntelliJ testing framework.
 */
class MissingFragmentInspectionTest : BerryCrushTestCase() {

    private val inspection = MissingFragmentInspection()

    // ========== Inspection Properties Tests ==========

    fun testInspectionDisplayName() {
        assertEquals("Missing fragment reference", inspection.displayName)
    }

    fun testInspectionShortName() {
        assertEquals("BerryCrushMissingFragment", inspection.shortName)
    }

    fun testInspectionGroupDisplayName() {
        assertEquals("BerryCrush", inspection.groupDisplayName)
    }

    fun testInspectionEnabledByDefault() {
        assertTrue("Should be enabled by default", inspection.isEnabledByDefault)
    }

    // ========== Fragment Detection Tests ==========

    fun testProblemForMissingFragment() {
        // Include referencing non-existent fragment should be flagged
        val psiFile = myFixture.addFileToProject("test.scenario", """
            scenario: test
              given: setup
                include missing-fragment
        """.trimIndent())

        val problems = runInspection(psiFile)
        val missingFragmentProblems = problems.filter {
            it.descriptionTemplate.contains("not found")
        }
        assertTrue(
            "Missing fragment should be flagged",
            missingFragmentProblems.isNotEmpty()
        )
    }

    fun testNoProblemsForExistingFragment() {
        // Create the fragment first
        myFixture.addFileToProject("existing.fragment", """
            fragment: existing-fragment
              given: some step
                call GET /api
        """.trimIndent())

        // Create scenario that includes it
        val psiFile = myFixture.addFileToProject("test2.scenario", """
            scenario: test
              given: setup
                include existing-fragment
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertTrue(
            "Existing fragment should not be flagged",
            problems.isEmpty()
        )
    }

    fun testIncludePatternMatching() {
        // Include with various whitespace should be detected
        val psiFile = myFixture.addFileToProject("test3.scenario", """
            scenario: test
              given: setup
                include fragment-one
                  include fragment-two
            include fragment-three
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertEquals(
            "All missing fragments should be flagged",
            3,
            problems.size
        )
    }

    fun testMissingFragmentHasQuickFix() {
        // Missing fragment should have a quick fix to create it
        val psiFile = myFixture.addFileToProject("test4.scenario", """
            scenario: test
              given: setup
                include new-fragment
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertTrue("Should have problems", problems.isNotEmpty())

        val problem = problems.first()
        val fixes = problem.fixes
        assertNotNull("Should have quick fix", fixes)
        assertTrue("Should have at least one fix", fixes?.isNotEmpty() == true)
    }

    fun testMissingFragmentSeverity() {
        // Missing fragment should have ERROR severity (blocking)
        val psiFile = myFixture.addFileToProject("test5.scenario", """
            scenario: test
              given: setup
                include undefined-fragment
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertTrue("Should have problems", problems.isNotEmpty())

        val problem = problems.first()
        assertEquals(
            "Missing fragment should be ERROR severity",
            com.intellij.codeInspection.ProblemHighlightType.ERROR,
            problem.highlightType
        )
    }

    // ========== Non-BerryCrush Files Tests ==========

    fun testIgnoresNonBerryCrushFiles() {
        val psiFile = myFixture.addFileToProject("test.txt", """
            include missing-fragment
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


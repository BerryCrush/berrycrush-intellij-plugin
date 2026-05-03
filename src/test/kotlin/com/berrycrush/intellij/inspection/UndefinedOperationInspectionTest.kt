package com.berrycrush.intellij.inspection

import com.berrycrush.intellij.BerryCrushTestCase
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder

/**
 * Tests for UndefinedOperationInspection.
 *
 * Tests the actual inspection behavior using the IntelliJ testing framework.
 */
class UndefinedOperationInspectionTest : BerryCrushTestCase() {

    private val inspection = UndefinedOperationInspection()

    // ========== Inspection Properties Tests ==========

    fun testInspectionDisplayName() {
        assertEquals("Undefined OpenAPI operation", inspection.displayName)
    }

    fun testInspectionShortName() {
        assertEquals("BerryCrushUndefinedOperation", inspection.shortName)
    }

    fun testInspectionGroupDisplayName() {
        assertEquals("BerryCrush", inspection.groupDisplayName)
    }

    fun testInspectionEnabledByDefault() {
        assertTrue("Should be enabled by default", inspection.isEnabledByDefault)
    }

    // ========== Operation Detection Tests ==========

    fun testNoProblemsWhenNoOpenAPISpec() {
        // Without OpenAPI spec, inspection should skip
        val psiFile = myFixture.addFileToProject("test.scenario", """
            scenario: test
              given: setup
                call ^unknownOp
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertTrue(
            "Without OpenAPI spec, should not flag operations",
            problems.isEmpty()
        )
    }

    fun testNoProblemsForDefinedOperation() {
        // Create OpenAPI spec with operation
        myFixture.addFileToProject("openapi.yaml", """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            paths:
              /pets:
                get:
                  operationId: listPets
                  responses:
                    '200':
                      description: OK
        """.trimIndent())

        val psiFile = myFixture.addFileToProject("test2.scenario", """
            scenario: test
              given: setup
                call ^listPets
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertTrue(
            "Defined operation should not be flagged",
            problems.isEmpty()
        )
    }

    fun testProblemForUndefinedOperation() {
        // Create OpenAPI spec without the referenced operation
        myFixture.addFileToProject("openapi2.yaml", """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            paths:
              /pets:
                get:
                  operationId: listPets
                  responses:
                    '200':
                      description: OK
        """.trimIndent())

        val psiFile = myFixture.addFileToProject("test3.scenario", """
            scenario: test
              given: setup
                call ^undefinedOp
        """.trimIndent())

        val problems = runInspection(psiFile)
        val undefinedOpProblems = problems.filter {
            it.descriptionTemplate.contains("not found in OpenAPI specs")
        }
        assertTrue(
            "Undefined operation should be flagged",
            undefinedOpProblems.isNotEmpty()
        )
    }

    fun testCallWithoutCaretNotFlagged() {
        // call without ^ is HTTP call, not operation reference
        myFixture.addFileToProject("openapi3.yaml", """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            paths:
              /pets:
                get:
                  operationId: listPets
                  responses:
                    '200':
                      description: OK
        """.trimIndent())

        val psiFile = myFixture.addFileToProject("test4.scenario", """
            scenario: test
              given: setup
                call GET /api/pets
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertTrue(
            "HTTP call should not be flagged",
            problems.isEmpty()
        )
    }

    // ========== Non-BerryCrush Files Tests ==========

    fun testIgnoresNonBerryCrushFiles() {
        myFixture.addFileToProject("openapi4.yaml", """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            paths: {}
        """.trimIndent())

        val psiFile = myFixture.addFileToProject("test.txt", """
            call ^unknownOp
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


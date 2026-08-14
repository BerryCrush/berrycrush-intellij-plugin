package org.berrycrush.intellij.inspection

import org.junit.jupiter.api.Test

class DuplicateParameterInspectionTest : BerryCrushInspectionTestCase(DuplicateParameterInspection()) {
    @Test
    fun testDuplicateParameterInspectionForInclude() {
        // Step followed by directive should not be flagged
        val psiFile =
            myFixture.addFileToProject(
                "test.scenario",
                """
                scenario: foo
                  given bla
                    include operationId
                      id: 1
                      id: 2
                """.trimIndent(),
            )

        val problems = runInspection(psiFile)
        assertTrue("Duplicate parameter", problems.isNotEmpty())
    }

    @Test
    fun testDuplicateParameterInspectionForCall() {
        // Step followed by directive should not be flagged
        val psiFile =
            myFixture.addFileToProject(
                "test.scenario",
                """
                scenario: foo
                  given bla
                    call ^operationId
                      id: 1
                      id: 2
                """.trimIndent(),
            )

        val problems = runInspection(psiFile)
        assertTrue("Duplicate parameter", problems.isNotEmpty())
    }
}

package org.berrycrush.intellij.inspection

import org.junit.jupiter.api.Test

/**
 * Tests for UndefinedParameterReferenceInspection.
 *
 * Tests the inspection for undefined ${param.name} references.
 */
class UndefinedParameterReferenceInspectionTest : BerryCrushInspectionTestCase(UndefinedParameterReferenceInspection()) {
    // ========== Inspection Properties Tests ==========

    @Test
    fun testInspectionDisplayName() {
        assertEquals("Undefined parameter reference", inspection.displayName)
    }

    @Test
    fun testInspectionShortName() {
        assertEquals("BerryCrushUndefinedParameterReference", inspection.shortName)
    }

    @Test
    fun testInspectionGroupDisplayName() {
        assertEquals("BerryCrush", inspection.groupDisplayName)
    }

    @Test
    fun testInspectionEnabledByDefault() {
        assertTrue("Should be enabled by default", inspection.isEnabledByDefault)
    }

    // ========== Parameter Reference Detection Tests ==========

    @Test
    fun testNoProblemsForDefinedParameter() {
        val psiFile =
            myFixture.addFileToProject(
                "defined-param.scenario",
                """
                scenario: test
                  parameters:
                    timeout: 5000
                  given setup
                    call GET /api?timeout=${"$"}{param.timeout}
                """.trimIndent(),
            )

        val problems = runInspection(psiFile)
        assertTrue(
            "Defined parameter should not be flagged",
            problems.none { it.descriptionTemplate.contains("timeout") },
        )
    }

    @Test
    fun testReportsUndefinedParameterReference() {
        val psiFile =
            myFixture.addFileToProject(
                "undefined-param.scenario",
                """
                scenario: test
                  given setup
                    call GET /api?timeout={{param.undefinedParam}}
                """.trimIndent(),
            )

        val problems = runInspection(psiFile)
        assertTrue(
            "Should report undefined parameter reference, got: ${problems.map { it.descriptionTemplate }}",
            problems.any { it.descriptionTemplate.contains("undefinedParam") },
        )
    }

    @Test
    fun testNoProblemsForEnvVariable() {
        val psiFile =
            myFixture.addFileToProject(
                "env-var.scenario",
                """
                scenario: test
                  parameters:
                    baseUrl: ${"$"}{env.API_URL}
                """.trimIndent(),
            )

        val problems = runInspection(psiFile)
        // env variables should not be flagged (can't validate env vars in IDE)
        assertTrue(
            "env.* should not be flagged as undefined",
            problems.none { it.descriptionTemplate.contains("API_URL") },
        )
    }

    @Test
    fun testNoProblemsForDefinedContextVariable() {
        val psiFile =
            myFixture.addFileToProject(
                "context-var.scenario",
                """
                scenario: test
                  given setup
                    extract userId = $.data.id
                  when using variable
                    call GET /api/users/${"$"}{context.userId}
                """.trimIndent(),
            )

        val problems = runInspection(psiFile)
        assertTrue(
            "Defined context variable should not be flagged",
            problems.none { it.descriptionTemplate.contains("userId") },
        )
    }
}

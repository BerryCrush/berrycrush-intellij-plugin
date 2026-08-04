package com.berrycrush.intellij.inspection

class SyntaxCheckInspectionTest: BerryCrushInspectionTestCase(SyntaxCheckInspection()) {
    fun testInvalidFeatureWithUnknownKeyword() {
        // Step followed by directive should not be flagged
        val psiFile = myFixture.addFileToProject("test.scenario", """
            feature: feature0
              description: blabla
              scenario: scenario0
        """.trimIndent())

        val problems = runInspection(psiFile)
        assertTrue(
            "unknown keyword with directive should be flagged",
            problems.isNotEmpty()
        )
    }
}
package com.berrycrush.intellij.index

import com.berrycrush.intellij.BerryCrushTestCase

/**
 * Unit tests for OperationUsageIndex.
 *
 * Tests the actual index class behavior using the IntelliJ testing framework.
 */
class OperationUsageIndexTest : BerryCrushTestCase() {

    // ========== Index Detection Tests ==========

    fun testIndexesBasicOperationReference() {
        createScenarioFile("test", """
            scenario: test
              given: setup
                call ^createUser
        """.trimIndent())

        val operations = OperationUsageIndex.getAllOperationIds(project)
        assertTrue(
            "Should index 'createUser'",
            operations.contains("createUser")
        )
    }

    fun testIndexesMultipleOperationReferences() {
        createScenarioFile("test2", """
            scenario: test
              given: setup
                call ^createUser
              when: action
                call ^updateUser
              then: verify
                call ^deleteUser
        """.trimIndent())

        val operations = OperationUsageIndex.getAllOperationIds(project)
        assertTrue("Should index 'createUser'", operations.contains("createUser"))
        assertTrue("Should index 'updateUser'", operations.contains("updateUser"))
        assertTrue("Should index 'deleteUser'", operations.contains("deleteUser"))
    }

    fun testIndexesOperationWithUnderscores() {
        createScenarioFile("test3", """
            scenario: test
              given: setup
                call ^get_user_by_id
        """.trimIndent())

        val operations = OperationUsageIndex.getAllOperationIds(project)
        assertTrue(
            "Should index 'get_user_by_id'",
            operations.contains("get_user_by_id")
        )
    }

    fun testIndexesOperationStartingWithUnderscore() {
        createScenarioFile("test4", """
            scenario: test
              given: setup
                call ^_privateOp
        """.trimIndent())

        val operations = OperationUsageIndex.getAllOperationIds(project)
        assertTrue(
            "Should index '_privateOp'",
            operations.contains("_privateOp")
        )
    }

    fun testIndexesOperationWithNumbers() {
        createScenarioFile("test5", """
            scenario: test
              given: setup
                call ^User123
        """.trimIndent())

        val operations = OperationUsageIndex.getAllOperationIds(project)
        assertTrue(
            "Should index 'User123'",
            operations.contains("User123")
        )
    }

    fun testDoesNotIndexCaretWithoutValidId() {
        createScenarioFile("test6", """
            scenario: test
              given: this line has ^ by itself
        """.trimIndent())

        val operations = OperationUsageIndex.getAllOperationIds(project)
        // Should not index 'in' which comes after the caret
        // The pattern requires the caret to be followed by a valid identifier start
        assertFalse(
            "Should not index text after isolated caret",
            operations.contains("by")
        )
    }

    fun testDoesNotIndexOperationStartingWithNumber() {
        createScenarioFile("test7", """
            scenario: test
              given: setup
                call ^123invalid
        """.trimIndent())

        val operations = OperationUsageIndex.getAllOperationIds(project)
        assertFalse(
            "Should not index '123invalid' (starts with number)",
            operations.contains("123invalid")
        )
    }

    fun testIndexesOperationAtEndOfLine() {
        createScenarioFile("test8", """
            scenario: test
              given: setup
                call ^getUser
        """.trimIndent())

        val operations = OperationUsageIndex.getAllOperationIds(project)
        assertTrue(
            "Should index 'getUser'",
            operations.contains("getUser")
        )
    }

    fun testIndexesOperationInStepText() {
        createScenarioFile("test9", """
            scenario: test
              given: ^createUser is called
        """.trimIndent())

        val operations = OperationUsageIndex.getAllOperationIds(project)
        assertTrue(
            "Should index 'createUser' in step text",
            operations.contains("createUser")
        )
    }

    // ========== File Lookup Tests ==========

    fun testGetFilesReferencingOperation() {
        createScenarioFile("referencer", """
            scenario: test
              given: setup
                call ^targetOp
        """.trimIndent())

        createScenarioFile("other", """
            scenario: other
              given: setup
                call GET /api
        """.trimIndent())

        val files = OperationUsageIndex.getFilesReferencingOperation(project, "targetOp")
        assertEquals(
            "Should find one file referencing 'targetOp'",
            1,
            files.size
        )
        assertTrue(
            "Should be the referencer file",
            files.first().name == "referencer.scenario"
        )
    }

    // ========== Include Directive Tests ==========

    fun testIndexesOperationInIncludeDirective() {
        createScenarioFile("test10", """
            scenario: test
              given: setup
                include ^fragmentOp
        """.trimIndent())

        val operations = OperationUsageIndex.getAllOperationIds(project)
        assertTrue(
            "Should index 'fragmentOp' from include directive",
            operations.contains("fragmentOp")
        )
    }
}

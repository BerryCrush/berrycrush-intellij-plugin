package org.berrycrush.intellij.index

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.berrycrush.intellij.BerryCrushTestCase
import org.junit.jupiter.api.Test

/**
 * Unit tests for OperationUsageIndex.
 *
 * Tests the actual index class behavior using the IntelliJ testing framework.
 */
class OperationUsageIndexTest : BerryCrushTestCase() {
    // ========== Index Detection Tests ==========

    @Test
    fun testIndexesBasicOperationReference() {
        createScenarioFile(
            "test",
            """
            scenario: test
              given: setup
                call ^createUser
            """.trimIndent(),
        )

        val operations = getAllOperationIds(project)
        assertTrue(
            "Should index 'createUser'",
            operations.contains("createUser"),
        )
    }

    @Test
    fun testIndexesMultipleOperationReferences() {
        createScenarioFile(
            "test2",
            """
            scenario: test
              given: setup
                call ^createUser
              when: action
                call ^updateUser
              then: verify
                call ^deleteUser
            """.trimIndent(),
        )

        val operations = getAllOperationIds(project)
        assertTrue("Should index 'createUser'", operations.contains("createUser"))
        assertTrue("Should index 'updateUser'", operations.contains("updateUser"))
        assertTrue("Should index 'deleteUser'", operations.contains("deleteUser"))
    }

    @Test
    fun testIndexesOperationWithUnderscores() {
        createScenarioFile(
            "test3",
            """
            scenario: test
              given: setup
                call ^get_user_by_id
            """.trimIndent(),
        )

        val operations = getAllOperationIds(project)
        assertTrue(
            "Should index 'get_user_by_id'",
            operations.contains("get_user_by_id"),
        )
    }

    @Test
    fun testIndexesOperationStartingWithUnderscore() {
        createScenarioFile(
            "test4",
            """
            scenario: test
              given: setup
                call ^_privateOp
            """.trimIndent(),
        )

        val operations = getAllOperationIds(project)
        assertTrue(
            "Should index '_privateOp'",
            operations.contains("_privateOp"),
        )
    }

    @Test
    fun testIndexesOperationWithNumbers() {
        createScenarioFile(
            "test5",
            """
            scenario: test
              given: setup
                call ^User123
            """.trimIndent(),
        )

        val operations = getAllOperationIds(project)
        assertTrue(
            "Should index 'User123'",
            operations.contains("User123"),
        )
    }

    @Test
    fun testDoesNotIndexCaretWithoutValidId() {
        createScenarioFile(
            "test6",
            """
            scenario: test
              given: this line has ^ by itself
            """.trimIndent(),
        )

        val operations = getAllOperationIds(project)
        // Should not index 'in' which comes after the caret
        // The pattern requires the caret to be followed by a valid identifier start
        assertFalse(
            "Should not index text after isolated caret",
            operations.contains("by"),
        )
    }

    @Test
    fun testIndexesOperationAtEndOfLine() {
        createScenarioFile(
            "test8",
            """
            scenario: test
              given: setup
                call ^getUser
            """.trimIndent(),
        )

        val operations = getAllOperationIds(project)
        assertTrue(
            "Should index 'getUser'",
            operations.contains("getUser"),
        )
    }

    @Test
    fun testIndexesOperationInStepText() {
        createScenarioFile(
            "test9",
            """
            scenario: test
              given: ^createUser is called
            """.trimIndent(),
        )

        val operations = getAllOperationIds(project)
        assertTrue(
            "Should index 'createUser' in step text",
            operations.contains("createUser"),
        )
    }

    // ========== File Lookup Tests ==========

    @Test
    fun testGetFilesReferencingOperation() {
        createScenarioFile(
            "referencer",
            """
            scenario: test
              given: setup
                call ^targetOp
            """.trimIndent(),
        )

        createScenarioFile(
            "other",
            """
            scenario: other
              given: setup
                call GET /api
            """.trimIndent(),
        )

        val files = getFilesReferencingOperation(project, "targetOp")
        assertEquals(
            "Should find one file referencing 'targetOp'",
            1,
            files.size,
        )
        assertTrue(
            "Should be the referencer file",
            files.first().name == "referencer.scenario",
        )
    }

    // ========== Include Directive Tests ==========

    @Test
    fun testIndexesOperationInIncludeDirective() {
        createScenarioFile(
            "test10",
            """
            scenario: test
              given: setup
                call ^callOp
            """.trimIndent(),
        )

        val operations = getAllOperationIds(project)
        assertTrue(
            "Should index 'callOp' from include directive",
            operations.contains("callOp"),
        )
    }

    private fun getAllOperationIds(project: Project) = ReadAction.compute<Collection<String>, Throwable> {
        OperationUsageIndex.getAllOperationIds(project)
    }

    private fun getFilesReferencingOperation(project: Project, operationId: String) = ReadAction.compute<Collection<VirtualFile>, Throwable> {
        OperationUsageIndex.getFilesReferencingOperation(project, operationId)
    }
}

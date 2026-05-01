package com.berrycrush.intellij.index

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Unit tests for OperationUsageIndex.
 *
 * Tests the operation reference (^operationId) regex pattern matching.
 */
class OperationUsageIndexTest {

    // Test the regex pattern directly
    private val operationPattern = Regex("""\^([a-zA-Z_]\w*)""")

    @Test
    fun `pattern matches basic operation reference`() {
        val content = "Given ^createUser is called"
        val matches = operationPattern.findAll(content).toList()
        assertEquals(1, matches.size)
        assertEquals("createUser", matches[0].groupValues[1])
    }

    @Test
    fun `pattern matches multiple operation references`() {
        val content = """
            Given ^createUser is called
            When ^updateUser is called
            Then ^deleteUser is called
        """.trimIndent()
        val matches = operationPattern.findAll(content).toList()
        assertEquals(3, matches.size)
        assertEquals("createUser", matches[0].groupValues[1])
        assertEquals("updateUser", matches[1].groupValues[1])
        assertEquals("deleteUser", matches[2].groupValues[1])
    }

    @Test
    fun `pattern matches operation with underscores`() {
        val content = "Given ^get_user_by_id is called"
        val matches = operationPattern.findAll(content).toList()
        assertEquals("get_user_by_id", matches[0].groupValues[1])
    }

    @Test
    fun `pattern matches operation starting with letter`() {
        val content = "Given ^getUserById is called"
        val matches = operationPattern.findAll(content).toList()
        assertEquals("getUserById", matches[0].groupValues[1])
    }

    @Test
    fun `pattern matches operation starting with underscore`() {
        val content = "Given ^_privateOp is called"
        val matches = operationPattern.findAll(content).toList()
        assertEquals("_privateOp", matches[0].groupValues[1])
    }

    @Test
    fun `pattern matches operation with numbers`() {
        val content = "Given ^User123 is called"
        val matches = operationPattern.findAll(content).toList()
        assertEquals("User123", matches[0].groupValues[1])
    }

    @Test
    fun `pattern does not match caret without valid id`() {
        val content = "This line has ^ in middle"
        val matches = operationPattern.findAll(content).toList()
        assertEquals(0, matches.size)
    }

    @Test
    fun `pattern does not match operation starting with number`() {
        val content = "Given ^123invalid is called"
        val matches = operationPattern.findAll(content).toList()
        // Should not match because operation must start with letter or underscore
        assertEquals(0, matches.size)
    }

    @Test
    fun `pattern matches operation at end of line`() {
        val content = "Call ^getUser"
        val matches = operationPattern.findAll(content).toList()
        assertEquals("getUser", matches[0].groupValues[1])
    }

    @Test
    fun `pattern stops at non-word character`() {
        val content = "Given ^createUser, and ^deleteUser"
        val matches = operationPattern.findAll(content).toList()
        assertEquals(2, matches.size)
        assertEquals("createUser", matches[0].groupValues[1])
        assertEquals("deleteUser", matches[1].groupValues[1])
    }
}

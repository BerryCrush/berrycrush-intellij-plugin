package com.berrycrush.intellij.listener

import com.berrycrush.intellij.BerryCrushTestCase
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import java.lang.reflect.Method

/**
 * Tests for BerryCrushFileChangeListener.
 *
 * Tests the file filtering logic to ensure the listener responds
 * to the correct file types.
 */
class BerryCrushFileChangeListenerTest : BerryCrushTestCase() {

    private lateinit var listener: BerryCrushFileChangeListener
    private lateinit var isRelevantFileChangeMethod: Method

    override fun setUp() {
        super.setUp()
        listener = BerryCrushFileChangeListener()

        // Access private methods via reflection for testing
        isRelevantFileChangeMethod = listener.javaClass.getDeclaredMethod(
            "isRelevantFileChange",
            VFileEvent::class.java
        ).apply { isAccessible = true }
    }

    fun testFragmentFilesAreRelevant() {
        val file = createFragmentFile("test", "fragment: test-fragment")
        val event = createContentChangeEvent(file)

        assertTrue(
            "Fragment files should be relevant",
            isRelevantFileChange(event)
        )
    }

    fun testScenarioFilesAreRelevant() {
        val file = createScenarioFile("test", "scenario: test")
        val event = createContentChangeEvent(file)

        assertTrue(
            "Scenario files should be relevant for refresh",
            isRelevantFileChange(event)
        )
    }

    fun testJavaFilesAreRelevant() {
        val file = myFixture.addFileToProject("Steps.java", "class Steps {}")
        val event = createContentChangeEvent(file.virtualFile)

        assertTrue(
            "Java files should be relevant",
            isRelevantFileChange(event)
        )
    }

    fun testKotlinFilesAreRelevant() {
        val file = myFixture.addFileToProject("Steps.kt", "class Steps")
        val event = createContentChangeEvent(file.virtualFile)

        assertTrue(
            "Kotlin files should be relevant",
            isRelevantFileChange(event)
        )
    }

    fun testOpenApiYamlFilesAreRelevant() {
        // Create a file with OpenAPI-like filename (triggers filename heuristic)
        val file = myFixture.addFileToProject("openapi.yaml", """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            paths: {}
        """.trimIndent())
        val event = createContentChangeEvent(file.virtualFile)

        assertTrue(
            "OpenAPI YAML files should be relevant",
            isRelevantFileChange(event)
        )
    }

    fun testSwaggerJsonFilesAreRelevant() {
        // Create a file with Swagger-like filename (triggers filename heuristic)
        val file = myFixture.addFileToProject("swagger.json", """
            {
              "swagger": "2.0",
              "info": { "title": "Test", "version": "1.0" },
              "paths": {}
            }
        """.trimIndent())
        val event = createContentChangeEvent(file.virtualFile)

        assertTrue(
            "Swagger JSON files should be relevant",
            isRelevantFileChange(event)
        )
    }

    fun testRegularYamlFilesAreNotRelevant() {
        val file = myFixture.addFileToProject("config.yaml", "key: value")
        val event = createContentChangeEvent(file.virtualFile)

        assertFalse(
            "Regular YAML files should not be relevant",
            isRelevantFileChange(event)
        )
    }

    fun testTextFilesAreNotRelevant() {
        val file = myFixture.addFileToProject("readme.txt", "text content")
        val event = createContentChangeEvent(file.virtualFile)

        assertFalse(
            "Text files should not be relevant",
            isRelevantFileChange(event)
        )
    }

    fun testPrepareChangeReturnsNullForNoRelevantEvents() {
        val file = myFixture.addFileToProject("readme.txt", "text")
        val event = createContentChangeEvent(file.virtualFile)
        val events = mutableListOf(event)

        val result = listener.prepareChange(events)
        assertNull("Should return null for non-relevant events", result)
    }

    fun testPrepareChangeReturnsApplierForRelevantEvents() {
        val file = createFragmentFile("test", "fragment: test")
        val event = createContentChangeEvent(file)
        val events = mutableListOf(event)

        val result = listener.prepareChange(events)
        assertNotNull("Should return applier for relevant events", result)
    }

    // Helper methods

    private fun createContentChangeEvent(file: com.intellij.openapi.vfs.VirtualFile): VFileEvent {
        @Suppress("DEPRECATION")
        return VFileContentChangeEvent(
            this,          // requestor
            file,          // file
            0L,            // oldModificationStamp
            1L,            // newModificationStamp
            false          // isFromRefresh
        )
    }

    private fun isRelevantFileChange(event: VFileEvent): Boolean {
        return isRelevantFileChangeMethod.invoke(listener, event) as Boolean
    }
}

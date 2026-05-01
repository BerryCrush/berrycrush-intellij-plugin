package com.berrycrush.intellij.reference

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Unit tests for BerryCrushOperationReference OpenAPI parsing logic.
 */
class BerryCrushOperationReferenceTest {

    // Replicate the operation ID extraction logic for testing
    private fun extractOperationIds(text: String): List<String> {
        val result = mutableListOf<String>()

        // YAML format: operationId: someId
        Regex("""operationId:\s*['"]?(\w+)['"]?""").findAll(text).forEach {
            result.add(it.groupValues[1])
        }

        // JSON format: "operationId": "someId"
        Regex(""""operationId"\s*:\s*"(\w+)"""").findAll(text).forEach {
            result.add(it.groupValues[1])
        }

        return result.distinct()
    }

    @Test
    fun `detects OpenAPI 3 YAML spec`() {
        val spec = """
            openapi: 3.0.0
            info:
              title: Test API
              version: 1.0.0
            paths:
              /users:
                get:
                  operationId: listUsers
        """.trimIndent()
        assertTrue(BerryCrushOperationReference.isOpenAPISpec(spec))
    }

    @Test
    fun `detects OpenAPI 3 with quoted version`() {
        val spec = """
            openapi: '3.0.1'
            info:
              title: Test API
              version: 1.0.0
            paths: {}
            extra: content to make it longer than 100 chars
        """.trimIndent()
        assertTrue(BerryCrushOperationReference.isOpenAPISpec(spec))
    }

    @Test
    fun `detects Swagger 2 YAML spec`() {
        val spec = """
            swagger: 2.0
            info:
              title: Test API
              version: 1.0.0
            paths:
              /users:
                get:
                  operationId: listUsers
        """.trimIndent()
        assertTrue(BerryCrushOperationReference.isOpenAPISpec(spec))
    }

    @Test
    fun `detects OpenAPI 3 JSON spec`() {
        val spec = """
            {
              "openapi": "3.0.0",
              "info": {
                "title": "Test API",
                "version": "1.0.0",
                "description": "This is a test API for testing purposes"
              }
            }
        """.trimIndent()
        assertTrue(BerryCrushOperationReference.isOpenAPISpec(spec))
    }

    @Test
    fun `detects Swagger 2 JSON spec`() {
        val spec = """
            {
              "swagger": "2.0",
              "info": {
                "title": "Test API",
                "version": "1.0.0",
                "description": "This is a test API for testing purposes"
              }
            }
        """.trimIndent()
        assertTrue(BerryCrushOperationReference.isOpenAPISpec(spec))
    }

    @Test
    fun `rejects non-OpenAPI content`() {
        val content = """
            name: My Config
            version: 1.0.0
            settings:
              - key: value
        """.trimIndent()
        assertFalse(BerryCrushOperationReference.isOpenAPISpec(content))
    }

    @Test
    fun `rejects short content`() {
        val content = "openapi: 3.0.0"
        assertFalse(BerryCrushOperationReference.isOpenAPISpec(content))
    }

    @Test
    fun `extracts operation IDs from YAML`() {
        val spec = """
            openapi: 3.0.0
            paths:
              /users:
                get:
                  operationId: listUsers
                post:
                  operationId: createUser
              /users/{id}:
                get:
                  operationId: getUser
                delete:
                  operationId: deleteUser
        """.trimIndent()

        val ids = extractOperationIds(spec)
        assertEquals(4, ids.size)
        assertTrue(ids.contains("listUsers"))
        assertTrue(ids.contains("createUser"))
        assertTrue(ids.contains("getUser"))
        assertTrue(ids.contains("deleteUser"))
    }

    @Test
    fun `extracts operation IDs from JSON`() {
        val spec = """
            {
              "openapi": "3.0.0",
              "paths": {
                "/users": {
                  "get": { "operationId": "listUsers" },
                  "post": { "operationId": "createUser" }
                }
              }
            }
        """.trimIndent()

        val ids = extractOperationIds(spec)
        assertEquals(2, ids.size)
        assertTrue(ids.contains("listUsers"))
        assertTrue(ids.contains("createUser"))
    }

    @Test
    fun `handles quoted YAML operation IDs`() {
        val spec = """
            openapi: 3.0.0
            paths:
              /users:
                get:
                  operationId: 'listUsers'
                post:
                  operationId: "createUser"
        """.trimIndent()

        val ids = extractOperationIds(spec)
        assertTrue(ids.contains("listUsers"))
        assertTrue(ids.contains("createUser"))
    }

    @Test
    fun `handles spec without operations`() {
        val spec = """
            openapi: 3.0.0
            info:
              title: Empty API
              version: 1.0.0
            paths: {}
        """.trimIndent()

        val ids = extractOperationIds(spec)
        assertEquals(0, ids.size)
    }

    @Test
    fun `deduplicates operation IDs`() {
        val spec = """
            operationId: duplicate
            operationId: duplicate
            operationId: unique
        """.trimIndent()

        val ids = extractOperationIds(spec)
        assertEquals(2, ids.size)
        assertTrue(ids.contains("duplicate"))
        assertTrue(ids.contains("unique"))
    }
}

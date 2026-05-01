package com.berrycrush.intellij.inspection

import com.berrycrush.intellij.BerryCrushTestCase
import com.berrycrush.intellij.reference.BerryCrushOperationReference

/**
 * Tests for UndefinedOperationInspection.
 *
 * Tests the operation detection logic.
 */
class UndefinedOperationInspectionTest : BerryCrushTestCase() {

    fun testCallPatternMatching() {
        // Test the regex pattern matching
        val pattern = Regex("""call\s+\^(\w+)""")
        
        // Should match
        var match = pattern.find("call ^listPets")
        assertNotNull(match)
        assertEquals("listPets", match?.groupValues?.get(1))
        
        match = pattern.find("    call ^createPet")
        assertNotNull(match)
        assertEquals("createPet", match?.groupValues?.get(1))
        
        // Should not match (no caret)
        match = pattern.find("call listPets")
        assertNull(match)
        
        // Should not match (in prose)
        match = pattern.find("user should call the service")
        assertNull(match)
    }

    fun testOperationLookupWithOpenAPIFile() {
        // Create an OpenAPI spec
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

        // Verify operation is found
        val operations = BerryCrushOperationReference.findAllOperationIds(project)
        assertTrue(
            "listPets should be found in OpenAPI spec",
            operations.contains("listPets")
        )
    }

    fun testUndefinedOperationNotInOpenAPI() {
        // Create an OpenAPI spec with only listPets
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

        val operations = BerryCrushOperationReference.findAllOperationIds(project)
        assertFalse(
            "unknownOp should not be in OpenAPI spec",
            operations.contains("unknownOp")
        )
    }

    fun testNoOpenAPIFilesReturnsEmptyOperations() {
        // Without any OpenAPI files, no operations should be found
        val operations = BerryCrushOperationReference.findAllOperationIds(project)
        assertTrue(
            "Without OpenAPI files, no operations should be found",
            operations.isEmpty()
        )
    }
}


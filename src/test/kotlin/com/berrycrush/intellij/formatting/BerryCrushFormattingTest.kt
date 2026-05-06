package com.berrycrush.intellij.formatting

import com.berrycrush.intellij.BerryCrushTestCase
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.codeStyle.CodeStyleManager

/**
 * Tests for BerryCrush code formatting.
 * 
 * Each test verifies that the formatter produces deterministic output
 * by comparing actual results against expected results.
 */
class BerryCrushFormattingTest : BerryCrushTestCase() {

    /**
     * Helper function to test formatting.
     * Configures file with input, runs reformat, and checks result.
     */
    private fun doFormattingTest(input: String, expected: String, fileExtension: String = "scenario") {
        myFixture.configureByText("test.$fileExtension", input)
        
        WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project)
                .reformatText(myFixture.file, 0, myFixture.editor.document.textLength)
        }
        
        val actual = myFixture.editor.document.text
        
        // Debug output for test failures
        if (actual != expected) {
            println("=== INPUT ===")
            println(input.replace(" ", "·"))
            println("=== EXPECTED ===")
            println(expected.replace(" ", "·"))
            println("=== ACTUAL ===")
            println(actual.replace(" ", "·"))
            println("=== END ===")
        }
        
        assertEquals(
            "Formatting result mismatch",
            expected,
            actual
        )
    }

    // === Root Level Elements ===

    fun testScenarioAtRootLevel() {
        doFormattingTest(
            "scenario: Test Scenario",
            "scenario: Test Scenario"
        )
    }

    fun testFeatureAtRootLevel() {
        doFormattingTest(
            "feature: Test Feature",
            "feature: Test Feature"
        )
    }

    fun testFragmentAtRootLevel() {
        doFormattingTest(
            "fragment: test-fragment",
            "fragment: test-fragment",
            "fragment"
        )
    }

    // === Step Indentation ===
    
    fun testStepIndentationInScenario() {
        val input = """
            scenario: Test
            given step one
            when step two
            then step three
        """.trimIndent()
        
        val expected = """
            scenario: Test
              given step one
              when step two
              then step three
        """.trimIndent()
        
        doFormattingTest(input, expected)
    }
    
    fun testStepIndentationInFragment() {
        val input = """
            fragment: test-fragment
            given step one
            when step two
        """.trimIndent()
        
        val expected = """
            fragment: test-fragment
              given step one
              when step two
        """.trimIndent()
        
        doFormattingTest(input, expected, "fragment")
    }

    // === Directive Indentation ===
    
    fun testDirectiveIndentationInScenario() {
        val input = """
            scenario: Test
            when making a call
            call ^operation
            assert status 200
        """.trimIndent()
        
        val expected = """
            scenario: Test
              when making a call
                call ^operation
                assert status 200
        """.trimIndent()
        
        doFormattingTest(input, expected)
    }
    
    fun testParameterIndentation() {
        val input = """
            scenario: Test
            when making a call
            call ^operation
            petId: 123
        """.trimIndent()
        
        val expected = """
            scenario: Test
              when making a call
                call ^operation
                  petId: 123
        """.trimIndent()
        
        doFormattingTest(input, expected)
    }

    // === Table Alignment ===
    
    fun testTableColumnAlignment() {
        val input = """
            examples:
            | name | value |
            | foo| 1 |
        """.trimIndent()
        
        val expected = """
            examples:
              | name | value |
              | foo  | 1     |
        """.trimIndent()
        
        doFormattingTest(input, expected)
    }
    
    fun testTableAlignmentWithUnevenColumns() {
        val input = """
            examples:
            |petId|value|
            |1|fluffy|
            |123|short|
        """.trimIndent()
        
        val expected = """
            examples:
              | petId | value  |
              | 1     | fluffy |
              | 123   | short  |
        """.trimIndent()
        
        doFormattingTest(input, expected)
    }

    // === Complex Scenarios ===
    
    fun testComplexScenarioFormatting() {
        val input = """
            scenario: test file for copilot
                    include  verify_pet_by_id
                   petId: 1

            outline: foo
                when  I do something
                    call  ^getPetById
             petId: {{petId}}
              examples:
             | petId | value  |
             | 1     | fluffy |
        """.trimIndent()
        
        val expected = """
            scenario: test file for copilot
              include verify_pet_by_id
                petId: 1

            outline: foo
              when I do something
                call ^getPetById
                  petId: {{petId}}
              examples:
                | petId | value  |
                | 1     | fluffy |
        """.trimIndent()
        
        doFormattingTest(input, expected)
    }
    
    // === Feature with Nested Elements ===
    
    fun testFeatureWithScenario() {
        val input = """
            feature: Pet Store API
            scenario: List pets
            when listing pets
            call ^listPets
            then verify response
            assert status 200
        """.trimIndent()
        
        val expected = """
            feature: Pet Store API
              scenario: List pets
                when listing pets
                  call ^listPets
                then verify response
                  assert status 200
        """.trimIndent()
        
        doFormattingTest(input, expected)
    }
    
    fun testFeatureWithBackground() {
        val input = """
            feature: Pet Store API
            background:
            given authenticated user
            include auth
        """.trimIndent()
        
        val expected = """
            feature: Pet Store API
              background:
                given authenticated user
                  include auth
        """.trimIndent()
        
        doFormattingTest(input, expected)
    }

    // === Spacing Normalization ===
    
    fun testMultipleSpacesNormalized() {
        val input = "scenario:   Test    with    spaces"
        val expected = "scenario: Test with spaces"
        
        doFormattingTest(input, expected)
    }
}

package com.berrycrush.intellij.psi

import com.berrycrush.intellij.BerryCrushTestCase
import com.intellij.psi.util.PsiTreeUtil

/**
 * Tests for BerryCrush PSI element classes.
 * Verifies name extraction and property access for all element types.
 */
class BerryCrushElementsTest : BerryCrushTestCase() {

    // ========== BerryCrushScenarioElement Tests ==========

    fun testScenarioNameExtraction() {
        val file = createScenarioFile("test", """
            scenario: My Test Scenario
            given a precondition
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        val scenario = PsiTreeUtil.findChildOfType(psiFile, BerryCrushScenarioElement::class.java)
        assertNotNull("Scenario element should exist", scenario)
        assertEquals("My Test Scenario", scenario?.scenarioName)
        assertEquals("My Test Scenario", scenario?.name)
    }

    fun testScenarioNameWithLeadingWhitespace() {
        val file = createScenarioFile("whitespace", """
            scenario:   Spaced Name   
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val scenario = PsiTreeUtil.findChildOfType(psiFile, BerryCrushScenarioElement::class.java)
        assertEquals("Spaced Name", scenario?.scenarioName)
    }

    fun testScenarioNameLowercase() {
        val file = createScenarioFile("lower", """
            scenario: lowercase scenario name
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val scenario = PsiTreeUtil.findChildOfType(psiFile, BerryCrushScenarioElement::class.java)
        assertNotNull("Scenario with lowercase 'scenario' should be found", scenario)
        assertEquals("lowercase scenario name", scenario?.scenarioName)
    }

    // ========== BerryCrushFragmentElement Tests ==========

    fun testFragmentNameExtraction() {
        val file = createFragmentFile("test", """
            fragment: my-fragment-name
            given a step
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val fragment = PsiTreeUtil.findChildOfType(psiFile, BerryCrushFragmentElement::class.java)
        assertNotNull("Fragment element should exist", fragment)
        assertEquals("my-fragment-name", fragment?.fragmentName)
        assertEquals("my-fragment-name", fragment?.name)
    }

    fun testFragmentNameLowercase() {
        val file = createFragmentFile("lower", """
            fragment: lowercase-fragment
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val fragment = PsiTreeUtil.findChildOfType(psiFile, BerryCrushFragmentElement::class.java)
        assertNotNull("Fragment with lowercase 'fragment' should be found", fragment)
        assertEquals("lowercase-fragment", fragment?.fragmentName)
    }

    fun testFragmentNameWithDots() {
        val file = createFragmentFile("dots", """
            fragment: com.example.my-fragment
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val fragment = PsiTreeUtil.findChildOfType(psiFile, BerryCrushFragmentElement::class.java)
        assertEquals("com.example.my-fragment", fragment?.fragmentName)
    }

    // ========== BerryCrushFeatureElement Tests ==========

    fun testFeatureNameExtraction() {
        val file = createScenarioFile("feature", """
            feature: User Authentication
            scenario: Login
            given user exists
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val feature = PsiTreeUtil.findChildOfType(psiFile, BerryCrushFeatureElement::class.java)
        assertNotNull("Feature element should exist", feature)
        assertEquals("User Authentication", feature?.featureName)
        assertEquals("User Authentication", feature?.name)
    }

    fun testFeatureNameLowercase() {
        val file = createScenarioFile("lowerFeature", """
            feature: lowercase feature
            scenario: test
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val feature = PsiTreeUtil.findChildOfType(psiFile, BerryCrushFeatureElement::class.java)
        assertNotNull("Feature with lowercase 'feature' should be found", feature)
        assertEquals("lowercase feature", feature?.featureName)
    }

    // ========== BerryCrushStepElement Tests ==========

    fun testStepKeywordGiven() {
        val file = createFragmentFile("given", """
            fragment: test
            given a precondition is met
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val step = PsiTreeUtil.findChildOfType(psiFile, BerryCrushStepElement::class.java)
        assertNotNull("Step element should exist", step)
        assertEquals("given", step?.keyword)
        assertEquals("a precondition is met", step?.stepText)
    }

    fun testStepKeywordWhen() {
        val file = createFragmentFile("when", """
            fragment: test
            when user performs action
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val step = PsiTreeUtil.findChildOfType(psiFile, BerryCrushStepElement::class.java)
        assertEquals("when", step?.keyword)
        assertEquals("user performs action", step?.stepText)
    }

    fun testStepKeywordThen() {
        val file = createFragmentFile("then", """
            fragment: test
            then result is verified
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val step = PsiTreeUtil.findChildOfType(psiFile, BerryCrushStepElement::class.java)
        assertEquals("then", step?.keyword)
        assertEquals("result is verified", step?.stepText)
    }

    fun testStepKeywordAnd() {
        val file = createFragmentFile("and", """
            fragment: test
            and another condition
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val step = PsiTreeUtil.findChildOfType(psiFile, BerryCrushStepElement::class.java)
        assertEquals("and", step?.keyword)
        assertEquals("another condition", step?.stepText)
    }

    fun testStepKeywordBut() {
        val file = createFragmentFile("but", """
            fragment: test
            but not this condition
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val step = PsiTreeUtil.findChildOfType(psiFile, BerryCrushStepElement::class.java)
        assertEquals("but", step?.keyword)
        assertEquals("not this condition", step?.stepText)
    }

    fun testStepKeywordsCaseInsensitive() {
        // Uppercase keywords should NOT be recognized (strict lowercase per BerryCrush DSL spec)
        val file = createFragmentFile("case", """
            fragment: test
            GIVEN uppercase step
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val step = PsiTreeUtil.findChildOfType(psiFile, BerryCrushStepElement::class.java)
        // Step should not be found because GIVEN is not a valid keyword
        assertNull("Uppercase GIVEN should not be recognized as a step", step)
    }

    // ========== BerryCrushIncludeElement Tests ==========

    fun testIncludeFragmentName() {
        val file = createScenarioFile("include", """
            scenario: Test
            include my-fragment
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val include = PsiTreeUtil.findChildOfType(psiFile, BerryCrushIncludeElement::class.java)
        assertNotNull("Include element should exist", include)
        assertEquals("my-fragment", include?.fragmentName)
        assertEquals("my-fragment", include?.name)
    }

    fun testIncludeFragmentNameWithCaret() {
        val file = createScenarioFile("includeCaret", """
            scenario: Test
            include ^my-fragment
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val include = PsiTreeUtil.findChildOfType(psiFile, BerryCrushIncludeElement::class.java)
        assertEquals("my-fragment", include?.fragmentName)
    }

    fun testIncludeFragmentNameWithDots() {
        val file = createScenarioFile("includeDots", """
            scenario: Test
            include com.example.fragment
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val include = PsiTreeUtil.findChildOfType(psiFile, BerryCrushIncludeElement::class.java)
        assertEquals("com.example.fragment", include?.fragmentName)
    }

    // ========== BerryCrushCallElement Tests ==========

    fun testCallOperationId() {
        val file = createScenarioFile("call", """
            scenario: Test
            call ^getPet
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val call = PsiTreeUtil.findChildOfType(psiFile, BerryCrushCallElement::class.java)
        assertNotNull("Call element should exist", call)
        assertEquals("getPet", call?.operationId)
    }

    fun testCallOperationRefElement() {
        val file = createScenarioFile("callRef", """
            scenario: Test
            call ^createOrder
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val call = PsiTreeUtil.findChildOfType(psiFile, BerryCrushCallElement::class.java)
        val opRef = call?.operationRef
        assertNotNull("Operation ref should exist", opRef)
        assertEquals("createOrder", opRef?.operationId)
        assertEquals("createOrder", opRef?.name)
    }

    // ========== BerryCrushOperationRefElement Tests ==========

    fun testOperationRefRemovesCaret() {
        val file = createScenarioFile("opRef", """
            scenario: Test
            call ^myOperation
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val opRef = PsiTreeUtil.findChildOfType(psiFile, BerryCrushOperationRefElement::class.java)
        assertNotNull("Operation ref element should exist", opRef)
        assertEquals("myOperation", opRef?.operationId)
        assertEquals("myOperation", opRef?.name)
    }

    fun testOperationRefHasReference() {
        val file = createScenarioFile("opRefRef", """
            scenario: Test
            call ^targetOperation
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val opRef = PsiTreeUtil.findChildOfType(psiFile, BerryCrushOperationRefElement::class.java)
        val reference = opRef?.reference
        assertNotNull("Operation ref should have reference", reference)
    }

    // ========== BerryCrushAssertElement Tests ==========

    fun testAssertText() {
        val file = createScenarioFile("assert", """
            scenario: Test
            assert response.status == 200
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val assert = PsiTreeUtil.findChildOfType(psiFile, BerryCrushAssertElement::class.java)
        assertNotNull("Assert element should exist", assert)
        assertEquals("response.status == 200", assert?.assertionText)
    }

    fun testAssertTextWithMultipleSpaces() {
        val file = createScenarioFile("assertSpaces", """
            scenario: Test
            assert   response.body.name == "test"
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val assert = PsiTreeUtil.findChildOfType(psiFile, BerryCrushAssertElement::class.java)
        assertEquals("response.body.name == \"test\"", assert?.assertionText)
    }

    // ========== BerryCrushFragmentRefElement Tests ==========

    fun testFragmentRefRemovesCaret() {
        val file = createScenarioFile("fragRef", """
            scenario: Test
            include ^my-fragment
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val fragRef = PsiTreeUtil.findChildOfType(psiFile, BerryCrushFragmentRefElement::class.java)
        assertNotNull("Fragment ref element should exist", fragRef)
        assertEquals("my-fragment", fragRef?.name)
    }

    fun testFragmentRefHasReference() {
        val file = createScenarioFile("fragRefRef", """
            scenario: Test
            include ^target-fragment
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        val fragRef = PsiTreeUtil.findChildOfType(psiFile, BerryCrushFragmentRefElement::class.java)
        val reference = fragRef?.reference
        assertNotNull("Fragment ref should have reference", reference)
    }
}

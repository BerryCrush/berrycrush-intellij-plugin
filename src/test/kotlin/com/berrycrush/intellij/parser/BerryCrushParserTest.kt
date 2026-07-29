package com.berrycrush.intellij.parser

import com.berrycrush.intellij.BerryCrushTestCase
import com.berrycrush.intellij.psi.BerryCrushFragmentElement
import com.berrycrush.intellij.psi.BerryCrushFeatureElement
import com.berrycrush.intellij.psi.BerryCrushCallElement
import com.berrycrush.intellij.psi.BerryCrushAssertElement
import com.berrycrush.intellij.psi.BerryCrushParameterEntryElement
import com.berrycrush.intellij.psi.BerryCrushParametersBlockElement
import com.berrycrush.intellij.psi.BerryCrushScenarioElement
import com.berrycrush.intellij.psi.BerryCrushStepElement
import com.intellij.psi.util.PsiTreeUtil

/**
 * Tests for BerryCrush parser - verifies PSI tree structure.
 */
class BerryCrushParserTest : BerryCrushTestCase() {

        fun testProvidedSamplePsiHierarchy() {
                val file = createScenarioFile("hierarchy", """
                        feature: feature description
                            background: background description
                                given background given description
                                    call ^operationId
                                        id: {{petId}}
                                        body:
                                            name: foo
                                then check the value
                                    assert status 2xx
                            scenario: nested scenario
                                given nested step

                        scenario: standalone scenario
                            given standalone step
                """.trimIndent())

                val psiFile = psiManager.findFile(file)
                assertNotNull("PSI file should be created", psiFile)

                val feature = PsiTreeUtil.findChildOfType(psiFile, BerryCrushFeatureElement::class.java)
                assertNotNull("Feature element should exist", feature)
                assertEquals("feature description", feature?.description)

                val background = feature?.backgrounds?.singleOrNull()
                assertNotNull("Feature should contain one background child", background)
                assertEquals("background description", background?.description)

                val nestedScenario = feature?.scenarios?.singleOrNull()
                assertNotNull("Feature should contain one nested scenario child", nestedScenario)
                assertEquals("nested scenario", nestedScenario?.description)

                val standaloneScenario = PsiTreeUtil.findChildrenOfType(psiFile, BerryCrushScenarioElement::class.java)
                        .firstOrNull { it.keyword == "scenario" && it.description == "standalone scenario" }
                assertNotNull("Standalone scenario should exist", standaloneScenario)
                assertSame("Standalone scenario should be top-level", psiFile, standaloneScenario?.parent)
                assertSame("Nested scenario should be child of feature", feature, nestedScenario?.parent)
        }

        fun testBackgroundStepDirectiveComposition() {
                val file = createScenarioFile("stepComposition", """
                        feature: feature description
                            background: background description
                                given background given description
                                    call ^operationId
                                        id: {{petId}}
                                then check the value
                                    assert status 2xx
                """.trimIndent())

                val psiFile = psiManager.findFile(file)
                assertNotNull("PSI file should be created", psiFile)

                val background = PsiTreeUtil.findChildrenOfType(psiFile, BerryCrushScenarioElement::class.java)
                        .firstOrNull { it.keyword == "background" }
                assertNotNull("Background element should exist", background)

                val steps = background?.steps.orEmpty()
                assertEquals("Background should contain two direct steps", 2, steps.size)

                val givenStep = steps.firstOrNull { it.keyword == "given" }
                val thenStep = steps.firstOrNull { it.keyword == "then" }
                assertNotNull("Given step should exist", givenStep)
                assertNotNull("Then step should exist", thenStep)

                val call = givenStep?.callDirectives?.singleOrNull()
                assertNotNull("Given step should contain nested call", call)
                assertEquals("operationId", call?.operationId)

                val assertDirective = thenStep?.assertDirectives?.singleOrNull()
                assertNotNull("Then step should contain nested assert", assertDirective)
                assertEquals("status 2xx", assertDirective?.assertionText)
        }

        fun testMalformedPayloadRecoveryKeepsHierarchy() {
                val file = createScenarioFile("recovery", """
                        feature: feature description
                            background: background description
                                given background given description
                                    call ^operationId
                                        id {{petId}}
                                then check the value
                                    assert status 2xx
                            scenario: nested scenario
                                given nested step
                """.trimIndent())

                val psiFile = psiManager.findFile(file)
                assertNotNull("PSI file should be created", psiFile)

                val feature = PsiTreeUtil.findChildOfType(psiFile, BerryCrushFeatureElement::class.java)
                assertNotNull("Feature should still be parsed", feature)

                val background = feature?.backgrounds?.singleOrNull()
                assertNotNull("Background should still be parsed", background)
                assertEquals(
                        "Background should keep step hierarchy after malformed payload",
                        2,
                        background?.steps?.size,
                )

                val nestedScenario = feature?.scenarios?.singleOrNull()
                assertNotNull("Nested scenario should still be present", nestedScenario)
        }

    fun testFragmentContainsNestedSteps() {
        val file = createFragmentFile("test", """
            fragment: my-fragment
            given step one
            when step two
            then step three
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull("PSI file should be created", psiFile)

        // Find fragment element
        val fragments = PsiTreeUtil.findChildrenOfType(psiFile, BerryCrushFragmentElement::class.java)
        assertEquals("Should find 1 fragment", 1, fragments.size)

        val fragment = fragments.first()
        assertEquals("my-fragment", fragment.fragmentName)

        // Find ALL steps in file (should be nested in fragment)
        val allSteps = PsiTreeUtil.findChildrenOfType(psiFile, BerryCrushStepElement::class.java)
        assertEquals("File should contain 3 steps", 3, allSteps.size)

        // Find steps nested in fragment
        val nestedSteps = PsiTreeUtil.findChildrenOfType(fragment, BerryCrushStepElement::class.java)
        assertEquals("Fragment should contain 3 nested steps", 3, nestedSteps.size)
    }

    fun testMultipleFragmentsAreSeparate() {
        val file = createFragmentFile("multi", """
            fragment: first
            given first step

            fragment: second
            when second step
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        val fragments = PsiTreeUtil.findChildrenOfType(psiFile, BerryCrushFragmentElement::class.java)
        assertEquals("Should find 2 fragments", 2, fragments.size)

        val firstFragment = fragments.find { it.fragmentName == "first" }
        val secondFragment = fragments.find { it.fragmentName == "second" }

        assertNotNull("First fragment should exist", firstFragment)
        assertNotNull("Second fragment should exist", secondFragment)

        // Steps should be in correct fragments
        val firstSteps = PsiTreeUtil.findChildrenOfType(firstFragment, BerryCrushStepElement::class.java)
        val secondSteps = PsiTreeUtil.findChildrenOfType(secondFragment, BerryCrushStepElement::class.java)

        assertEquals("First fragment should have 1 step", 1, firstSteps.size)
        assertEquals("Second fragment should have 1 step", 1, secondSteps.size)
    }

    // ========== Parameters Block Tests ==========

    fun testScenarioWithParametersBlock() {
        val file = createScenarioFile("params", """
            scenario: test with parameters
              parameters:
                timeout: 5000
                baseUrl: https://api.example.com
              given the setup
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull("PSI file should be created", psiFile)

        // Find scenario element
        val scenarios = PsiTreeUtil.findChildrenOfType(psiFile, BerryCrushScenarioElement::class.java)
        assertEquals("Should find 1 scenario", 1, scenarios.size)

        // Find parameters block
        val paramsBlocks = PsiTreeUtil.findChildrenOfType(psiFile, BerryCrushParametersBlockElement::class.java)
        assertEquals("Should find 1 parameters block", 1, paramsBlocks.size)

        // Check parameter entries
        val paramsBlock = paramsBlocks.first()
        val entries = paramsBlock.entries
        assertEquals("Parameters block should have 2 entries", 2, entries.size)
        assertTrue("Should have timeout parameter", paramsBlock.parameterNames.contains("timeout"))
        assertTrue("Should have baseUrl parameter", paramsBlock.parameterNames.contains("baseUrl"))
    }

    fun testParameterEntryParsing() {
        val file = createScenarioFile("entry", """
            scenario: test
              parameters:
                myParam: myValue
        """.trimIndent())

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        val entries = PsiTreeUtil.findChildrenOfType(psiFile, BerryCrushParameterEntryElement::class.java)
        assertEquals("Should find 1 parameter entry", 1, entries.size)

        val entry = entries.first()
        assertEquals("myParam", entry.parameterName)
        assertEquals("myValue", entry.parameterValue)
    }
}

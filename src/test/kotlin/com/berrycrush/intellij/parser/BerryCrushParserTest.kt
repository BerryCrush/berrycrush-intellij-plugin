package com.berrycrush.intellij.parser

import com.berrycrush.intellij.BerryCrushTestCase
import com.berrycrush.intellij.psi.BerryCrushAssertElement
import com.berrycrush.intellij.psi.BerryCrushBackgroundElement
import com.berrycrush.intellij.psi.BerryCrushCallElement
import com.berrycrush.intellij.psi.BerryCrushElseElement
import com.berrycrush.intellij.psi.BerryCrushExamplesElement
import com.berrycrush.intellij.psi.BerryCrushFeatureElement
import com.berrycrush.intellij.psi.BerryCrushFragmentElement
import com.berrycrush.intellij.psi.BerryCrushIfElement
import com.berrycrush.intellij.psi.BerryCrushIncludeParameterElement
import com.berrycrush.intellij.psi.BerryCrushOutlineElement
import com.berrycrush.intellij.psi.BerryCrushParameterEntryElement
import com.berrycrush.intellij.psi.BerryCrushParametersElement
import com.berrycrush.intellij.psi.BerryCrushPsiElement
import com.berrycrush.intellij.psi.BerryCrushScenarioElement
import com.berrycrush.intellij.psi.BerryCrushStepElement
import com.intellij.psi.PsiComment
import com.intellij.psi.util.PsiTreeUtil

/**
 * Tests for BerryCrush parser - verifies PSI tree structure.
 */
class BerryCrushParserTest : BerryCrushTestCase() {
    fun testCommentsInterleavedAcrossTopLevelAndFeatureKeepTreeStructure() {
        val file =
            createScenarioFile(
                "interleavedComments",
                """
                # top-level comment
                feature: feature with comments
                    # comment before background
                    background: bg
                        given step in background
                    # comment before nested scenario
                    scenario: nested scenario
                        given nested step

                # comment between top-level blocks
                scenario: standalone scenario
                    given standalone step
                """.trimIndent(),
            )

        val psiFile = psiManager.findFile(file)
        assertNotNull("PSI file should be created", psiFile)

        val feature = PsiTreeUtil.findChildOfType(psiFile, BerryCrushFeatureElement::class.java)
        assertNotNull("Feature element should exist", feature)
        assertEquals("feature with comments", feature?.description)
        assertEquals("Feature should contain one background", 1, feature?.backgrounds?.size)
        assertEquals("Feature should contain one nested scenario", 1, feature?.scenarios?.size)

        val standaloneScenario =
            PsiTreeUtil
                .findChildrenOfType(psiFile, BerryCrushScenarioElement::class.java)
                .firstOrNull { it.description == "standalone scenario" }
        assertNotNull("Standalone scenario should exist", standaloneScenario)
        assertSame("Standalone scenario should remain top-level", psiFile, standaloneScenario?.parent)

        val comments = PsiTreeUtil.findChildrenOfType(psiFile, PsiComment::class.java)
        assertTrue("Comment PSI nodes should be emitted", comments.size >= 3)
    }

    fun testCommentsInsideStepDirectivesAndPayloadKeepNesting() {
        val file =
            createScenarioFile(
                "nestedPayloadComments",
                """
                scenario: payload comments
                    given call with comments
                        call ^operationId
                            id: {{petId}}
                            # comment between payload entries
                            body:
                                # comment inside body block
                                name: foo
                    then verify response
                        assert status 2xx
                """.trimIndent(),
            )

        val psiFile = psiManager.findFile(file)
        assertNotNull("PSI file should be created", psiFile)

        val scenario = PsiTreeUtil.findChildOfType(psiFile, BerryCrushScenarioElement::class.java)
        assertNotNull("Scenario should exist", scenario)
        assertEquals("Scenario should keep two direct steps", 2, scenario?.steps?.size)

        val givenStep = scenario?.steps?.firstOrNull { it.keyword == "given" }
        val thenStep = scenario?.steps?.firstOrNull { it.keyword == "then" }
        assertNotNull("Given step should exist", givenStep)
        assertNotNull("Then step should exist", thenStep)

        val call = givenStep?.callDirectives?.singleOrNull()
        assertNotNull("Given step should keep nested call directive", call)

        val callEntries = PsiTreeUtil.findChildrenOfType(call, BerryCrushParameterEntryElement::class.java)
        val entryNames = callEntries.mapNotNull { it.parameterName }
        assertTrue("Call payload should contain id entry", entryNames.contains("id"))
        assertTrue("Call payload should contain body entry", entryNames.contains("body"))
        assertTrue("Call payload should contain nested name entry", entryNames.contains("name"))

        val assertDirective = thenStep?.assertDirectives?.singleOrNull()
        assertNotNull("Then step should remain parsed after payload comments", assertDirective)
    }

    fun testCommentBetweenCallAndBodyKeepsBodyInsideCallBlock() {
        val file =
            createScenarioFile(
                "commentBetweenCallAndBody",
                """
                scenario: Body syntax - structured body
                  when I create a pet with structured body
                    call ^createPet
                    # Uses OpenAPI schema defaults for unspecified fields
                      body:
                        name: StructuredBodyPet
                        status: pending
                """.trimIndent(),
            )

        val psiFile = psiManager.findFile(file)
        assertNotNull("PSI file should be created", psiFile)

        val scenario = PsiTreeUtil.findChildOfType(psiFile, BerryCrushScenarioElement::class.java)
        assertNotNull("Scenario should exist", scenario)

        val whenStep = scenario?.steps?.singleOrNull { it.keyword == "when" }
        assertNotNull("When step should exist", whenStep)

        val call = whenStep?.callDirectives?.singleOrNull()
        assertNotNull("Call directive should remain nested under when step", call)

        val callEntries = PsiTreeUtil.findChildrenOfType(call, BerryCrushParameterEntryElement::class.java)
        val entryNames = callEntries.mapNotNull { it.parameterName }
        assertTrue("body entry should be inside call parameter block", entryNames.contains("body"))
        assertTrue("nested body field 'name' should be parsed", entryNames.contains("name"))
        assertTrue("nested body field 'status' should be parsed", entryNames.contains("status"))
    }

    fun testCommentsInOutlineExamplesAndFragmentKeepContainment() {
        val scenarioFile =
            createScenarioFile(
                "outlineComments",
                """
                outline: commented outline
                    given setup step
                    examples:
                        # comment above header row
                        | name | value |
                        # comment between rows
                        | foo  | bar   |

                scenario: sibling scenario
                    given sibling step
                """.trimIndent(),
            )

        val psiScenarioFile = psiManager.findFile(scenarioFile)
        assertNotNull("Scenario PSI file should be created", psiScenarioFile)

        val outline = PsiTreeUtil.findChildOfType(psiScenarioFile, BerryCrushOutlineElement::class.java)
        assertNotNull("Outline should exist", outline)

        val examples = PsiTreeUtil.findChildOfType(outline, BerryCrushExamplesElement::class.java)
        assertNotNull("Examples block should be parsed under outline", examples)

        val siblingScenario =
            PsiTreeUtil
                .findChildrenOfType(psiScenarioFile, BerryCrushScenarioElement::class.java)
                .firstOrNull { it.description == "sibling scenario" }
        assertNotNull("Scenario after outline/examples should remain top-level", siblingScenario)
        assertSame("Sibling scenario should remain top-level", psiScenarioFile, siblingScenario?.parent)

        val fragmentFile =
            createFragmentFile(
                "fragmentComments",
                """
                fragment: commented-fragment
                # comment before first step
                  given first step
                  # indented step comment
                  when second step
                # comment between steps
                  then third step
                """.trimIndent(),
            )

        val psiFragmentFile = psiManager.findFile(fragmentFile)
        assertNotNull("Fragment PSI file should be created", psiFragmentFile)

        val fragment = PsiTreeUtil.findChildOfType(psiFragmentFile, BerryCrushFragmentElement::class.java)
        assertNotNull("Fragment should exist", fragment)

        val stepsInFragment = PsiTreeUtil.findChildrenOfType(fragment, BerryCrushStepElement::class.java)
        assertEquals("Fragment steps should remain contained despite comments", 3, stepsInFragment.size)
    }

    fun testProvidedSamplePsiHierarchy() {
        val file =
            createScenarioFile(
                "hierarchy",
                """
                feature: feature description
                    background:
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
                """.trimIndent(),
            )

        val psiFile = psiManager.findFile(file)
        assertNotNull("PSI file should be created", psiFile)

        val feature = PsiTreeUtil.findChildOfType(psiFile, BerryCrushFeatureElement::class.java)
        assertNotNull("Feature element should exist", feature)
        assertEquals("feature description", feature?.description)

        val background = feature?.backgrounds?.singleOrNull()
        assertNotNull("Feature should contain one background child", background)

        val nestedScenario = feature?.scenarios?.singleOrNull()
        assertNotNull("Feature should contain one nested scenario child", nestedScenario)
        assertEquals("nested scenario", nestedScenario?.description)

        val standaloneScenario =
            PsiTreeUtil
                .findChildrenOfType(psiFile, BerryCrushScenarioElement::class.java)
                .firstOrNull { it.keyword == "scenario" && it.description == "standalone scenario" }
        assertNotNull("Standalone scenario should exist", standaloneScenario)
        assertSame("Standalone scenario should be top-level", psiFile, standaloneScenario?.parent)
        assertSame("Nested scenario should be child of feature", feature, nestedScenario?.parent)
    }

    fun testBackgroundStepDirectiveComposition() {
        val file =
            createScenarioFile(
                "stepComposition",
                """
                feature: feature description
                    background: background description
                        given background given description
                            call ^operationId
                                id: {{petId}}
                        then check the value
                            assert status 2xx
                """.trimIndent(),
            )

        val psiFile = psiManager.findFile(file)
        assertNotNull("PSI file should be created", psiFile)

        val background =
            PsiTreeUtil
                .findChildrenOfType(psiFile, BerryCrushBackgroundElement::class.java)
                .firstOrNull()
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
        val file =
            createScenarioFile(
                "recovery",
                """
                feature: feature description
                    background: background description
                        given background given description
                            call ^operationId
                                id {{petId}}
                        then check the value
                            assert status 2xx
                    scenario: nested scenario
                        given nested step
                """.trimIndent(),
            )

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
        val file =
            createFragmentFile(
                "test",
                """
                fragment: my-fragment
                  given step one
                  when step two
                  then step three
                """.trimIndent(),
            )

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
        val file =
            createFragmentFile(
                "multi",
                """
                fragment: first
                  given first step

                fragment: second
                  when second step
                """.trimIndent(),
            )

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
        val file =
            createScenarioFile(
                "params",
                """
                scenario: test with parameters
                  # comment
                  parameters:
                    timeout: 5000
                    baseUrl: https://api.example.com
                  given the setup
                """.trimIndent(),
            )

        val psiFile = psiManager.findFile(file)
        assertNotNull("PSI file should be created", psiFile)

        // Find scenario element
        val scenarios = PsiTreeUtil.findChildrenOfType(psiFile, BerryCrushScenarioElement::class.java)
        assertEquals("Should find 1 scenario", 1, scenarios.size)

        // Find parameters block
        val paramsBlocks = PsiTreeUtil.findChildrenOfType(scenarios.firstOrNull(), BerryCrushParametersElement::class.java)
        assertEquals("Should find 1 parameters block", 1, paramsBlocks.size)

        // Check parameter entries
        val paramsBlock = paramsBlocks.first()
        val entries = paramsBlock.entries
        assertEquals("Parameters block should have 2 entries", 2, entries.size)
        assertTrue("Should have timeout parameter", paramsBlock.parameterNames.contains("timeout"))
        assertTrue("Should have baseUrl parameter", paramsBlock.parameterNames.contains("baseUrl"))
    }

    fun testParameterEntryParsing() {
        val file =
            createScenarioFile(
                "entry",
                """
                scenario: test
                  parameters:
                    myParam: myValue
                """.trimIndent(),
            )

        val psiFile = psiManager.findFile(file)
        assertNotNull(psiFile)

        val entries = PsiTreeUtil.findChildrenOfType(psiFile, BerryCrushParameterEntryElement::class.java)
        assertEquals("Should find 1 parameter entry", 1, entries.size)

        val entry = entries.first()
        assertEquals("myParam", entry.parameterName)
        assertEquals("myValue", entry.parameterValue)
    }

    fun testStepConditionalIfElseParsesAsNestedDirectives() {
        val file =
            createScenarioFile(
                "conditional",
                """
                scenario: conditional checks
                  then verify response
                    if status 2xx
                      assert $.id exists
                    else
                      assert status 5xx
                """.trimIndent(),
            )

        val psiFile = psiManager.findFile(file)
        assertNotNull("PSI file should be created", psiFile)

        val ifDirectives = PsiTreeUtil.findChildrenOfType(psiFile, BerryCrushIfElement::class.java)
        val elseDirectives = PsiTreeUtil.findChildrenOfType(psiFile, BerryCrushElseElement::class.java)
        assertEquals("Should find one if directive", 1, ifDirectives.size)
        assertEquals("Should find one else directive", 1, elseDirectives.size)

        val ifAssertions = PsiTreeUtil.findChildrenOfType(ifDirectives.first(), BerryCrushAssertElement::class.java)
        val elseAssertions = PsiTreeUtil.findChildrenOfType(elseDirectives.first(), BerryCrushAssertElement::class.java)
        assertEquals("If branch should contain one assert", 1, ifAssertions.size)
        assertEquals("Else branch should contain one assert", 1, elseAssertions.size)
    }

    fun testIncludedParametersWithNewlinedValues() {
        val file =
            createScenarioFile(
                "key-pair",
                """
                scenario: conditional checks
                  when I call
                    call ^operationId
                      id:
                        1234
                      body:
                        ${'"'}""
                          {
                            "body": "something"
                          }
                        ${'"'}""
                """.trimIndent(),
            )

        val psiFile = psiManager.findFile(file)
        assertNotNull("PSI file should be created", psiFile)

        val callElement = PsiTreeUtil.findChildrenOfType(psiFile, BerryCrushCallElement::class.java)
        assertEquals("Should find one call directive", 1, callElement.size)

        val includedElement = PsiTreeUtil.findChildrenOfType(callElement.first(), BerryCrushIncludeParameterElement::class.java)
        assertEquals("Should find one included parameter", 1, includedElement.size)

        val entries = includedElement.first().entries
        assertEquals("Should find 2 included parameter entries", 2, entries.size)
    }

    fun testCommentRightAfterStep() {
        val file =
            createScenarioFile(
                "test",
                """
                scenario: comment check
                  when custom step
                  then custom step
                # ----------------
                # comment line
                # ----------------
                """.trimIndent(),
            )

        val psiFile = psiManager.findFile(file)
        assertNotNull("PSI file should be created", psiFile)

        val stepElements = PsiTreeUtil.findChildrenOfType(psiFile, BerryCrushStepElement::class.java)
        assertEquals("Should find one step", 2, stepElements.size)

        val stepText = stepElements.toList()[1].stepText
        assertEquals("Must only contain step text", "custom step", stepText)
    }

    fun testNestedFeatureWithTaggedScenario() {
        val file =
            createScenarioFile(
                "test",
                """
                feature: this
                  @api scenario: this belongs to the feature
                    when do something
                """.trimIndent(),
            )

        val psiFile = psiManager.findFile(file)
        assertNotNull("PSI file should be created", psiFile)

        val featureElements = PsiTreeUtil.findChildrenOfType(psiFile, BerryCrushFeatureElement::class.java)
        assertEquals("Should one feature", 1, featureElements.size)

        assertEquals("Should contain one scenario", 1, featureElements.firstOrNull()?.scenarios?.size)
    }

    fun testMultipleTaggedFeature() {
        val file =
            createScenarioFile(
                "test",
                """
                feature: this
                @api
                feature: that
                """.trimIndent(),
            )

        val psiFile = psiManager.findFile(file)
        assertNotNull("PSI file should be created", psiFile)

        val elements = psiFile?.children?.filterIsInstance<BerryCrushPsiElement>()
        assertEquals("Should find 3 element", 3, elements?.size)
    }
}

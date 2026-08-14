package org.berrycrush.intellij.psi

import org.berrycrush.intellij.BerryCrushTestCase
import org.junit.jupiter.api.Test

/**
 * Tests for BerryCrush PSI element classes.
 * Verifies name extraction and property access for all element types.
 */
class BerryCrushElementsTest : BerryCrushTestCase() {
    // ========== BerryCrushScenarioElement Tests ==========
    @Test
    fun testScenarioNameExtraction() {
        val file =
            createScenarioFile(
                "test",
                """
                scenario: My Test Scenario
                given a precondition
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        assertNotNull(psiFile)

        val scenario = findChildOfType(psiFile, BerryCrushScenarioElement::class.java)
        consume {
            assertNotNull("Scenario element should exist", scenario)
            assertEquals("My Test Scenario", scenario?.description)
            assertEquals("My Test Scenario", scenario?.name)
        }
    }

    @Test
    fun testScenarioNameWithLeadingWhitespace() {
        val file =
            createScenarioFile(
                "whitespace",
                """
                scenario:   Spaced Name   
                given step
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val scenario = findChildOfType(psiFile, BerryCrushScenarioElement::class.java)
        consume {
            assertEquals("Spaced Name", scenario?.description)
        }
    }

    @Test
    fun testScenarioNameLowercase() {
        val file =
            createScenarioFile(
                "lower",
                """
                scenario: lowercase scenario name
                given step
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val scenario = findChildOfType(psiFile, BerryCrushScenarioElement::class.java)
        consume {
            assertNotNull("Scenario with lowercase 'scenario' should be found", scenario)
            assertEquals("lowercase scenario name", scenario?.description)
        }
    }

    // ========== BerryCrushFragmentElement Tests ==========

    @Test
    fun testFragmentNameExtraction() {
        val file =
            createFragmentFile(
                "test",
                """
                fragment: my-fragment-name
                given a step
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val fragment = findChildOfType(psiFile, BerryCrushFragmentElement::class.java)
        consume {
            assertNotNull("Fragment element should exist", fragment)
            assertEquals("my-fragment-name", fragment?.description)
            assertEquals("my-fragment-name", fragment?.name)
        }
    }

    @Test
    fun testFragmentNameLowercase() {
        val file =
            createFragmentFile(
                "lower",
                """
                fragment: lowercase-fragment
                given step
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val fragment = findChildOfType(psiFile, BerryCrushFragmentElement::class.java)
        consume {
            assertNotNull("Fragment with lowercase 'fragment' should be found", fragment)
            assertEquals("lowercase-fragment", fragment?.description)
        }
    }

    @Test
    fun testFragmentNameWithDots() {
        val file =
            createFragmentFile(
                "dots",
                """
                fragment: com.example.my-fragment
                given step
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val fragment = findChildOfType(psiFile, BerryCrushFragmentElement::class.java)
        assertEquals("com.example.my-fragment", fragment?.description)
    }

    // ========== BerryCrushFeatureElement Tests ==========

    @Test
    fun testFeatureNameExtraction() {
        val file =
            createScenarioFile(
                "feature",
                """
                feature: User Authentication
                scenario: Login
                given user exists
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val feature = findChildOfType(psiFile, BerryCrushFeatureElement::class.java)
        consume {
            assertNotNull("Feature element should exist", feature)
            assertEquals("User Authentication", feature?.description)
            assertEquals("User Authentication", feature?.name)
        }
    }

    @Test
    fun testFeatureNameLowercase() {
        val file =
            createScenarioFile(
                "lowerFeature",
                """
                feature: lowercase feature
                scenario: test
                given step
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val feature = findChildOfType(psiFile, BerryCrushFeatureElement::class.java)
        consume {
            assertNotNull("Feature with lowercase 'feature' should be found", feature)
            assertEquals("lowercase feature", feature?.description)
        }
    }

    // ========== BerryCrushStepElement Tests ==========

    @Test
    fun testStepKeywordGiven() {
        val file =
            createFragmentFile(
                "given",
                """
                fragment: test
                given a precondition is met
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val step = findChildOfType(psiFile, BerryCrushStepElement::class.java)
        consume {
            assertNotNull("Step element should exist", step)
            assertEquals("given", step?.keyword)
            assertEquals("a precondition is met", step?.stepText)
        }
    }

    @Test
    fun testStepKeywordWhen() {
        val file =
            createFragmentFile(
                "when",
                """
                fragment: test
                when user performs action
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val step = findChildOfType(psiFile, BerryCrushStepElement::class.java)
        consume {
            assertEquals("when", step?.keyword)
            assertEquals("user performs action", step?.stepText)
        }
    }

    @Test
    fun testStepKeywordThen() {
        val file =
            createFragmentFile(
                "then",
                """
                fragment: test
                then result is verified
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val step = findChildOfType(psiFile, BerryCrushStepElement::class.java)
        consume {
            assertEquals("then", step?.keyword)
            assertEquals("result is verified", step?.stepText)
        }
    }

    @Test
    fun testStepKeywordAnd() {
        val file =
            createFragmentFile(
                "and",
                """
                fragment: test
                and another condition
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val step = findChildOfType(psiFile, BerryCrushStepElement::class.java)
        consume {
            assertEquals("and", step?.keyword)
            assertEquals("another condition", step?.stepText)
        }
    }

    @Test
    fun testStepKeywordBut() {
        val file =
            createFragmentFile(
                "but",
                """
                fragment: test
                but not this condition
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val step = findChildOfType(psiFile, BerryCrushStepElement::class.java)
        consume {
            assertEquals("but", step?.keyword)
            assertEquals("not this condition", step?.stepText)
        }
    }

    @Test
    fun testStepKeywordsCaseInsensitive() {
        // Uppercase keywords should NOT be recognized (strict lowercase per BerryCrush DSL spec)
        val file =
            createFragmentFile(
                "case",
                """
                fragment: test
                GIVEN uppercase step
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val step = findChildOfType(psiFile, BerryCrushStepElement::class.java)
        // Step should not be found because GIVEN is not a valid keyword
        assertNull("Uppercase GIVEN should not be recognized as a step", step)
    }

    // ========== BerryCrushIncludeElement Tests ==========

    @Test
    fun testIncludeFragmentName() {
        val file =
            createScenarioFile(
                "include",
                """
                scenario: Test
                include my-fragment
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val include = findChildOfType(psiFile, BerryCrushIncludeElement::class.java)
        assertNotNull("Include element should exist", include)
        assertEquals("my-fragment", include?.fragmentName)
        assertEquals("include", include?.directiveName)
    }

    @Test
    fun testIncludeFragmentNameWithCaret() {
        val file =
            createScenarioFile(
                "includeCaret",
                """
                scenario: Test
                include my-fragment
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val include = findChildOfType(psiFile, BerryCrushIncludeElement::class.java)
        assertEquals("my-fragment", include?.fragmentName)
    }

    @Test
    fun testIncludeFragmentNameWithDots() {
        val file =
            createScenarioFile(
                "includeDots",
                """
                scenario: Test
                include com.example.fragment
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val include = findChildOfType(psiFile, BerryCrushIncludeElement::class.java)
        assertEquals("com.example.fragment", include?.fragmentName)
    }

    // ========== BerryCrushCallElement Tests ==========

    @Test
    fun testCallOperationId() {
        val file =
            createScenarioFile(
                "call",
                """
                scenario: Test
                call ^getPet
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val call = findChildOfType(psiFile, BerryCrushCallElement::class.java)
        assertNotNull("Call element should exist", call)
        assertEquals("getPet", call?.operationId)
    }

    @Test
    fun testCallOperationRefElement() {
        val file =
            createScenarioFile(
                "callRef",
                """
                scenario: Test
                call ^createOrder
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val call = findChildOfType(psiFile, BerryCrushCallElement::class.java)
        val opRef = call?.operationRef
        assertNotNull("Operation ref should exist", opRef)
        assertEquals("createOrder", opRef?.operationId)
        assertEquals("createOrder", opRef?.name)
    }

    // ========== BerryCrushOperationRefElement Tests ==========

    @Test
    fun testOperationRefRemovesCaret() {
        val file =
            createScenarioFile(
                "opRef",
                """
                scenario: Test
                call ^myOperation
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val opRef = findChildOfType(psiFile, BerryCrushOperationRefElement::class.java)
        assertNotNull("Operation ref element should exist", opRef)
        assertEquals("myOperation", opRef?.operationId)
        assertEquals("myOperation", opRef?.name)
    }

    @Test
    fun testOperationRefHasReference() {
        val file =
            createScenarioFile(
                "opRefRef",
                """
                scenario: Test
                call ^targetOperation
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val opRef = findChildOfType(psiFile, BerryCrushOperationRefElement::class.java)
        val reference = opRef?.reference
        assertNotNull("Operation ref should have reference", reference)
    }

    // ========== BerryCrushAssertElement Tests ==========

    @Test
    fun testAssertText() {
        val file =
            createScenarioFile(
                "assert",
                """
                scenario: Test
                assert response.status == 200
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        consume {
            val assertElement = findChildOfType(psiFile, BerryCrushAssertElement::class.java)
            assertNotNull("Assert element should exist", assertElement)
            assertEquals("response.status == 200", assertElement?.assertionText)
        }
    }

    @Test
    fun testAssertTextWithMultipleSpaces() {
        val file =
            createScenarioFile(
                "assertSpaces",
                """
                scenario: Test
                assert   response.body.name == "test"
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val assert = findChildOfType(psiFile, BerryCrushAssertElement::class.java)
        consume {
            assertEquals("response.body.name == \"test\"", assert?.assertionText)
        }
    }

    @Test
    fun testCallPayloadHierarchyExtraction() {
        val file =
            createScenarioFile(
                "payloadHierarchy",
                """
                feature: feature description
                    background: background description
                        given background given description
                            call ^operationId
                                id: {{petId}}
                                body:
                                    name: foo
                        then check the value
                            assert status 2xx
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val call = findChildOfType(psiFile, BerryCrushCallElement::class.java)
        consume {
            assertNotNull("Call element should exist", call)
            assertEquals("operationId", call?.operationId)

            val idParam = call?.findParameter("id")
            assertNotNull("Call should expose id parameter", idParam)
            assertEquals("{{petId}}", idParam?.parameterValue)

            val bodyParam = call?.findParameter("body")
            assertNotNull("Call should expose body parameter", bodyParam)

            val bodyName = bodyParam?.findNestedParameter("name")
            assertNotNull("Body should expose nested name field", bodyName)
            assertEquals("foo", bodyName?.parameterValue)

            val assertDirective = findChildOfType(psiFile, BerryCrushAssertElement::class.java)
            assertNotNull("Assert directive should exist", assertDirective)
            assertEquals("status 2xx", assertDirective?.assertionText)
        }
    }

    // ========== BerryCrushFragmentRefElement Tests ==========

    @Test
    fun testFragmentRefRemovesCaret() {
        val file =
            createScenarioFile(
                "fragRef",
                """
                scenario: Test
                include my-fragment
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val fragRef = findChildOfType(psiFile, BerryCrushFragmentRefElement::class.java)
        assertNotNull("Fragment ref element should exist", fragRef)
        assertEquals("my-fragment", fragRef?.name)
    }

    @Test
    fun testFragmentRefHasReference() {
        val file =
            createScenarioFile(
                "fragRefRef",
                """
                scenario: Test
                include ^target-fragment
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val fragRef = findChildOfType(psiFile, BerryCrushFragmentRefElement::class.java)
        val reference = fragRef?.reference
        assertNotNull("Fragment ref should have reference", reference)
    }

    @Test
    fun testStringLiteralSegmentOrderForMixedTextAndInterpolation() {
        val file =
            createScenarioFile(
                "stringSegments",
                """
                scenario: interpolation
                  parameters:
                    message: "hello {{name}} world"
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val stringLiteral = findChildOfType(psiFile, BerryCrushStringLiteralElement::class.java)
        assertNotNull("String literal should exist", stringLiteral)

        val segments = stringLiteral?.segments.orEmpty()
        assertEquals(3, segments.size)
        assertTrue(segments[0] is BerryCrushStringTextSegment)
        assertTrue(segments[1] is BerryCrushStringVariableSegment)
        assertTrue(segments[2] is BerryCrushStringTextSegment)
    }

    @Test
    fun testMultilineStringSegmentsPreserveIndentation() {
        val file =
            createScenarioFile(
                "multilineSegments",
                """
                scenario: interpolation
                  parameters:
                    message: ${'"'}""
                      hello
                        {{name}}
                    ${'"'}""
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val stringLiteral = findChildOfType(psiFile, BerryCrushStringLiteralElement::class.java)
        assertNotNull("String literal should exist", stringLiteral)

        val segments = stringLiteral?.segments.orEmpty()
        assertTrue(
            "Expected explicit indentation segment in multiline string",
            segments.any { it is BerryCrushStringIndentSegment && it.text.contains("  ") },
        )
    }
}

package org.berrycrush.intellij.run

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BerryCrushScenarioExecutionSupportTest {
    @Test
    fun `selectPreferredCandidate prefers class in preferred module`() {
        val selected =
            BerryCrushScenarioExecutionSupport.selectPreferredCandidate(
                listOf(
                    ClassCandidate(
                        value = "com.example.ZetaTest",
                        qualifiedName = "com.example.ZetaTest",
                        inPreferredModule = false,
                    ),
                    ClassCandidate(
                        value = "com.example.AlphaTest",
                        qualifiedName = "com.example.AlphaTest",
                        inPreferredModule = true,
                    ),
                ),
            )

        assertEquals("com.example.AlphaTest", selected)
    }

    @Test
    fun `selectPreferredCandidate uses lexical order for deterministic tie break`() {
        val selected =
            BerryCrushScenarioExecutionSupport.selectPreferredCandidate(
                listOf(
                    ClassCandidate(
                        value = "com.example.ZetaTest",
                        qualifiedName = "com.example.ZetaTest",
                        inPreferredModule = false,
                    ),
                    ClassCandidate(
                        value = "com.example.AlphaTest",
                        qualifiedName = "com.example.AlphaTest",
                        inPreferredModule = false,
                    ),
                ),
            )

        assertEquals("com.example.AlphaTest", selected)
    }

    @Test
    fun `selectPreferredCandidate returns null for empty candidates`() {
        val selected = BerryCrushScenarioExecutionSupport.selectPreferredCandidate<String>(emptyList())
        assertNull(selected)
    }

    @Test
    fun `buildVmOption quotes values containing spaces`() {
        val option = BerryCrushScenarioExecutionSupport.buildVmOption("berryCrush.scenarioFile", "pet store.scenario")
        assertEquals("-DberryCrush.scenarioFile=\"pet store.scenario\"", option)
    }

    @Test
    fun `buildVmOptions includes scenario file and scenario name for scenario keyword`() {
        val vmOptions =
            BerryCrushScenarioExecutionSupport.buildVmOptions(
                scenarioFile = "petstore.scenario",
                scenarioName = "Create Pet",
                keywordType = "Scenario",
            )

        assertEquals(
            "-DberryCrush.scenarioFile=petstore.scenario -DberryCrush.scenarioName=\"Create Pet\"",
            vmOptions,
        )
    }

    @Test
    fun `buildVmOptions includes feature name for feature keyword`() {
        val vmOptions =
            BerryCrushScenarioExecutionSupport.buildVmOptions(
                scenarioFile = "petstore.scenario",
                scenarioName = "Petstore API",
                keywordType = "Feature",
            )

        assertEquals(
            "-DberryCrush.scenarioFile=petstore.scenario -DberryCrush.featureName=\"Petstore API\"",
            vmOptions,
        )
    }

    @Test
    fun `buildVmOptions from file context only includes scenario file filter`() {
        val vmOptions = BerryCrushScenarioExecutionSupport.buildVmOptions(scenarioFile = "petstore.scenario")
        assertEquals("-DberryCrush.scenarioFile=petstore.scenario", vmOptions)
    }

    @Test
    fun `buildChoiceContextLabel includes package only when module is not requested`() {
        val label =
            BerryCrushScenarioExecutionSupport.buildChoiceContextLabel(
                packageName = "com.example.tests",
                moduleName = "intellij",
                includeModuleName = false,
            )

        assertEquals(" (com.example.tests)", label)
    }

    @Test
    fun `buildChoiceContextLabel includes package and module when module is requested`() {
        val label =
            BerryCrushScenarioExecutionSupport.buildChoiceContextLabel(
                packageName = "com.example.tests",
                moduleName = "intellij",
                includeModuleName = true,
            )

        assertEquals(" (com.example.tests - intellij)", label)
    }

    @Test
    fun `buildChoiceContextLabel handles empty package and module`() {
        val label =
            BerryCrushScenarioExecutionSupport.buildChoiceContextLabel(
                packageName = "",
                moduleName = null,
                includeModuleName = true,
            )

        assertEquals("", label)
    }
}

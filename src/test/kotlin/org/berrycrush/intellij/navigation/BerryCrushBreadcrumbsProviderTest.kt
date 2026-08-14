package org.berrycrush.intellij.navigation

import org.berrycrush.intellij.BerryCrushTestCase
import org.berrycrush.intellij.language.BerryCrushLanguage
import org.berrycrush.intellij.psi.BerryCrushFragmentElement
import org.berrycrush.intellij.psi.BerryCrushScenarioElement
import org.berrycrush.intellij.psi.BerryCrushStepElement
import org.junit.jupiter.api.Test

/**
 * Tests for BerryCrush breadcrumbs provider.
 * Verifies breadcrumb display and navigation for scenario/fragment hierarchies.
 */
class BerryCrushBreadcrumbsProviderTest : BerryCrushTestCase() {
    private val provider = BerryCrushBreadcrumbsProvider()

    // ========== Language Tests ==========

    @Test
    fun testSupportsBerryCrushLanguage() {
        val languages = provider.languages
        assertTrue(
            "Should support BerryCrush language",
            languages.contains(BerryCrushLanguage),
        )
    }

    // ========== acceptElement Tests ==========

    @Test
    fun testAcceptScenarioElement() {
        val file =
            createScenarioFile(
                "accept",
                """
                scenario: Test Scenario
                given step
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val scenario = findChildOfType(psiFile, BerryCrushScenarioElement::class.java)

        assertNotNull(scenario)
        assertTrue(
            "Should accept scenario element",
            provider.acceptElement(scenario!!),
        )
    }

    @Test
    fun testAcceptFragmentElement() {
        val file =
            createFragmentFile(
                "accept",
                """
                fragment: test-fragment
                given step
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val fragment = findChildOfType(psiFile, BerryCrushFragmentElement::class.java)

        assertNotNull(fragment)
        assertTrue(
            "Should accept fragment element",
            provider.acceptElement(fragment!!),
        )
    }

    @Test
    fun testAcceptStepElement() {
        val file =
            createFragmentFile(
                "acceptStep",
                """
                fragment: test
                given a step
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val step = findChildOfType(psiFile, BerryCrushStepElement::class.java)

        assertNotNull(step)
        assertTrue(
            "Should accept step element",
            provider.acceptElement(step!!),
        )
    }

    // ========== getElementInfo Tests ==========

    @Test
    fun testElementInfoForScenario() {
        val file =
            createScenarioFile(
                "info",
                """
                scenario: My Test Scenario
                given step
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val scenario = findChildOfType(psiFile, BerryCrushScenarioElement::class.java)

        assertNotNull(scenario)
        val info = consume { provider.getElementInfo(scenario!!) }
        assertTrue(
            "Info should contain 'scenario: My Test Scenario', got: $info",
            info.contains("scenario") && info.contains("My Test Scenario"),
        )
    }

    @Test
    fun testElementInfoForFragment() {
        val file =
            createFragmentFile(
                "info",
                """
                fragment: my-fragment
                given step
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val fragment = findChildOfType(psiFile, BerryCrushFragmentElement::class.java)

        assertNotNull(fragment)
        val info = provider.getElementInfo(fragment!!)
        assertTrue(
            "Info should contain 'fragment: my-fragment', got: $info",
            info.contains("fragment") && info.contains("my-fragment"),
        )
    }

    @Test
    fun testElementInfoForStep() {
        val file =
            createFragmentFile(
                "stepInfo",
                """
                fragment: test
                given a precondition is met
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val step = findChildOfType(psiFile, BerryCrushStepElement::class.java)

        assertNotNull(step)
        val info = consume { provider.getElementInfo(step!!) }
        assertTrue(
            "Info should contain step info, got: $info",
            info.contains("given") || info.contains("precondition"),
        )
    }

    // ========== getParent Tests ==========

    @Test
    fun testStepParentIsFragment() {
        val file =
            createFragmentFile(
                "parent",
                """
                fragment: test-fragment
                given a step
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val step = findChildOfType(psiFile, BerryCrushStepElement::class.java)

        assertNotNull(step)
        val parent = consume { provider.getParent(step!!) }

        // Parent should be the fragment
        assertTrue(
            "Step parent should be fragment",
            parent is BerryCrushFragmentElement,
        )
    }

    @Test
    fun testFragmentParentIsNull() {
        val file =
            createFragmentFile(
                "topLevel",
                """
                fragment: test-fragment
                given a step
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val fragment = findChildOfType(psiFile, BerryCrushFragmentElement::class.java)

        assertNotNull(fragment)
        val parent = consume { provider.getParent(fragment!!) }

        // Fragment is top-level, so parent should be null
        assertNull("Fragment should have no parent in breadcrumbs", parent)
    }

    // ========== getElementTooltip Tests ==========

    @Test
    fun testTooltipReturnsElementText() {
        val file =
            createFragmentFile(
                "tooltip",
                """
                fragment: test-fragment
                given a step
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val fragment = findChildOfType(psiFile, BerryCrushFragmentElement::class.java)

        assertNotNull(fragment)
        val tooltip = consume { provider.getElementTooltip(fragment!!) }

        assertNotNull("Tooltip should not be null", tooltip)
        assertTrue(
            "Tooltip should contain element text",
            tooltip!!.contains("Fragment") || tooltip.contains("fragment"),
        )
    }

    @Test
    fun testLongTooltipIsTruncated() {
        val longName = "a".repeat(100)
        val file =
            createFragmentFile(
                "longTooltip",
                """
                fragment: $longName
                given step
                """.trimIndent(),
            )

        val psiFile = findFile(file)
        val fragment = findChildOfType(psiFile, BerryCrushFragmentElement::class.java)

        assertNotNull(fragment)
        val tooltip = consume { provider.getElementTooltip(fragment!!) }

        assertNotNull("Tooltip should not be null", tooltip)
        // Long tooltips should be truncated with "..."
        if (tooltip!!.length > 103) {
            assertTrue("Long tooltip should end with ...", tooltip.endsWith("..."))
        }
    }
}

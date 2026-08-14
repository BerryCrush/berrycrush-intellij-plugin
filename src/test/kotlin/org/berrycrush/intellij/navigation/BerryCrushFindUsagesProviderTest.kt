package org.berrycrush.intellij.navigation

import org.berrycrush.intellij.BerryCrushTestCase
import org.berrycrush.intellij.psi.BerryCrushFile
import org.berrycrush.intellij.psi.BerryCrushFragmentElement
import org.berrycrush.intellij.psi.BerryCrushScenarioElement
import org.junit.jupiter.api.Test

/**
 * Tests for BerryCrush Find Usages provider.
 * Verifies Find Usages (Alt+F7) functionality for fragments and elements.
 */
class BerryCrushFindUsagesProviderTest : BerryCrushTestCase() {
    private val provider = BerryCrushFindUsagesProvider()

    // ========== canFindUsagesFor Tests ==========

    @Test
    fun testCanFindUsagesForFile() {
        val file =
            createScenarioFile(
                "usages",
                """
                scenario: Test
                given step
                """.trimIndent(),
            )

        val psiFile = findFile(file) as? BerryCrushFile
        assertNotNull(psiFile)

        assertTrue(
            "Should allow find usages for BerryCrush files",
            provider.canFindUsagesFor(psiFile!!),
        )
    }

    @Test
    fun testCanFindUsagesForNamedElement() {
        val file =
            createFragmentFile(
                "named",
                """
                fragment: my-fragment
                given step
                """.trimIndent(),
            )

        val psiFile = findFile(file) as? BerryCrushFile
        val fragment = findChildOfType(psiFile, BerryCrushFragmentElement::class.java)
        assertNotNull(fragment)

        assertTrue(
            "Should allow find usages for named elements",
            provider.canFindUsagesFor(fragment!!),
        )
    }

    // ========== getType Tests ==========

    @Test
    fun testTypeForFragmentFile() {
        val file =
            createFragmentFile(
                "typeTest",
                """
                fragment: test
                given step
                """.trimIndent(),
            )

        val psiFile = findFile(file) as? BerryCrushFile
        assertNotNull(psiFile)

        val type = provider.getType(psiFile!!)
        assertEquals("fragment", type)
    }

    @Test
    fun testTypeForScenarioFile() {
        val file =
            createScenarioFile(
                "typeTest",
                """
                scenario: Test
                given step
                """.trimIndent(),
            )

        val psiFile = findFile(file) as? BerryCrushFile
        assertNotNull(psiFile)

        val type = provider.getType(psiFile!!)
        assertEquals("scenario", type)
    }

    @Test
    fun testTypeForOtherElement() {
        val file =
            createScenarioFile(
                "otherType",
                """
                scenario: Test
                given step
                """.trimIndent(),
            )

        val psiFile = findFile(file) as? BerryCrushFile
        val scenario = findChildOfType(psiFile, BerryCrushScenarioElement::class.java)
        assertNotNull(scenario)

        val type = provider.getType(scenario!!)
        assertEquals("element", type)
    }

    // ========== getDescriptiveName Tests ==========

    @Test
    fun testDescriptiveNameForFile() {
        val file =
            createScenarioFile(
                "descriptive",
                """
                scenario: Test
                given step
                """.trimIndent(),
            )

        val psiFile = findFile(file) as? BerryCrushFile
        assertNotNull(psiFile)

        val name = provider.getDescriptiveName(psiFile!!)
        assertEquals("descriptive.scenario", name)
    }

    @Test
    fun testDescriptiveNameForNamedElement() {
        val file =
            createFragmentFile(
                "descName",
                """
                fragment: my-fragment
                given step
                """.trimIndent(),
            )

        val psiFile = findFile(file) as? BerryCrushFile
        val fragment = findChildOfType(psiFile, BerryCrushFragmentElement::class.java)
        assertNotNull(fragment)

        val name = provider.getDescriptiveName(fragment!!)
        assertEquals("my-fragment", name)
    }

    // ========== getNodeText Tests ==========

    @Test
    fun testNodeTextForFile() {
        val file =
            createScenarioFile(
                "nodeText",
                """
                scenario: Test
                given step
                """.trimIndent(),
            )

        val psiFile = findFile(file) as? BerryCrushFile
        assertNotNull(psiFile)

        val text = provider.getNodeText(psiFile!!, false)
        assertEquals("nodeText.scenario", text)
    }

    @Test
    fun testNodeTextForFragment() {
        val file =
            createFragmentFile(
                "nodeTextFrag",
                """
                fragment: test-fragment
                given step
                """.trimIndent(),
            )

        val psiFile = findFile(file) as? BerryCrushFile
        val fragment = findChildOfType(psiFile, BerryCrushFragmentElement::class.java)
        assertNotNull(fragment)

        val text = provider.getNodeText(fragment!!, false)
        assertEquals("test-fragment", text)
    }

    // ========== getHelpId Tests ==========

    @Test
    fun testHelpIdReturnsNull() {
        val file =
            createScenarioFile(
                "help",
                """
                scenario: Test
                given step
                """.trimIndent(),
            )

        val psiFile = findFile(file) as? BerryCrushFile
        assertNotNull(psiFile)

        val helpId = provider.getHelpId(psiFile!!)
        assertNull("Help ID should be null", helpId)
    }

    // ========== getWordsScanner Tests ==========

    @Test
    fun testWordsScannerReturnsScanner() {
        val scanner = provider.wordsScanner
        assertNotNull("Should return a words scanner", scanner)
    }
}

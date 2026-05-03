package com.berrycrush.intellij.navigation

import com.berrycrush.intellij.BerryCrushTestCase
import com.berrycrush.intellij.psi.BerryCrushFile
import com.berrycrush.intellij.psi.BerryCrushFragmentElement
import com.berrycrush.intellij.psi.BerryCrushScenarioElement
import com.intellij.psi.util.PsiTreeUtil

/**
 * Tests for BerryCrush Find Usages provider.
 * Verifies Find Usages (Alt+F7) functionality for fragments and elements.
 */
class BerryCrushFindUsagesProviderTest : BerryCrushTestCase() {

    private val provider = BerryCrushFindUsagesProvider()

    // ========== canFindUsagesFor Tests ==========

    fun testCanFindUsagesForFile() {
        val file = createScenarioFile("usages", """
            scenario: Test
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file) as? BerryCrushFile
        assertNotNull(psiFile)

        assertTrue(
            "Should allow find usages for BerryCrush files",
            provider.canFindUsagesFor(psiFile!!)
        )
    }

    fun testCanFindUsagesForNamedElement() {
        val file = createFragmentFile("named", """
            fragment: my-fragment
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file) as? BerryCrushFile
        val fragment = PsiTreeUtil.findChildOfType(psiFile, BerryCrushFragmentElement::class.java)
        assertNotNull(fragment)

        assertTrue(
            "Should allow find usages for named elements",
            provider.canFindUsagesFor(fragment!!)
        )
    }

    // ========== getType Tests ==========

    fun testTypeForFragmentFile() {
        val file = createFragmentFile("typeTest", """
            fragment: test
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file) as? BerryCrushFile
        assertNotNull(psiFile)

        val type = provider.getType(psiFile!!)
        assertEquals("fragment", type)
    }

    fun testTypeForScenarioFile() {
        val file = createScenarioFile("typeTest", """
            scenario: Test
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file) as? BerryCrushFile
        assertNotNull(psiFile)

        val type = provider.getType(psiFile!!)
        assertEquals("scenario", type)
    }

    fun testTypeForOtherElement() {
        val file = createScenarioFile("otherType", """
            scenario: Test
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file) as? BerryCrushFile
        val scenario = PsiTreeUtil.findChildOfType(psiFile, BerryCrushScenarioElement::class.java)
        assertNotNull(scenario)

        val type = provider.getType(scenario!!)
        assertEquals("element", type)
    }

    // ========== getDescriptiveName Tests ==========

    fun testDescriptiveNameForFile() {
        val file = createScenarioFile("descriptive", """
            scenario: Test
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file) as? BerryCrushFile
        assertNotNull(psiFile)

        val name = provider.getDescriptiveName(psiFile!!)
        assertEquals("descriptive.scenario", name)
    }

    fun testDescriptiveNameForNamedElement() {
        val file = createFragmentFile("descName", """
            fragment: my-fragment
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file) as? BerryCrushFile
        val fragment = PsiTreeUtil.findChildOfType(psiFile, BerryCrushFragmentElement::class.java)
        assertNotNull(fragment)

        val name = provider.getDescriptiveName(fragment!!)
        assertEquals("my-fragment", name)
    }

    // ========== getNodeText Tests ==========

    fun testNodeTextForFile() {
        val file = createScenarioFile("nodeText", """
            scenario: Test
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file) as? BerryCrushFile
        assertNotNull(psiFile)

        val text = provider.getNodeText(psiFile!!, false)
        assertEquals("nodeText.scenario", text)
    }

    fun testNodeTextForFragment() {
        val file = createFragmentFile("nodeTextFrag", """
            fragment: test-fragment
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file) as? BerryCrushFile
        val fragment = PsiTreeUtil.findChildOfType(psiFile, BerryCrushFragmentElement::class.java)
        assertNotNull(fragment)

        val text = provider.getNodeText(fragment!!, false)
        assertEquals("test-fragment", text)
    }

    // ========== getHelpId Tests ==========

    fun testHelpIdReturnsNull() {
        val file = createScenarioFile("help", """
            scenario: Test
            given step
        """.trimIndent())

        val psiFile = psiManager.findFile(file) as? BerryCrushFile
        assertNotNull(psiFile)

        val helpId = provider.getHelpId(psiFile!!)
        assertNull("Help ID should be null", helpId)
    }

    // ========== getWordsScanner Tests ==========

    fun testWordsScannerReturnsScanner() {
        val scanner = provider.wordsScanner
        assertNotNull("Should return a words scanner", scanner)
    }
}

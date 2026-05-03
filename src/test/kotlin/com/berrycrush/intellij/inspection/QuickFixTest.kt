package com.berrycrush.intellij.inspection

import com.berrycrush.intellij.BerryCrushTestCase

/**
 * Tests for BerryCrush Quick Fixes.
 * Verifies quick fix creation and properties.
 */
class QuickFixTest : BerryCrushTestCase() {

    // ========== CreateFragmentQuickFix Tests ==========

    fun testCreateFragmentQuickFixName() {
        val quickFix = CreateFragmentQuickFix("my-fragment")
        assertEquals(
            "Create fragment 'my-fragment'...",
            quickFix.name
        )
    }

    fun testCreateFragmentQuickFixFamilyName() {
        val quickFix = CreateFragmentQuickFix("test")
        assertEquals("BerryCrush", quickFix.familyName)
    }

    fun testCreateFragmentQuickFixNotInWriteAction() {
        val quickFix = CreateFragmentQuickFix("test")
        // Fragment quick fix shows a dialog, so should NOT start in write action
        assertFalse(
            "Should not start in write action (shows dialog)",
            quickFix.startInWriteAction()
        )
    }

    // ========== CreateStepQuickFix Tests ==========

    fun testCreateStepQuickFixName() {
        val quickFix = CreateStepQuickFix("user logs in")
        // Actual implementation copies to clipboard
        assertEquals(
            "Copy @Step method template to clipboard",
            quickFix.name
        )
    }

    fun testCreateStepQuickFixFamilyName() {
        val quickFix = CreateStepQuickFix("test step")
        assertEquals("BerryCrush", quickFix.familyName)
    }

    fun testCreateStepQuickFixStartsInWriteAction() {
        val quickFix = CreateStepQuickFix("test")
        // Step quick fix copies to clipboard, uses default behavior
        // Default is true for LocalQuickFix
        assertTrue(
            "Clipboard quick fix can start in write action",
            quickFix.startInWriteAction()
        )
    }

    // ========== CreateAssertionQuickFix Tests ==========

    fun testCreateAssertionQuickFixName() {
        val quickFix = CreateAssertionQuickFix("response.status == 200")
        assertEquals(
            "Copy @Assertion method template to clipboard",
            quickFix.name
        )
    }

    fun testCreateAssertionQuickFixFamilyName() {
        val quickFix = CreateAssertionQuickFix("test")
        assertEquals("BerryCrush", quickFix.familyName)
    }

    fun testCreateAssertionQuickFixStartsInWriteAction() {
        val quickFix = CreateAssertionQuickFix("test")
        // Assertion quick fix copies to clipboard, uses default behavior
        assertTrue(
            "Clipboard quick fix can start in write action",
            quickFix.startInWriteAction()
        )
    }

    // ========== Quick Fix Uniqueness Tests ==========

    fun testDifferentFragmentNamesCreateDifferentQuickFixes() {
        val fix1 = CreateFragmentQuickFix("fragment-one")
        val fix2 = CreateFragmentQuickFix("fragment-two")

        // Different fragment names produce different quick fix names
        assertFalse("Names should differ", fix1.name == fix2.name)
    }

    fun testStepQuickFixNameIsConstant() {
        val fix1 = CreateStepQuickFix("step one")
        val fix2 = CreateStepQuickFix("step two")

        // Step quick fix name is constant (clipboard operation)
        assertEquals("Names should be the same", fix1.name, fix2.name)
    }

    fun testAssertionQuickFixNameIsConstant() {
        val fix1 = CreateAssertionQuickFix("condition one")
        val fix2 = CreateAssertionQuickFix("condition two")

        // Assertion quick fix name is constant (clipboard operation)
        assertEquals("Names should be the same", fix1.name, fix2.name)
    }

    // ========== LocalQuickFix Interface Tests ==========

    fun testQuickFixesImplementLocalQuickFix() {
        val fragmentFix = CreateFragmentQuickFix("test")
        val stepFix = CreateStepQuickFix("test")
        val assertionFix = CreateAssertionQuickFix("test")

        assertTrue(
            "CreateFragmentQuickFix should be LocalQuickFix",
            fragmentFix is com.intellij.codeInspection.LocalQuickFix
        )
        assertTrue(
            "CreateStepQuickFix should be LocalQuickFix",
            stepFix is com.intellij.codeInspection.LocalQuickFix
        )
        assertTrue(
            "CreateAssertionQuickFix should be LocalQuickFix",
            assertionFix is com.intellij.codeInspection.LocalQuickFix
        )
    }
}

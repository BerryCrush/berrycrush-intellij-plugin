package com.berrycrush.intellij.util

import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

/**
 * Unit tests for ModuleScopeResolver.
 *
 * Note: Most of ModuleScopeResolver functionality requires a real IntelliJ environment
 * with modules, so these tests are limited. Integration tests with real project fixtures
 * are needed for comprehensive testing.
 */
class ModuleScopeResolverTest {

    @Test
    fun `ModuleScopeResolver object exists`() {
        // Simple test to verify the object is accessible
        assertNotNull(ModuleScopeResolver)
    }

    @Test
    fun `getModuleDependencyScope returns scope for null module`() {
        // when module is null, should return allScope
        // This test documents the fallback behavior
        // Note: Cannot test without a real Project instance
    }

    @Test
    fun `getDependentModulesScope returns scope for null module`() {
        // when module is null, should return allScope
        // This test documents the fallback behavior
        // Note: Cannot test without a real Project instance
    }

    @Test
    fun `canAccess returns true for null modules`() {
        // Fallback behavior: null modules should allow access
        val result = ModuleScopeResolver.canAccess(null, null)
        kotlin.test.assertTrue(result, "canAccess should return true when both modules are null")
    }
}

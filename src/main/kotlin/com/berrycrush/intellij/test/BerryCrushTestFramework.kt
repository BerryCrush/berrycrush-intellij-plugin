package com.berrycrush.intellij.test

import com.berrycrush.intellij.BerryCrushIcons
import com.intellij.execution.junit.JUnit5Framework
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import javax.swing.Icon

/**
 * Test framework integration for BerryCrush scenarios.
 *
 * This allows IntelliJ's native JUnit runner to recognize BerryCrush test classes
 * and run them via JUnit Platform with proper navigation support.
 *
 * ## Recognized Test Classes
 *
 * - Classes annotated with `@BerryCrushScenarios`
 * - Classes annotated with `@BerryCrushSpec`
 *
 * ## How It Works
 *
 * By extending `JUnit5Framework`, we leverage IntelliJ's existing JUnit 5 infrastructure:
 * 1. Gutter icons appear on recognized test classes
 * 2. Clicking the gutter icon creates a JUnit run configuration
 * 3. IntelliJ forks a test JVM with `JUnit5IdeaTestRunner`
 * 4. The runner uses `JUnit5TestExecutionListener` which handles `FileSource`
 * 5. Double-clicking test results navigates to scenario files via `file://` location hints
 *
 * This provides build-tool agnostic test execution with proper navigation support.
 */
class BerryCrushTestFramework : JUnit5Framework() {

    companion object {
        private const val BERRYCRUSH_SCENARIOS = "org.berrycrush.junit.BerryCrushScenarios"
        private const val BERRYCRUSH_SPEC = "org.berrycrush.junit.BerryCrushSpec"
        private const val SCENARIO_TEST = "org.berrycrush.junit.ScenarioTest"
    }

    override fun getName(): String = "BerryCrush"

    override fun getIcon(): Icon = BerryCrushIcons.SCENARIO_FILE

    override fun isTestClass(clazz: PsiClass, canBePotential: Boolean): Boolean {
        // Check for @BerryCrushScenarios annotation
        if (clazz.hasAnnotation(BERRYCRUSH_SCENARIOS)) {
            return true
        }
        // Check for @BerryCrushSpec annotation
        if (clazz.hasAnnotation(BERRYCRUSH_SPEC)) {
            return true
        }
        return false
    }

    override fun isTestMethod(element: PsiElement?): Boolean {
        val method = element as? PsiMethod ?: return false

        // Check for @ScenarioTest annotation
        if (method.hasAnnotation(SCENARIO_TEST)) {
            return true
        }

        // Also check parent class for @BerryCrushSpec with @ScenarioTest methods
        val containingClass = method.containingClass ?: return false
        if (!containingClass.hasAnnotation(BERRYCRUSH_SPEC)) {
            return false
        }
        return method.hasAnnotation(SCENARIO_TEST)
    }
}

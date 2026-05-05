package com.berrycrush.intellij.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.junit.JUnitConfiguration
import com.intellij.execution.junit.JUnitConfigurationType
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil

/**
 * Produces BerryCrush run configurations for BerryCrush test classes.
 *
 * This producer creates BerryCrush configurations (which have file:// navigation support)
 * instead of plain JUnit configurations when the context is a BerryCrush test class
 * (annotated with @BerryCrushScenarios or @BerryCrushSpec).
 *
 * Order is important: this producer must be ordered before JUnit's TestInClassConfigurationProducer
 * to ensure BerryCrush configs take precedence.
 */
class BerryCrushConfigurationProducer : LazyRunConfigurationProducer<BerryCrushRunConfiguration>(), DumbAware {

    companion object {
        private val BERRYCRUSH_ANNOTATIONS = setOf(
            "org.berrycrush.junit.BerryCrushScenarios",
            "org.berrycrush.junit.BerryCrushSpec",
            "org.berrycrush.junit.BerryCrushConfiguration",
            "BerryCrushScenarios",
            "BerryCrushSpec",
            "BerryCrushConfiguration"
        )
    }

    override fun getConfigurationFactory(): ConfigurationFactory {
        return BerryCrushConfigurationType().configurationFactories[0]
    }

    override fun setupConfigurationFromContext(
        configuration: BerryCrushRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val psiElement = context.psiLocation ?: return false
        val testClass = findBerryCrushTestClass(psiElement) ?: return false

        // Get module
        val module = ModuleUtilCore.findModuleForPsiElement(psiElement) ?: return false
        configuration.setModule(module)

        // Check if we're on a specific test method
        val testMethod = findTestMethod(psiElement)

        if (testMethod != null) {
            // Configure for specific method
            configuration.persistentData.TEST_OBJECT = JUnitConfiguration.TEST_METHOD
            configuration.persistentData.METHOD_NAME = testMethod.name
            configuration.persistentData.MAIN_CLASS_NAME = testClass.qualifiedName
            configuration.name = "BerryCrush: ${testClass.name}.${testMethod.name}"
            sourceElement.set(testMethod)
        } else {
            // Configure for entire class
            configuration.persistentData.TEST_OBJECT = JUnitConfiguration.TEST_CLASS
            configuration.persistentData.MAIN_CLASS_NAME = testClass.qualifiedName
            configuration.name = "BerryCrush: ${testClass.name}"
            sourceElement.set(testClass)
        }

        return true
    }

    override fun isConfigurationFromContext(
        configuration: BerryCrushRunConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val psiElement = context.psiLocation ?: return false
        val testClass = findBerryCrushTestClass(psiElement) ?: return false

        // Check if class matches
        if (configuration.persistentData.MAIN_CLASS_NAME != testClass.qualifiedName) {
            return false
        }

        // Check method match if configured for a method
        val testMethod = findTestMethod(psiElement)
        val configuredMethod = configuration.persistentData.METHOD_NAME

        return when {
            testMethod != null && configuredMethod != null -> testMethod.name == configuredMethod
            testMethod == null && configuredMethod.isNullOrEmpty() -> true
            else -> false
        }
    }

    override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext): Boolean {
        // BerryCrush configs should replace plain JUnit configs for BerryCrush test classes
        val otherType = other.configuration.factory?.type ?: return false
        // Replace JUnit configurations but not our own BerryCrush configurations
        return otherType is JUnitConfigurationType && other.configuration !is BerryCrushRunConfiguration
    }

    /**
     * Finds the BerryCrush test class containing the given element.
     */
    private fun findBerryCrushTestClass(element: PsiElement): PsiClass? {
        val psiClass = when (element) {
            is PsiClass -> element
            is PsiMethod -> element.containingClass
            else -> PsiTreeUtil.getParentOfType(element, PsiClass::class.java)
        } ?: return null

        return if (isBerryCrushTestClass(psiClass)) psiClass else null
    }

    /**
     * Finds a test method at the given element position.
     */
    private fun findTestMethod(element: PsiElement): PsiMethod? {
        val method = when (element) {
            is PsiMethod -> element
            else -> PsiTreeUtil.getParentOfType(element, PsiMethod::class.java)
        } ?: return null

        // Check if it's a test method (has @Test, @ParameterizedTest, @ScenarioTest, etc.)
        val testAnnotations = setOf(
            "org.junit.jupiter.api.Test",
            "org.junit.jupiter.params.ParameterizedTest",
            "org.junit.Test",
            "org.berrycrush.junit.ScenarioTest",
            "Test",
            "ParameterizedTest",
            "ScenarioTest"
        )

        val hasTestAnnotation = method.annotations.any { annotation ->
            val qualifiedName = annotation.qualifiedName
            qualifiedName != null && testAnnotations.any { expected ->
                qualifiedName == expected || qualifiedName.endsWith(".$expected")
            }
        }

        return if (hasTestAnnotation) method else null
    }

    private fun isBerryCrushTestClass(psiClass: PsiClass): Boolean {
        return psiClass.annotations.any { annotation ->
            val qualifiedName = annotation.qualifiedName
            qualifiedName != null && BERRYCRUSH_ANNOTATIONS.any { expected ->
                qualifiedName == expected || qualifiedName.endsWith(".$expected")
            }
        }
    }
}

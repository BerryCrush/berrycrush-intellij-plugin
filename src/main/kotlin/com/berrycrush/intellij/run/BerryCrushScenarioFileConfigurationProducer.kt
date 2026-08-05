package com.berrycrush.intellij.run

import com.berrycrush.intellij.language.ScenarioFileType
import com.berrycrush.intellij.util.ModuleScopeResolver
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.junit.JUnitConfigurationType
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement

/**
 * Produces run configurations when a `.scenario` file is selected in editor or project view.
 */
class BerryCrushScenarioFileConfigurationProducer :
    LazyRunConfigurationProducer<BerryCrushRunConfiguration>(),
    DumbAware {
    override fun getConfigurationFactory(): ConfigurationFactory = BerryCrushConfigurationType().configurationFactories[0]

    override fun setupConfigurationFromContext(
        configuration: BerryCrushRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>,
    ): Boolean {
        val scenarioFile = getScenarioFile(context) ?: return false
        val preferredModule = ModuleScopeResolver.findModuleForFile(scenarioFile, context.project)
        val fallbackClass = System.getProperty(BerryCrushScenarioExecutionSupport.DEFAULT_TEST_CLASS_PROPERTY)

        val testClass =
            BerryCrushScenarioExecutionSupport.resolveTestClass(
                project = context.project,
                preferredModule = preferredModule,
                fallbackClassFqn = fallbackClass,
                chooseWhenMultiple = false,
            ) ?: return false

        val effectiveModule = preferredModule ?: BerryCrushScenarioExecutionSupport.resolveModuleForClass(context.project, testClass)
        val vmOptions = BerryCrushScenarioExecutionSupport.buildVmOptions(scenarioFile.name)

        BerryCrushScenarioExecutionSupport.configureForClassRun(
            configuration = configuration,
            testClass = testClass,
            module = effectiveModule,
            configName = "BerryCrush: ${scenarioFile.name}",
            vmOptions = vmOptions,
        )

        sourceElement.set(context.psiLocation ?: context.location?.psiElement)
        return true
    }

    override fun isConfigurationFromContext(
        configuration: BerryCrushRunConfiguration,
        context: ConfigurationContext,
    ): Boolean {
        val scenarioFile = getScenarioFile(context) ?: return false
        val expectedName = "BerryCrush: ${scenarioFile.name}"
        val expectedVmOption = BerryCrushScenarioExecutionSupport.buildVmOption("berryCrush.scenarioFile", scenarioFile.name)

        return configuration.name == expectedName && configuration.vmParameters.orEmpty().contains(expectedVmOption)
    }

    override fun shouldReplace(
        self: ConfigurationFromContext,
        other: ConfigurationFromContext,
    ): Boolean {
        val otherType = other.configuration.factory?.type ?: return false
        return otherType is JUnitConfigurationType && other.configuration !is BerryCrushRunConfiguration
    }

    override fun onFirstRun(
        configuration: ConfigurationFromContext,
        context: ConfigurationContext,
        startRunnable: Runnable,
    ) {
        val project = context.project
        val scenarioFile = getScenarioFile(context)
        val runConfiguration = configuration.configuration as? BerryCrushRunConfiguration

        if (scenarioFile == null || runConfiguration == null) {
            startRunnable.run()
            return
        }

        val candidates = BerryCrushScenarioExecutionSupport.findBerryCrushTestClasses(project)
        if (candidates.size <= 1) {
            startRunnable.run()
            return
        }

        val preferredModule = ModuleScopeResolver.findModuleForFile(scenarioFile, project)
        BerryCrushScenarioExecutionSupport.showTestClassChooser(
            project = project,
            candidates = candidates,
            preferredModule = preferredModule,
            onSelected = { selectedClass ->
                val module = preferredModule ?: BerryCrushScenarioExecutionSupport.resolveModuleForClass(project, selectedClass)
                BerryCrushScenarioExecutionSupport.configureForClassRun(
                    configuration = runConfiguration,
                    testClass = selectedClass,
                    module = module,
                    configName = "BerryCrush: ${scenarioFile.name}",
                    vmOptions = BerryCrushScenarioExecutionSupport.buildVmOptions(scenarioFile.name),
                )

                startRunnable.run()
            },
        )
    }

    private fun getScenarioFile(context: ConfigurationContext): VirtualFile? {
        val psiFile = CommonDataKeys.PSI_FILE.getData(context.dataContext)?.virtualFile
        val candidate =
            context.location?.virtualFile
                ?: CommonDataKeys.VIRTUAL_FILE.getData(context.dataContext)
                ?: psiFile
                ?: context.psiLocation?.containingFile?.virtualFile

        return candidate?.takeIf { it.extension == ScenarioFileType.EXTENSION }
    }
}

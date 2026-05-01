package com.berrycrush.intellij.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import java.io.File

/**
 * Run configuration for executing BerryCrush scenario tests.
 *
 * This configuration stores:
 * - Path to the .scenario file
 * - Optional scenario name filter
 * - Optional feature name filter
 * - Module for classpath resolution
 */
class BerryCrushRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String,
) : RunConfigurationBase<BerryCrushRunConfigurationOptions>(project, factory, name) {

    override fun getOptions(): BerryCrushRunConfigurationOptions {
        return super.getOptions() as BerryCrushRunConfigurationOptions
    }

    /**
     * Path to the .scenario file to execute.
     */
    var scenarioFilePath: String
        get() = options.scenarioFilePath
        set(value) {
            options.scenarioFilePath = value
        }

    /**
     * Optional: Specific scenario name to run.
     */
    var scenarioName: String?
        get() = options.scenarioName
        set(value) {
            options.scenarioName = value
        }

    /**
     * Optional: Specific feature name to run.
     */
    var featureName: String?
        get() = options.featureName
        set(value) {
            options.featureName = value
        }

    /**
     * Module name for classpath configuration.
     */
    var moduleName: String?
        get() = options.moduleName
        set(value) {
            options.moduleName = value
        }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return BerryCrushRunConfigurationEditor(project)
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return BerryCrushRunState(this, environment)
    }

    @Throws(RuntimeConfigurationException::class)
    override fun checkConfiguration() {
        if (scenarioFilePath.isEmpty()) {
            throw RuntimeConfigurationError("No scenario file specified")
        }

        val file = File(scenarioFilePath)
        if (!file.exists()) {
            throw RuntimeConfigurationError("Scenario file not found: $scenarioFilePath")
        }

        if (!file.extension.equals("scenario", ignoreCase = true)) {
            throw RuntimeConfigurationError("File must have .scenario extension: $scenarioFilePath")
        }
    }
}

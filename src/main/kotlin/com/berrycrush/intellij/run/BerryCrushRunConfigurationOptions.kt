package com.berrycrush.intellij.run

import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.openapi.components.StoredProperty

/**
 * Persistent options for BerryCrush run configurations.
 *
 * These options are automatically persisted with the run configuration
 * in the project's .idea/runConfigurations directory.
 */
class BerryCrushRunConfigurationOptions : RunConfigurationOptions() {

    /**
     * Path to the .scenario file to execute.
     */
    private val scenarioFilePathOption: StoredProperty<String?> =
        string("").provideDelegate(this, "scenarioFilePath")

    var scenarioFilePath: String
        get() = scenarioFilePathOption.getValue(this) ?: ""
        set(value) = scenarioFilePathOption.setValue(this, value)

    /**
     * Optional: Specific scenario name to run within the file.
     * If null/empty, all scenarios in the file are run.
     */
    private val scenarioNameOption: StoredProperty<String?> =
        string("").provideDelegate(this, "scenarioName")

    var scenarioName: String?
        get() = scenarioNameOption.getValue(this)?.takeIf { it.isNotEmpty() }
        set(value) = scenarioNameOption.setValue(this, value ?: "")

    /**
     * Optional: Specific feature name to run within the file.
     * If null/empty, all features are run.
     */
    private val featureNameOption: StoredProperty<String?> =
        string("").provideDelegate(this, "featureName")

    var featureName: String?
        get() = featureNameOption.getValue(this)?.takeIf { it.isNotEmpty() }
        set(value) = featureNameOption.setValue(this, value ?: "")

    /**
     * Module name for classpath configuration.
     */
    private val moduleNameOption: StoredProperty<String?> =
        string("").provideDelegate(this, "moduleName")

    var moduleName: String?
        get() = moduleNameOption.getValue(this)?.takeIf { it.isNotEmpty() }
        set(value) = moduleNameOption.setValue(this, value ?: "")
}

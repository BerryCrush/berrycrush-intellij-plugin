package com.berrycrush.intellij.run

import com.berrycrush.intellij.BerryCrushIcons
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.junit.JUnitConfiguration
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.openapi.project.Project
import javax.swing.Icon

/**
 * BerryCrush run configuration type.
 *
 * This is a minimal wrapper around JUnit that:
 * 1. Delegates all execution to JUnit
 * 2. Provides custom console properties with file:// URL navigation support
 *
 * The key difference from plain JUnit is that BerryCrush tests use FileSource
 * which generates file:// location hints. JUnit's default JavaTestLocator only
 * handles java:// URLs. This configuration adds FileUrlProvider support.
 */
class BerryCrushConfigurationType : ConfigurationType {
    override fun getDisplayName(): String = "BerryCrush"
    override fun getConfigurationTypeDescription(): String = "BerryCrush test configuration"
    override fun getIcon(): Icon = BerryCrushIcons.SCENARIO_FILE
    override fun getId(): String = "BerryCrushConfiguration"

    override fun getConfigurationFactories(): Array<ConfigurationFactory> =
        arrayOf(BerryCrushConfigurationFactory(this))
}

/**
 * Factory for creating BerryCrush run configurations.
 */
class BerryCrushConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    override fun getId(): String = "BerryCrush"

    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        BerryCrushRunConfiguration(project, this, "BerryCrush")

    override fun getName(): String = "BerryCrush"
}

/**
 * BerryCrush run configuration that extends JUnitConfiguration.
 *
 * Inherits all JUnit functionality and only overrides console properties
 * to support file:// URL navigation for .scenario files.
 */
class BerryCrushRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String,
) : JUnitConfiguration(name, project, factory) {

    override fun createTestConsoleProperties(executor: Executor): SMTRunnerConsoleProperties {
        return BerryCrushConsoleProperties(this, executor)
    }
}

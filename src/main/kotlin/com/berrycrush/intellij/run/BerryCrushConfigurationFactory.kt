package com.berrycrush.intellij.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project

/**
 * Factory for creating BerryCrush run configurations.
 */
class BerryCrushConfigurationFactory(
    type: ConfigurationType,
) : ConfigurationFactory(type) {

    override fun getId(): String = BerryCrushConfigurationType.ID

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return BerryCrushRunConfiguration(project, this, "Unnamed")
    }

    override fun getOptionsClass(): Class<out BaseState> {
        return BerryCrushRunConfigurationOptions::class.java
    }
}

package com.berrycrush.intellij.run

import com.berrycrush.intellij.BerryCrushIcons
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.util.NotNullLazyValue

/**
 * Configuration type for BerryCrush scenario tests.
 *
 * This registers "BerryCrush Scenario" as a run configuration type in IntelliJ,
 * appearing in the Run/Debug Configurations dialog alongside JUnit, Gradle, etc.
 */
class BerryCrushConfigurationType : ConfigurationTypeBase(
    ID,
    DISPLAY_NAME,
    DESCRIPTION,
    NotNullLazyValue.createValue { BerryCrushIcons.RUN_CONFIGURATION },
) {
    init {
        addFactory(BerryCrushConfigurationFactory(this))
    }

    companion object {
        const val ID = "BerryCrushRunConfiguration"
        const val DISPLAY_NAME = "BerryCrush Scenario"
        const val DESCRIPTION = "Run BerryCrush scenario tests"

        fun getInstance(): BerryCrushConfigurationType =
            ConfigurationTypeUtil.findConfigurationType(BerryCrushConfigurationType::class.java)
    }
}

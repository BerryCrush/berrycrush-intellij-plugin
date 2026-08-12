package org.berrycrush.intellij

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/**
 * Icon resources for the BerryCrush plugin.
 */
object BerryCrushIcons {
    private fun load(path: String): Icon = IconLoader.getIcon(path, BerryCrushIcons::class.java)

    @JvmField
    val SCENARIO_FILE: Icon = load("/icons/scenario.svg")

    @JvmField
    val FRAGMENT_FILE: Icon = load("/icons/fragment.svg")

    @JvmField
    val FEATURE: Icon = load("/icons/feature.svg")

    @JvmField
    val SCENARIO: Icon = load("/icons/scenario-item.svg")

    @JvmField
    val FRAGMENT: Icon = load("/icons/fragment-item.svg")

    @JvmField
    val STEP: Icon = AllIcons.General.InspectionsOK

    @JvmField
    val RUN_CONFIGURATION: Icon = load("/icons/run-configuration.svg")

    // Structure view icons - reuse existing icons as fallbacks
    @JvmField
    val OPERATION: Icon = AllIcons.Webreferences.Openapi

    @JvmField
    val ASSERTION: Icon = AllIcons.General.InspectionsOK

    @JvmField
    val VARIABLE: Icon = STEP // Extract directives

    @JvmField
    val INCLUDE = FRAGMENT

    @JvmField
    val CONDITIONAL = AllIcons.Debugger.Question_badge

    @JvmField
    val EXTRACT = AllIcons.Vcs.Arrow_right

    @JvmField
    val PARAMETER = AllIcons.General.ProjectConfigurable
}

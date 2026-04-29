package com.berrycrush.intellij

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
    val STEP: Icon = load("/icons/step.svg")
}

package com.berrycrush.intellij.language

import com.berrycrush.intellij.BerryCrushIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

/**
 * File type for BerryCrush scenario files (.scenario).
 */
object ScenarioFileType : LanguageFileType(BerryCrushLanguage) {
    const val EXTENSION = "scenario"

    override fun getName(): String = "BerryCrush Scenario"
    override fun getDisplayName(): String = "BerryCrush Scenario"
    override fun getDescription(): String = "BerryCrush test scenario file"
    override fun getDefaultExtension(): String = EXTENSION
    override fun getIcon(): Icon = BerryCrushIcons.SCENARIO_FILE
}

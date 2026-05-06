package com.berrycrush.intellij.codestyle

import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CustomCodeStyleSettings

/**
 * Custom code style settings for BerryCrush language.
 * 
 * Note: Indentation settings are handled by IntelliJ's standard IndentOptions
 * (configured in BerryCrushLanguageCodeStyleSettingsProvider).
 * 
 * IntelliJ's CustomCodeStyleSettings requires @JvmField variables
 * with UPPER_SNAKE_CASE naming for persistence to work correctly.
 */
@Suppress("VariableNaming")
class BerryCrushCodeStyleSettings(container: CodeStyleSettings) :
    CustomCodeStyleSettings("BerryCrushCodeStyleSettings", container) {

    /**
     * Whether to align table columns in examples blocks.
     */
    @JvmField
    var ALIGN_TABLE_COLUMNS: Boolean = true

    /**
     * Whether to align parameter values.
     */
    @JvmField
    var ALIGN_PARAMETERS: Boolean = true

    /**
     * Whether to right-align numeric values in tables.
     */
    @JvmField
    var RIGHT_ALIGN_NUMBERS: Boolean = true
}

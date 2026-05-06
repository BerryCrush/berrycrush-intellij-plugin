package com.berrycrush.intellij.codestyle

import com.berrycrush.intellij.language.BerryCrushLanguage
import com.intellij.application.options.IndentOptionsEditor
import com.intellij.lang.Language
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizable
import com.intellij.psi.codeStyle.CodeStyleSettingsCustomizableOptions
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider

/**
 * Provides code style settings for BerryCrush language.
 * Enables the "BerryCrush" tab in Settings → Code Style.
 */
class BerryCrushLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {

    override fun getLanguage(): Language = BerryCrushLanguage

    override fun getCodeSample(settingsType: SettingsType): String {
        return when (settingsType) {
            SettingsType.INDENT_SETTINGS -> INDENT_SAMPLE
            SettingsType.SPACING_SETTINGS -> SPACING_SAMPLE
            else -> GENERAL_SAMPLE
        }
    }

    override fun customizeSettings(consumer: CodeStyleSettingsCustomizable, settingsType: SettingsType) {
        when (settingsType) {
            SettingsType.INDENT_SETTINGS -> {
                consumer.showStandardOptions(
                    CodeStyleSettingsCustomizable.IndentOption.INDENT_SIZE.name,
                    CodeStyleSettingsCustomizable.IndentOption.TAB_SIZE.name,
                    CodeStyleSettingsCustomizable.IndentOption.USE_TAB_CHARACTER.name
                )
            }
            SettingsType.SPACING_SETTINGS -> {
                consumer.showStandardOptions(
                    CodeStyleSettingsCustomizable.SpacingOption.SPACE_AROUND_ASSIGNMENT_OPERATORS.name
                )
                consumer.showCustomOption(
                    BerryCrushCodeStyleSettings::class.java,
                    "ALIGN_TABLE_COLUMNS",
                    "Align table columns",
                    CodeStyleSettingsCustomizableOptions.getInstance().SPACES_OTHER
                )
                consumer.showCustomOption(
                    BerryCrushCodeStyleSettings::class.java,
                    "ALIGN_PARAMETERS",
                    "Align parameter values",
                    CodeStyleSettingsCustomizableOptions.getInstance().SPACES_OTHER
                )
                consumer.showCustomOption(
                    BerryCrushCodeStyleSettings::class.java,
                    "RIGHT_ALIGN_NUMBERS",
                    "Right-align numeric values in tables",
                    CodeStyleSettingsCustomizableOptions.getInstance().SPACES_OTHER
                )
            }
            else -> {}
        }
    }

    override fun getIndentOptionsEditor(): IndentOptionsEditor = IndentOptionsEditor()

    override fun customizeDefaults(
        commonSettings: CommonCodeStyleSettings,
        indentOptions: CommonCodeStyleSettings.IndentOptions
    ) {
        indentOptions.INDENT_SIZE = 2
        indentOptions.TAB_SIZE = 2
        indentOptions.USE_TAB_CHARACTER = false
        indentOptions.CONTINUATION_INDENT_SIZE = 2
    }

    companion object {
        private val INDENT_SAMPLE = """
            feature: Pet Store API
              background:
                given authenticated user
                  include auth
              
              scenario: List all pets
                when listing pets
                  call ^listPets
                then pets are returned
                  assert status 200
                  assert $.data notEmpty
        """.trimIndent()

        private val SPACING_SAMPLE = """
            scenario: Test spacing
              when making a call
                call ^operation
                  query: value
                  header: value
              then verify response
                assert status 200
                assert $.field = "value"
              examples:
                | name   | value |
                | first  | 1     |
                | second | 2     |
        """.trimIndent()

        private val GENERAL_SAMPLE = """
            @api @smoke feature: Pet Store API Tests
              # This is a comment
              background:
                given authenticated user
                  include auth
              
              scenario: List all pets
                when listing pets
                  call ^listPets
                then pets are returned
                  assert status 200
                  assert $.data notEmpty
                  extract $.data[0].id => firstPetId
            
            @critical scenario: Get pet by ID
              given I have a pet ID
                call using petstore ^listPets
                extract $.data[0].id => petId
              when requesting pet
                call ^getPetById
                  petId: {{petId}}
              then pet details returned
                assert status 200
                assert $.name exists
            
            outline: Create pet with status
              when creating a pet
                call ^createPet
                  body:
                    '''
                    {
                      "name": "{{name}}",
                      "status": "{{status}}"
                    }
                    '''
              then pet is created
                assert status 201
              examples:
                | name   | status    |
                | Fluffy | available |
                | Max    | pending   |
        """.trimIndent()
    }
}

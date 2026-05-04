package com.berrycrush.intellij.highlighting

import com.berrycrush.intellij.BerryCrushIcons
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import javax.swing.Icon

/**
 * Color settings page for BerryCrush syntax highlighting customization.
 */
class BerryCrushColorSettingsPage : ColorSettingsPage {

    override fun getIcon(): Icon = BerryCrushIcons.SCENARIO_FILE

    override fun getHighlighter(): SyntaxHighlighter = BerryCrushSyntaxHighlighter()

    override fun getDemoText(): String = """
# BerryCrush Scenario Demo
@smoke @api
feature: Pet Store API
  background:
    given the API is running

  scenario: list all pets
    when I request the list of pets
      call ^listPets
      assert status 200
      assert $.length > 0
      extract petId = $.items[0].id
    then the response is valid
      assert schema
      assert header Content-Type contains "application/json"

  scenario: create pet with parameters
    when I create a custom pet
      include create_pet
        name: "Fluffy"
        status: "available"
        price: 29.99
    then the pet is created

  outline: create pet with name
    when I create a pet named "{{name}}"
      call ^createPet
        body:
          name: {{name}}
      assert status 201
      if $.id exists
        extract newPetId = $.id
      else
        fail "Pet creation failed"

  examples:
    | name   |
    | Fluffy |
    | Max    |

fragment: common-auth
  given I am authenticated
    call ^authenticate
      body:
        username: "admin"
        password: "secret123"
    assert status 200
    extract token = $.accessToken

fragment: create_pet
  when creating the pet
    call ^createPet
      body: {"name": "{{name}}", "status": "{{status}}"}
    assert status 201
"""

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = DESCRIPTORS

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDisplayName(): String = "BerryCrush"

    companion object {
        private val DESCRIPTORS = arrayOf(
            AttributesDescriptor("Block keywords", BerryCrushHighlightingColors.BLOCK_KEYWORD),
            AttributesDescriptor("Step keywords", BerryCrushHighlightingColors.STEP_KEYWORD),
            AttributesDescriptor("Directives", BerryCrushHighlightingColors.DIRECTIVE),
            AttributesDescriptor("Assertion keywords", BerryCrushHighlightingColors.ASSERTION_KEYWORD),
            AttributesDescriptor("Operators", BerryCrushHighlightingColors.OPERATOR),
            AttributesDescriptor("Tags", BerryCrushHighlightingColors.TAG),
            AttributesDescriptor("Operation references", BerryCrushHighlightingColors.OPERATION_REF),
            AttributesDescriptor("Variables", BerryCrushHighlightingColors.VARIABLE),
            AttributesDescriptor("JSON path", BerryCrushHighlightingColors.JSON_PATH),
            AttributesDescriptor("Strings", BerryCrushHighlightingColors.STRING),
            AttributesDescriptor("Numbers", BerryCrushHighlightingColors.NUMBER),
            AttributesDescriptor("Comments", BerryCrushHighlightingColors.COMMENT),
            AttributesDescriptor("Braces", BerryCrushHighlightingColors.BRACES),
            AttributesDescriptor("Pipe", BerryCrushHighlightingColors.PIPE),
            AttributesDescriptor("Parameter keys", BerryCrushHighlightingColors.PARAMETER_KEY),
            AttributesDescriptor("Bad character", BerryCrushHighlightingColors.BAD_CHARACTER),
        )
    }
}

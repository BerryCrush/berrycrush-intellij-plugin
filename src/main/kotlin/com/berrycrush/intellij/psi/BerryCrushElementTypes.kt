package com.berrycrush.intellij.psi

import com.berrycrush.intellij.language.BerryCrushLanguage
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType

/**
 * PSI element types for BerryCrush language.
 */
object BerryCrushElementTypes {
    // File element
    @JvmField
    val FILE = IFileElementType("BERRYCRUSH_FILE", BerryCrushLanguage)

    // Block elements
    @JvmField
    val FEATURE = BerryCrushPsiElementType("FEATURE")

    @JvmField
    val SCENARIO = BerryCrushPsiElementType("SCENARIO")

    @JvmField
    val OUTLINE = BerryCrushPsiElementType("OUTLINE")

    @JvmField
    val FRAGMENT = BerryCrushPsiElementType("FRAGMENT")

    @JvmField
    val BLOCK_NAME = BerryCrushPsiElementType("BLOCK_NAME")

    @JvmField
    val BACKGROUND = BerryCrushPsiElementType("BACKGROUND")

    @JvmField
    val EXAMPLES = BerryCrushPsiElementType("EXAMPLES")

    @JvmField
    val EXAMPLE_ROW = BerryCrushPsiElementType("EXAMPLE_ROW")

    @JvmField
    val EXAMPLES_HEADER = BerryCrushPsiElementType("EXAMPLES_HEADER")

    @JvmField
    val EXAMPLES_VALUE = BerryCrushPsiElementType("EXAMPLES_VALUE")

    // Step elements
    @JvmField
    val STEP = BerryCrushPsiElementType("STEP")

    // Directive elements
    @JvmField
    val CALL_DIRECTIVE = BerryCrushPsiElementType("CALL_DIRECTIVE")

    @JvmField
    val WEBHOOK_DIRECTIVE = BerryCrushPsiElementType("WEBHOOK_DIRECTIVE")

    @JvmField
    val ASSERT_DIRECTIVE = BerryCrushPsiElementType("ASSERT_DIRECTIVE")

    @JvmField
    val EXTRACT_DIRECTIVE = BerryCrushPsiElementType("EXTRACT_DIRECTIVE")

    @JvmField
    val INCLUDE_DIRECTIVE = BerryCrushPsiElementType("INCLUDE_DIRECTIVE")

    @JvmField
    val IF_DIRECTIVE = BerryCrushPsiElementType("IF_DIRECTIVE")

    @JvmField
    val CONDITION = BerryCrushPsiElementType("CONDITION")

    @JvmField
    val ELSE_DIRECTIVE = BerryCrushPsiElementType("ELSE_DIRECTIVE")

    @JvmField
    val BODY_DIRECTIVE = BerryCrushPsiElementType("BODY_DIRECTIVE")

    // Reference elements
    @JvmField
    val OPERATION_REF = BerryCrushPsiElementType("OPERATION_REF")

    @JvmField
    val FRAGMENT_REF = BerryCrushPsiElementType("FRAGMENT_REF")

    @JvmField
    val VARIABLE_REF = BerryCrushPsiElementType("VARIABLE_REF")

    @JvmField
    val STRING_LITERAL = BerryCrushPsiElementType("STRING_LITERAL")

    // assert elements
    @JvmField
    val JSON_PATH = BerryCrushPsiElementType("JSON_PATH")

    @JvmField
    val ASSERTION_OPERATION = BerryCrushPsiElementType("ASSERTION_OPERATION")

    @JvmField
    val WEBHOOK_NAME = BerryCrushPsiElementType("WEBHOOK_NAME")

    // Other elements
    @JvmField
    val TAG = BerryCrushPsiElementType("TAG")

    @JvmField
    val PARAMETERS = BerryCrushPsiElementType("PARAMETERS")

    @JvmField
    val INCLUDED_PARAMETER = BerryCrushPsiElementType("INCLUDED_PARAMETER")

    @JvmField
    val PARAMETER_ENTRY = BerryCrushPsiElementType("PARAMETER_ENTRY")

    @JvmField
    val PARAMETER_KEY = BerryCrushPsiElementType("PARAMETER_KEY")

    @JvmField
    val PARAMETER_VALUE = BerryCrushPsiElementType("PARAMETER_VALUE")

    @JvmField
    val TEXT = BerryCrushPsiElementType("TEXT")

    @JvmField
    val NOT = BerryCrushPsiElementType("NOT")

    @JvmField
    val OPERATOR = BerryCrushPsiElementType("OPERATOR")

    @JvmField
    val ARROW = BerryCrushPsiElementType("ARROW")
}

/**
 * Custom element type for BerryCrush PSI elements.
 */
class BerryCrushPsiElementType(
    debugName: String,
) : IElementType(debugName, BerryCrushLanguage)

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
    val BACKGROUND = BerryCrushPsiElementType("BACKGROUND")

    @JvmField
    val EXAMPLES = BerryCrushPsiElementType("EXAMPLES")

    // Step elements
    @JvmField
    val STEP = BerryCrushPsiElementType("STEP")

    // Directive elements
    @JvmField
    val CALL_DIRECTIVE = BerryCrushPsiElementType("CALL_DIRECTIVE")

    @JvmField
    val ASSERT_DIRECTIVE = BerryCrushPsiElementType("ASSERT_DIRECTIVE")

    @JvmField
    val EXTRACT_DIRECTIVE = BerryCrushPsiElementType("EXTRACT_DIRECTIVE")

    @JvmField
    val INCLUDE_DIRECTIVE = BerryCrushPsiElementType("INCLUDE_DIRECTIVE")

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
    val JSON_PATH_REF = BerryCrushPsiElementType("JSON_PATH_REF")

    // Other elements
    @JvmField
    val TAG = BerryCrushPsiElementType("TAG")

    @JvmField
    val PARAMETERS = BerryCrushPsiElementType("PARAMETERS")

    @JvmField
    val PARAMETER = BerryCrushPsiElementType("PARAMETER")
}

/**
 * Custom element type for BerryCrush PSI elements.
 */
class BerryCrushPsiElementType(debugName: String) : IElementType(debugName, BerryCrushLanguage)

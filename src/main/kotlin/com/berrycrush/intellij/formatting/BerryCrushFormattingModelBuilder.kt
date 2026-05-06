package com.berrycrush.intellij.formatting

import com.berrycrush.intellij.language.BerryCrushLanguage
import com.berrycrush.intellij.lexer.BerryCrushTokenTypes
import com.intellij.formatting.FormattingContext
import com.intellij.formatting.FormattingModel
import com.intellij.formatting.FormattingModelBuilder
import com.intellij.formatting.FormattingModelProvider
import com.intellij.formatting.Indent
import com.intellij.formatting.SpacingBuilder
import com.intellij.psi.codeStyle.CodeStyleSettings

/**
 * Formatting model builder for BerryCrush language.
 * Creates the formatting model used by IntelliJ's "Reformat Code" action.
 * 
 * Uses a context-aware block approach where each block determines its
 * indent based on surrounding context in the flat AST structure.
 */
class BerryCrushFormattingModelBuilder : FormattingModelBuilder {

    override fun createModel(formattingContext: FormattingContext): FormattingModel {
        val element = formattingContext.psiElement
        val settings = formattingContext.codeStyleSettings
        
        // Get indent size (default to 2)
        val indentSize = settings.getIndentSize(BerryCrushLanguage.associatedFileType) ?: 2
        
        val spacingBuilder = createSpacingBuilder(settings)
        
        val rootBlock = BerryCrushBlock(
            element.node,
            null,
            null,
            Indent.getNoneIndent(),
            spacingBuilder,
            indentSize
        )
        
        return FormattingModelProvider.createFormattingModelForPsiFile(
            element.containingFile,
            rootBlock,
            settings
        )
    }

    /**
     * Create spacing rules for BerryCrush elements.
     */
    private fun createSpacingBuilder(settings: CodeStyleSettings): SpacingBuilder {
        return SpacingBuilder(settings, BerryCrushLanguage)
            // Space after colon in keywords
            .after(BerryCrushTokenTypes.COLON).spaces(1)
            // No space before colon
            .before(BerryCrushTokenTypes.COLON).spaces(0)
            // Space after step keywords
            .after(BerryCrushTokenTypes.GIVEN).spaces(1)
            .after(BerryCrushTokenTypes.WHEN).spaces(1)
            .after(BerryCrushTokenTypes.THEN).spaces(1)
            .after(BerryCrushTokenTypes.AND).spaces(1)
            .after(BerryCrushTokenTypes.BUT).spaces(1)
            // Space after directives
            .after(BerryCrushTokenTypes.CALL).spaces(1)
            .after(BerryCrushTokenTypes.ASSERT).spaces(1)
            .after(BerryCrushTokenTypes.INCLUDE).spaces(1)
    }
}

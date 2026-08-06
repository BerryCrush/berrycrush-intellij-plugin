package com.berrycrush.intellij.highlighting

import com.berrycrush.intellij.lexer.BerryCrushLexer
import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

/**
 * Syntax highlighter for BerryCrush language.
 */
class BerryCrushSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = BerryCrushLexer()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> = BerryCrushTokenHighlighting.keysForToken(tokenType)
}

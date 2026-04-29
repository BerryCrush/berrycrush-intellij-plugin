package com.berrycrush.intellij.highlighting

import com.berrycrush.intellij.lexer.BerryCrushTokenTypes
import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

/**
 * Brace matcher for BerryCrush language.
 *
 * Matches braces, brackets, and variable delimiters.
 */
class BerryCrushBraceMatcher : PairedBraceMatcher {

    override fun getPairs(): Array<BracePair> = PAIRS

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset

    companion object {
        private val PAIRS = arrayOf(
            BracePair(BerryCrushTokenTypes.LBRACE, BerryCrushTokenTypes.RBRACE, false),
            BracePair(BerryCrushTokenTypes.LBRACKET, BerryCrushTokenTypes.RBRACKET, false),
            BracePair(BerryCrushTokenTypes.LPAREN, BerryCrushTokenTypes.RPAREN, false),
            BracePair(BerryCrushTokenTypes.VARIABLE_START, BerryCrushTokenTypes.VARIABLE_END, false),
        )
    }
}

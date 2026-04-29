package com.berrycrush.intellij.parser

import com.berrycrush.intellij.language.BerryCrushLanguage
import com.berrycrush.intellij.lexer.BerryCrushLexer
import com.berrycrush.intellij.lexer.BerryCrushTokenTypes
import com.berrycrush.intellij.psi.BerryCrushFile
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

/**
 * Parser definition for BerryCrush language.
 */
class BerryCrushParserDefinition : ParserDefinition {

    companion object {
        val FILE = IFileElementType(BerryCrushLanguage)
    }

    override fun createLexer(project: Project?): Lexer = BerryCrushLexer()

    override fun createParser(project: Project?): PsiParser = BerryCrushParser()

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getWhitespaceTokens(): TokenSet = BerryCrushTokenTypes.WHITESPACES

    override fun getCommentTokens(): TokenSet = BerryCrushTokenTypes.COMMENTS

    override fun getStringLiteralElements(): TokenSet = TokenSet.create(BerryCrushTokenTypes.STRING)

    override fun createElement(node: ASTNode): PsiElement = BerryCrushPsiElement(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = BerryCrushFile(viewProvider)
}

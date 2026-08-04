package com.berrycrush.intellij.parser

import com.berrycrush.intellij.language.BerryCrushLanguage
import com.berrycrush.intellij.lexer.BerryCrushLexer
import com.berrycrush.intellij.lexer.BerryCrushTokenTypes
import com.berrycrush.intellij.psi.BerryCrushAssertElement
import com.berrycrush.intellij.psi.BerryCrushAssertOperationElement
import com.berrycrush.intellij.psi.BerryCrushCallElement
import com.berrycrush.intellij.psi.BerryCrushElementTypes
import com.berrycrush.intellij.psi.BerryCrushExtractElement
import com.berrycrush.intellij.psi.BerryCrushFeatureElement
import com.berrycrush.intellij.psi.BerryCrushFile
import com.berrycrush.intellij.psi.BerryCrushFragmentElement
import com.berrycrush.intellij.psi.BerryCrushFragmentRefElement
import com.berrycrush.intellij.psi.BerryCrushGenericElement
import com.berrycrush.intellij.psi.BerryCrushIncludeElement
import com.berrycrush.intellij.psi.BerryCrushIncludeParameterElement
import com.berrycrush.intellij.psi.BerryCrushJsonPathElement
import com.berrycrush.intellij.psi.BerryCrushNotElement
import com.berrycrush.intellij.psi.BerryCrushOperationRefElement
import com.berrycrush.intellij.psi.BerryCrushOperatorElement
import com.berrycrush.intellij.psi.BerryCrushParameterEntryElement
import com.berrycrush.intellij.psi.BerryCrushParameterKeyElement
import com.berrycrush.intellij.psi.BerryCrushParameterValueElement
import com.berrycrush.intellij.psi.BerryCrushParametersElement
import com.berrycrush.intellij.psi.BerryCrushScenarioElement
import com.berrycrush.intellij.psi.BerryCrushStepElement
import com.berrycrush.intellij.psi.BerryCrushTextElement
import com.berrycrush.intellij.psi.BerryCrushVariableRefElement
import com.berrycrush.intellij.psi.BerryCrushWebhookElement
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

    override fun getStringLiteralElements(): TokenSet = BerryCrushTokenTypes.STRINGS

    override fun createElement(node: ASTNode): PsiElement {
        return when (node.elementType) {
            BerryCrushElementTypes.FEATURE -> BerryCrushFeatureElement(node)
            BerryCrushElementTypes.SCENARIO -> BerryCrushScenarioElement("scenario", node)
            BerryCrushElementTypes.OUTLINE -> BerryCrushScenarioElement("outline", node)
            BerryCrushElementTypes.BACKGROUND -> BerryCrushScenarioElement("background", node)
            BerryCrushElementTypes.FRAGMENT -> BerryCrushFragmentElement(node)
            BerryCrushElementTypes.STEP -> BerryCrushStepElement(node)
            BerryCrushElementTypes.CALL_DIRECTIVE -> BerryCrushCallElement(node)
            BerryCrushElementTypes.INCLUDE_DIRECTIVE -> BerryCrushIncludeElement(node)
            BerryCrushElementTypes.ASSERT_DIRECTIVE -> BerryCrushAssertElement(node)
            BerryCrushElementTypes.EXTRACT_DIRECTIVE -> BerryCrushExtractElement(node)
            BerryCrushElementTypes.WEBHOOK_DIRECTIVE -> BerryCrushWebhookElement(node)
            BerryCrushElementTypes.OPERATION_REF -> BerryCrushOperationRefElement(node)
            BerryCrushElementTypes.FRAGMENT_REF -> BerryCrushFragmentRefElement(node)
            BerryCrushElementTypes.VARIABLE_REF -> BerryCrushVariableRefElement(node)
            BerryCrushElementTypes.INCLUDED_PARAMETER -> BerryCrushIncludeParameterElement(node)
            BerryCrushElementTypes.PARAMETERS -> BerryCrushParametersElement(node)
            BerryCrushElementTypes.PARAMETER_ENTRY -> BerryCrushParameterEntryElement(node)
            BerryCrushElementTypes.PARAMETER_KEY -> BerryCrushParameterKeyElement(node)
            BerryCrushElementTypes.PARAMETER_VALUE -> BerryCrushParameterValueElement(node)
            BerryCrushElementTypes.JSON_PATH -> BerryCrushJsonPathElement(node)
            BerryCrushElementTypes.ASSERTION_OPERATION -> BerryCrushAssertOperationElement(node)
            BerryCrushElementTypes.NOT -> BerryCrushNotElement(node)
            BerryCrushElementTypes.OPERATOR -> BerryCrushOperatorElement(node)
            BerryCrushElementTypes.TEXT -> BerryCrushTextElement(node)
            else -> BerryCrushGenericElement(node)
        }
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile = BerryCrushFile(viewProvider)
}

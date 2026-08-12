package org.berrycrush.intellij.parser

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
import org.berrycrush.intellij.language.BerryCrushLanguage
import org.berrycrush.intellij.lexer.BerryCrushLexer
import org.berrycrush.intellij.lexer.BerryCrushTokenTypes
import org.berrycrush.intellij.psi.BerryCrushAssertElement
import org.berrycrush.intellij.psi.BerryCrushAssertOperationElement
import org.berrycrush.intellij.psi.BerryCrushBackgroundElement
import org.berrycrush.intellij.psi.BerryCrushBlockNameElement
import org.berrycrush.intellij.psi.BerryCrushCallElement
import org.berrycrush.intellij.psi.BerryCrushConditionElement
import org.berrycrush.intellij.psi.BerryCrushElementTypes
import org.berrycrush.intellij.psi.BerryCrushElseElement
import org.berrycrush.intellij.psi.BerryCrushExampleHeaderElement
import org.berrycrush.intellij.psi.BerryCrushExampleRowElement
import org.berrycrush.intellij.psi.BerryCrushExampleValueElement
import org.berrycrush.intellij.psi.BerryCrushExamplesElement
import org.berrycrush.intellij.psi.BerryCrushExtractElement
import org.berrycrush.intellij.psi.BerryCrushFeatureElement
import org.berrycrush.intellij.psi.BerryCrushFile
import org.berrycrush.intellij.psi.BerryCrushFragmentElement
import org.berrycrush.intellij.psi.BerryCrushFragmentRefElement
import org.berrycrush.intellij.psi.BerryCrushGenericElement
import org.berrycrush.intellij.psi.BerryCrushIfElement
import org.berrycrush.intellij.psi.BerryCrushIncludeElement
import org.berrycrush.intellij.psi.BerryCrushIncludeParameterElement
import org.berrycrush.intellij.psi.BerryCrushJsonPathElement
import org.berrycrush.intellij.psi.BerryCrushNotElement
import org.berrycrush.intellij.psi.BerryCrushOperationRefElement
import org.berrycrush.intellij.psi.BerryCrushOperatorElement
import org.berrycrush.intellij.psi.BerryCrushOutlineElement
import org.berrycrush.intellij.psi.BerryCrushParameterEntryElement
import org.berrycrush.intellij.psi.BerryCrushParameterKeyElement
import org.berrycrush.intellij.psi.BerryCrushParameterValueElement
import org.berrycrush.intellij.psi.BerryCrushParametersElement
import org.berrycrush.intellij.psi.BerryCrushScenarioElement
import org.berrycrush.intellij.psi.BerryCrushStepElement
import org.berrycrush.intellij.psi.BerryCrushStringLiteralElement
import org.berrycrush.intellij.psi.BerryCrushTagElement
import org.berrycrush.intellij.psi.BerryCrushTextElement
import org.berrycrush.intellij.psi.BerryCrushVariableRefElement
import org.berrycrush.intellij.psi.BerryCrushWebhookElement

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

    override fun createElement(node: ASTNode): PsiElement = when (node.elementType) {
        BerryCrushElementTypes.FEATURE -> BerryCrushFeatureElement(node)
        BerryCrushElementTypes.SCENARIO -> BerryCrushScenarioElement(node)
        BerryCrushElementTypes.OUTLINE -> BerryCrushOutlineElement(node)
        BerryCrushElementTypes.EXAMPLES -> BerryCrushExamplesElement(node)
        BerryCrushElementTypes.EXAMPLE_ROW -> BerryCrushExampleRowElement(node)
        BerryCrushElementTypes.EXAMPLES_HEADER -> BerryCrushExampleHeaderElement(node)
        BerryCrushElementTypes.EXAMPLES_VALUE -> BerryCrushExampleValueElement(node)
        BerryCrushElementTypes.BACKGROUND -> BerryCrushBackgroundElement(node)
        BerryCrushElementTypes.FRAGMENT -> BerryCrushFragmentElement(node)
        BerryCrushElementTypes.STEP -> BerryCrushStepElement(node)
        BerryCrushElementTypes.CALL_DIRECTIVE -> BerryCrushCallElement(node)
        BerryCrushElementTypes.INCLUDE_DIRECTIVE -> BerryCrushIncludeElement(node)
        BerryCrushElementTypes.IF_DIRECTIVE -> BerryCrushIfElement(node)
        BerryCrushElementTypes.ELSE_DIRECTIVE -> BerryCrushElseElement(node)
        BerryCrushElementTypes.ASSERT_DIRECTIVE -> BerryCrushAssertElement(node)
        BerryCrushElementTypes.CONDITION -> BerryCrushConditionElement(node)
        BerryCrushElementTypes.EXTRACT_DIRECTIVE -> BerryCrushExtractElement(node)
        BerryCrushElementTypes.WEBHOOK_DIRECTIVE -> BerryCrushWebhookElement(node)
        BerryCrushElementTypes.OPERATION_REF -> BerryCrushOperationRefElement(node)
        BerryCrushElementTypes.FRAGMENT_REF -> BerryCrushFragmentRefElement(node)
        BerryCrushElementTypes.VARIABLE_REF -> BerryCrushVariableRefElement(node)
        BerryCrushElementTypes.STRING_LITERAL -> BerryCrushStringLiteralElement(node)
        BerryCrushElementTypes.INCLUDED_PARAMETER -> BerryCrushIncludeParameterElement(node)
        BerryCrushElementTypes.PARAMETERS -> BerryCrushParametersElement(node)
        BerryCrushElementTypes.PARAMETER_ENTRY -> BerryCrushParameterEntryElement(node)
        BerryCrushElementTypes.PARAMETER_KEY -> BerryCrushParameterKeyElement(node)
        BerryCrushElementTypes.PARAMETER_VALUE -> BerryCrushParameterValueElement(node)
        BerryCrushElementTypes.JSON_PATH -> BerryCrushJsonPathElement(node)
        BerryCrushElementTypes.ASSERTION_OPERATION -> BerryCrushAssertOperationElement(node)
        BerryCrushElementTypes.NOT -> BerryCrushNotElement(node)
        BerryCrushElementTypes.OPERATOR -> BerryCrushOperatorElement(node)
        BerryCrushElementTypes.BLOCK_NAME -> BerryCrushBlockNameElement(node)
        BerryCrushElementTypes.TEXT -> BerryCrushTextElement(node)
        BerryCrushElementTypes.TAG -> BerryCrushTagElement(node)
        else -> BerryCrushGenericElement(node)
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile = BerryCrushFile(viewProvider)
}

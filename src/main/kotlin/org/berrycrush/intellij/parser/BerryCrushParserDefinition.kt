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
import org.berrycrush.intellij.psi.BerryCrushBindingNameElement
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
import org.berrycrush.intellij.psi.BerryCrushFailElement
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
import org.berrycrush.intellij.psi.BerryCrushStepNameElement
import org.berrycrush.intellij.psi.BerryCrushStringLiteralElement
import org.berrycrush.intellij.psi.BerryCrushTagElement
import org.berrycrush.intellij.psi.BerryCrushTextElement
import org.berrycrush.intellij.psi.BerryCrushUsingElement
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

    override fun createElement(node: ASTNode): PsiElement = elementDefinition[node.elementType]?.invoke(node) ?: BerryCrushGenericElement(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = BerryCrushFile(viewProvider)
}

private val elementDefinition = mapOf(
    BerryCrushElementTypes.FEATURE to ::BerryCrushFeatureElement,
    BerryCrushElementTypes.SCENARIO to ::BerryCrushScenarioElement,
    BerryCrushElementTypes.OUTLINE to ::BerryCrushOutlineElement,
    BerryCrushElementTypes.EXAMPLES to ::BerryCrushExamplesElement,
    BerryCrushElementTypes.EXAMPLE_ROW to ::BerryCrushExampleRowElement,
    BerryCrushElementTypes.EXAMPLES_HEADER to ::BerryCrushExampleHeaderElement,
    BerryCrushElementTypes.EXAMPLES_VALUE to ::BerryCrushExampleValueElement,
    BerryCrushElementTypes.BACKGROUND to ::BerryCrushBackgroundElement,
    BerryCrushElementTypes.FRAGMENT to ::BerryCrushFragmentElement,
    BerryCrushElementTypes.STEP to ::BerryCrushStepElement,
    BerryCrushElementTypes.STEP_DESCRIPTION to ::BerryCrushStepNameElement,
    BerryCrushElementTypes.CALL_DIRECTIVE to ::BerryCrushCallElement,
    BerryCrushElementTypes.USING to ::BerryCrushUsingElement,
    BerryCrushElementTypes.BINDING_NAME to ::BerryCrushBindingNameElement,
    BerryCrushElementTypes.INCLUDE_DIRECTIVE to ::BerryCrushIncludeElement,
    BerryCrushElementTypes.IF_DIRECTIVE to ::BerryCrushIfElement,
    BerryCrushElementTypes.ELSE_DIRECTIVE to ::BerryCrushElseElement,
    BerryCrushElementTypes.ASSERT_DIRECTIVE to ::BerryCrushAssertElement,
    BerryCrushElementTypes.FAIL_DIRECTIVE to ::BerryCrushFailElement,
    BerryCrushElementTypes.CONDITION to ::BerryCrushConditionElement,
    BerryCrushElementTypes.EXTRACT_DIRECTIVE to ::BerryCrushExtractElement,
    BerryCrushElementTypes.WEBHOOK_DIRECTIVE to ::BerryCrushWebhookElement,
    BerryCrushElementTypes.OPERATION_REF to ::BerryCrushOperationRefElement,
    BerryCrushElementTypes.FRAGMENT_REF to ::BerryCrushFragmentRefElement,
    BerryCrushElementTypes.VARIABLE_REF to ::BerryCrushVariableRefElement,
    BerryCrushElementTypes.STRING_LITERAL to ::BerryCrushStringLiteralElement,
    BerryCrushElementTypes.INCLUDED_PARAMETER to ::BerryCrushIncludeParameterElement,
    BerryCrushElementTypes.PARAMETERS to ::BerryCrushParametersElement,
    BerryCrushElementTypes.PARAMETER_ENTRY to ::BerryCrushParameterEntryElement,
    BerryCrushElementTypes.PARAMETER_KEY to ::BerryCrushParameterKeyElement,
    BerryCrushElementTypes.PARAMETER_VALUE to ::BerryCrushParameterValueElement,
    BerryCrushElementTypes.JSON_PATH to ::BerryCrushJsonPathElement,
    BerryCrushElementTypes.ASSERTION_OPERATION to ::BerryCrushAssertOperationElement,
    BerryCrushElementTypes.NOT to ::BerryCrushNotElement,
    BerryCrushElementTypes.OPERATOR to ::BerryCrushOperatorElement,
    BerryCrushElementTypes.BLOCK_NAME to ::BerryCrushBlockNameElement,
    BerryCrushElementTypes.TEXT to ::BerryCrushTextElement,
    BerryCrushElementTypes.TAG to ::BerryCrushTagElement,
)

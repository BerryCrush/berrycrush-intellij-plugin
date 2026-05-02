package com.berrycrush.intellij.refactoring

import com.berrycrush.intellij.psi.BerryCrushFile
import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement

/**
 * Enables refactoring support for BerryCrush scenario and fragment files.
 *
 * Supports:
 * - In-place rename for fragment definitions
 * - In-place rename for variable placeholders
 */
class BerryCrushRefactoringSupportProvider : RefactoringSupportProvider() {

    override fun isMemberInplaceRenameAvailable(element: PsiElement, context: PsiElement?): Boolean =
        element.containingFile is BerryCrushFile && isRenameableElement(element)

    override fun isInplaceRenameAvailable(element: PsiElement, context: PsiElement?): Boolean =
        element.containingFile is BerryCrushFile && isRenameableElement(element)

    private fun isRenameableElement(element: PsiElement): Boolean {
        val text = element.text
        val lineText = getLineText(element)

        return isFragmentDefinition(lineText) || isIncludeDirective(lineText) || isVariablePlaceholder(text)
    }

    private fun getLineText(element: PsiElement): String {
        val document = element.containingFile?.viewProvider?.document ?: return ""
        val offset = element.textOffset
        val lineNumber = document.getLineNumber(offset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineEnd = document.getLineEndOffset(lineNumber)
        return document.getText(com.intellij.openapi.util.TextRange(lineStart, lineEnd))
    }

    private fun isFragmentDefinition(lineText: String): Boolean =
        FRAGMENT_DEF_PATTERN.containsMatchIn(lineText)

    private fun isIncludeDirective(lineText: String): Boolean =
        INCLUDE_PATTERN.containsMatchIn(lineText)

    private fun isVariablePlaceholder(text: String): Boolean =
        VARIABLE_PATTERN.containsMatchIn(text)

    companion object {
        private val FRAGMENT_DEF_PATTERN = Regex("""^\s*[Ff]ragment:\s*\S+""")
        private val INCLUDE_PATTERN = Regex("""^\s*include\s+\^?\S+""")
        private val VARIABLE_PATTERN = Regex("""\{\{[^}]+}}""")
    }
}

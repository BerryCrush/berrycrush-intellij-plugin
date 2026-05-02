package com.berrycrush.intellij.refactoring.fragment

import com.berrycrush.intellij.index.IncludeUsageIndex
import com.berrycrush.intellij.psi.BerryCrushFile
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.rename.RenamePsiElementProcessor

/**
 * Handles renaming of fragment definitions with automatic update of all include directives.
 *
 * When a fragment is renamed:
 * 1. The fragment definition (fragment: name) is updated
 * 2. All include directives referencing this fragment are updated
 *
 * Supports renaming from:
 * - Fragment definition line
 * - Include directive line
 */
class FragmentRenameProcessor : RenamePsiElementProcessor() {

    override fun canProcessElement(element: PsiElement): Boolean {
        if (element.containingFile !is BerryCrushFile) return false

        val lineText = getLineText(element)
        return isFragmentDefinition(lineText) || isIncludeDirective(lineText)
    }

    override fun prepareRenaming(
        element: PsiElement,
        newName: String,
        allRenames: MutableMap<PsiElement, String>,
        scope: SearchScope,
    ) {
        val fragmentName = extractFragmentName(element) ?: return
        val project = element.project

        // Find all include usages
        IncludeUsageIndex.findIncludeUsages(project, fragmentName)
            .forEach { usage -> allRenames[usage] = newName }

        // If renaming from include, also rename the definition
        if (isIncludeDirective(getLineText(element))) {
            findFragmentDefinition(project, fragmentName)?.let { definition ->
                allRenames[definition] = newName
            }
        }
    }

    /**
     * Extracts fragment name from element's line.
     * Handles both "fragment: name" and "include name" syntaxes.
     */
    private fun extractFragmentName(element: PsiElement): String? {
        val lineText = getLineText(element)
        return extractFromFragmentDef(lineText) ?: extractFromInclude(lineText)
    }

    private fun extractFromFragmentDef(lineText: String): String? =
        FRAGMENT_DEF_PATTERN.find(lineText)?.groupValues?.get(1)

    private fun extractFromInclude(lineText: String): String? =
        INCLUDE_PATTERN.find(lineText)?.groupValues?.get(1)?.removePrefix("^")

    private fun isFragmentDefinition(lineText: String): Boolean =
        FRAGMENT_DEF_PATTERN.containsMatchIn(lineText)

    private fun isIncludeDirective(lineText: String): Boolean =
        INCLUDE_PATTERN.containsMatchIn(lineText)

    private fun getLineText(element: PsiElement): String {
        val document = element.containingFile?.viewProvider?.document ?: return ""
        val offset = element.textOffset
        val lineNumber = document.getLineNumber(offset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineEnd = document.getLineEndOffset(lineNumber)
        return document.getText(com.intellij.openapi.util.TextRange(lineStart, lineEnd))
    }

    private fun findFragmentDefinition(project: Project, fragmentName: String): PsiElement? =
        com.berrycrush.intellij.index.FragmentIndex.findFragmentElement(project, fragmentName)

    companion object {
        private val FRAGMENT_DEF_PATTERN = Regex("""^\s*[Ff]ragment:\s*(\S+)""")
        private val INCLUDE_PATTERN = Regex("""^\s*include\s+(\^?[a-zA-Z_][a-zA-Z0-9_.\-]*)""")
    }
}

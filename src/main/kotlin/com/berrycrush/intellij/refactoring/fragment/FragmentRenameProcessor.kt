package com.berrycrush.intellij.refactoring.fragment

import com.berrycrush.intellij.index.IncludeUsageIndex
import com.berrycrush.intellij.psi.BerryCrushFile
import com.berrycrush.intellij.psi.BerryCrushFragmentElement
import com.berrycrush.intellij.psi.BerryCrushIncludeElement
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil
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

        return isFragmentDefinition(element) || isIncludeDirective(element)
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
        IncludeUsageIndex
            .findIncludeUsages(project, fragmentName)
            .forEach { usage -> allRenames[usage] = newName }

        // If renaming from include, also rename the definition
        if (isIncludeDirective(element)) {
            findFragmentDefinition(project, fragmentName)?.let { definition ->
                allRenames[definition] = newName
            }
        }
    }

    /**
     * Extracts fragment name from element's line.
     * Handles both "fragment: name" and "include name" syntaxes.
     */
    private fun extractFragmentName(element: PsiElement): String? = extractFromFragmentDef(element) ?: extractFromInclude(element)

    private fun extractFromFragmentDef(element: PsiElement): String? = PsiTreeUtil.getParentOfType(element, BerryCrushFragmentElement::class.java)?.fragmentName

    private fun extractFromInclude(element: PsiElement): String? = PsiTreeUtil.getParentOfType(element, BerryCrushIncludeElement::class.java)?.fragmentName

    private fun isFragmentDefinition(element: PsiElement): Boolean = PsiTreeUtil.getParentOfType(element, BerryCrushFragmentElement::class.java) != null

    private fun isIncludeDirective(element: PsiElement): Boolean = PsiTreeUtil.getParentOfType(element, BerryCrushIncludeElement::class.java) != null

    private fun findFragmentDefinition(
        project: Project,
        fragmentName: String,
    ): PsiElement? = com.berrycrush.intellij.index.FragmentIndex
        .findFragmentElement(project, fragmentName)
}

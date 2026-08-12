package com.berrycrush.intellij.reference

import com.berrycrush.intellij.index.FragmentIndex
import com.berrycrush.intellij.psi.BerryCrushElementFactory
import com.berrycrush.intellij.psi.BerryCrushFragmentElement
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil

/**
 * Reference from `include fragmentName` to the fragment file.
 */
class BerryCrushFragmentReference(
    element: PsiElement,
    textRange: TextRange,
    private val fragmentName: String,
) : PsiReferenceBase<PsiElement>(element, textRange, false) {
    override fun resolve(): PsiElement? = findFragmentByName(this.element.project, fragmentName)

    override fun getVariants(): Array<Any> = findAllFragments(this.element.project).toTypedArray()

    override fun handleElementRename(newElementName: String): PsiElement = element.replace(BerryCrushElementFactory.createFragmentRefElement(element.project, newElementName))

    companion object {
        /**
         * Find a fragment by its name using the FragmentIndex.
         * Returns the fragment definition element, or the file if element not found.
         */
        fun findFragmentByName(
            project: Project,
            fragmentName: String,
        ): PsiElement? = // Use FragmentIndex for content-based lookup (finds fragments by "fragment: name")
            FragmentIndex.findFragmentElement(project, fragmentName)

        /**
         * Find all fragments in the project.
         */
        fun findAllFragments(project: Project): List<BerryCrushFragmentElement> {
            val scope = GlobalSearchScope.allScope(project)
            return FilenameIndex.getAllFilesByExt(project, "fragment", scope).toList()
                .map { it.findPsiFile(project) }
                .flatMap { PsiTreeUtil.findChildrenOfType(it, BerryCrushFragmentElement::class.java) }
        }
    }
}

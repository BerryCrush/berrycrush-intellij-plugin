package com.berrycrush.intellij.reference

import com.berrycrush.intellij.index.FragmentIndex
import com.berrycrush.intellij.psi.BerryCrushFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

/**
 * Reference from `include fragmentName` to the fragment file.
 */
class BerryCrushFragmentReference(
    element: PsiElement,
    textRange: TextRange,
    private val fragmentName: String
) : PsiReferenceBase<PsiElement>(element, textRange, true) {

    override fun resolve(): PsiElement? {
        val project = element.project
        return findFragmentByName(project, fragmentName)
    }

    override fun getVariants(): Array<Any> {
        val project = element.project
        return findAllFragments(project)
            .mapNotNull { it.nameWithoutExtension }
            .toTypedArray()
    }

    companion object {
        /**
         * Find a fragment by its name using the FragmentIndex.
         * Returns the fragment definition element, or the file if element not found.
         */
        fun findFragmentByName(project: Project, fragmentName: String): PsiElement? {
            // Use FragmentIndex for content-based lookup (finds fragments by "fragment: name")
            val fragmentElement = FragmentIndex.findFragmentElement(project, fragmentName)
            if (fragmentElement != null) {
                return fragmentElement
            }

            // Fallback: Try exact filename match (fragmentName.fragment)
            val scope = GlobalSearchScope.allScope(project)
            val psiManager = PsiManager.getInstance(project)
            val exactName = if (fragmentName.endsWith(".fragment")) fragmentName else "$fragmentName.fragment"
            val exactFiles = FilenameIndex.getVirtualFilesByName(exactName, scope)
            if (exactFiles.isNotEmpty()) {
                return psiManager.findFile(exactFiles.first())
            }

            // Fallback: Try filename without extension match
            FilenameIndex.getAllFilesByExt(project, "fragment", scope).forEach { file ->
                if (file.nameWithoutExtension == fragmentName) {
                    return psiManager.findFile(file)
                }
            }

            return null
        }

        /**
         * Find all fragment files in the project.
         */
        fun findAllFragments(project: Project): List<VirtualFile> {
            val scope = GlobalSearchScope.allScope(project)
            return FilenameIndex.getAllFilesByExt(project, "fragment", scope).toList()
        }
    }
}

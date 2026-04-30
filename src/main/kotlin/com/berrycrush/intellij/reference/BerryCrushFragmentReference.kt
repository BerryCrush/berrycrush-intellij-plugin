package com.berrycrush.intellij.reference

import com.berrycrush.intellij.psi.BerryCrushFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
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
         * Find a fragment file by its name.
         */
        fun findFragmentByName(project: Project, fragmentName: String): PsiElement? {
            val scope = GlobalSearchScope.allScope(project)
            val psiManager = PsiManager.getInstance(project)

            // Try to find by exact filename (fragmentName.fragment)
            val exactName = if (fragmentName.endsWith(".fragment")) fragmentName else "$fragmentName.fragment"
            val exactFiles = FilenameIndex.getVirtualFilesByName(exactName, scope)
            if (exactFiles.isNotEmpty()) {
                return psiManager.findFile(exactFiles.first())
            }

            // Search all .fragment files
            FilenameIndex.getAllFilesByExt(project, "fragment", scope).forEach { file ->
                // Match by filename without extension
                if (file.nameWithoutExtension == fragmentName) {
                    return psiManager.findFile(file)
                }

                // Match by content (fragment: name)
                val psiFile = psiManager.findFile(file) ?: return@forEach
                val name = extractFragmentNameFromFile(psiFile)
                if (name == fragmentName) {
                    return psiFile
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

        /**
         * Extract fragment name from file content.
         */
        private fun extractFragmentNameFromFile(file: PsiFile): String? {
            val text = file.text
            // Look for "fragment: name" at the start of the file
            val fragmentMatch = Regex("^\\s*fragment:\\s*(\\S+)", RegexOption.MULTILINE).find(text)
            return fragmentMatch?.groupValues?.get(1)
        }
    }
}

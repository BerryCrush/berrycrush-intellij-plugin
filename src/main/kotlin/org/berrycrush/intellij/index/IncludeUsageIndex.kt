package org.berrycrush.intellij.index

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.indexing.ScalarIndexExtension
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import org.berrycrush.intellij.language.FragmentFileType
import org.berrycrush.intellij.language.ScenarioFileType
import org.berrycrush.intellij.psi.BerryCrushIncludeElement

/**
 * Index for BerryCrush include directive usages.
 *
 * Enables fast lookup of fragment usages (include fragmentName) across the project.
 * Used for reverse navigation from fragment definition to its usages.
 */
class IncludeUsageIndex : ScalarIndexExtension<String>() {
    override fun getName(): ID<String, Void> = KEY

    override fun getVersion(): Int = VERSION

    override fun dependsOnFileContent(): Boolean = true

    override fun getIndexer(): DataIndexer<String, Void, FileContent> = DataIndexer { fileContent ->
        val result = mutableMapOf<String, Void?>()
        PsiTreeUtil.findChildrenOfType(fileContent.psiFile, BerryCrushIncludeElement::class.java).forEach { include ->
            include.fragmentName?.let { name ->
                if (name.isNotEmpty()) {
                    result[name] = null
                }
            }
        }
        result
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getInputFilter(): FileBasedIndex.InputFilter = DefaultFileTypeSpecificInputFilter(ScenarioFileType, FragmentFileType)

    companion object {
        @JvmField
        val KEY: ID<String, Void> = ID.create("berrycrush.include.usage.index")

        private const val VERSION = 1

        /**
         * Gets all included fragment names in the project.
         */
        fun getAllIncludedFragments(project: Project): Collection<String> = FileBasedIndex.getInstance().getAllKeys(KEY, project)

        /**
         * Gets all files that include the given fragment.
         */
        fun getFilesIncludingFragment(
            project: Project,
            fragmentName: String,
        ): Collection<VirtualFile> = FileBasedIndex.getInstance().getContainingFiles(
            KEY,
            fragmentName,
            GlobalSearchScope.projectScope(project),
        )

        /**
         * Finds all PSI elements that include the given fragment.
         */
        fun findIncludeUsages(
            project: Project,
            fragmentName: String,
        ): List<PsiElement> {
            val files = getFilesIncludingFragment(project, fragmentName)
            val psiManager = PsiManager.getInstance(project)
            val results = mutableListOf<PsiElement>()

            for (file in files) {
                val psiFile = psiManager.findFile(file) ?: continue
                results.addAll(findIncludeDirectivesInFile(psiFile, fragmentName))
            }

            return results
        }

        private fun findIncludeDirectivesInFile(
            file: PsiFile,
            fragmentName: String,
        ): List<PsiElement> = PsiTreeUtil
            .findChildrenOfType(file, BerryCrushIncludeElement::class.java)
            .filter { it.fragmentName == fragmentName }
    }
}

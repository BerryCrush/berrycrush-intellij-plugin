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
import org.berrycrush.intellij.psi.BerryCrushFragmentElement

/**
 * Index for BerryCrush fragment definitions.
 *
 * Enables fast lookup of fragment definitions by name across the project.
 * This index stores fragment names defined in .fragment files using the
 * "Fragment: name" syntax.
 */
class FragmentIndex : ScalarIndexExtension<String>() {
    override fun getName(): ID<String, Void> = KEY

    override fun getVersion(): Int = VERSION

    override fun dependsOnFileContent(): Boolean = true

    override fun getIndexer(): DataIndexer<String, Void, FileContent> = DataIndexer { fileContent ->
        val result = mutableMapOf<String, Void?>()
        PsiTreeUtil.findChildrenOfType(fileContent.psiFile, BerryCrushFragmentElement::class.java).forEach { fragment ->
            fragment.fragmentName?.let { name ->
                if (name.isNotEmpty()) {
                    result[name] = null
                }
            }
        }

        result
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getInputFilter(): FileBasedIndex.InputFilter = DefaultFileTypeSpecificInputFilter(FragmentFileType)

    companion object {
        @JvmField
        val KEY: ID<String, Void> = ID.create("berrycrush.fragment.index")

        private const val VERSION = 2

        /**
         * Gets all fragment names in the project.
         * Returns empty collection if indexing is in progress (dumb mode).
         */
        fun getAllFragmentNames(project: Project): Collection<String> = FileBasedIndex.getInstance().getAllKeys(KEY, project)

        /**
         * Gets all files containing a fragment with the given name.
         * Returns empty collection if indexing is in progress (dumb mode).
         */
        fun getFragmentFiles(
            project: Project,
            fragmentName: String,
        ): Collection<VirtualFile> = FileBasedIndex.getInstance().getContainingFiles(
            KEY,
            fragmentName,
            GlobalSearchScope.projectScope(project),
        )

        /**
         * Finds the PSI element for a fragment definition.
         */
        fun findFragmentElement(
            project: Project,
            fragmentName: String,
        ): PsiElement? {
            val files = getFragmentFiles(project, fragmentName)
            val psiManager = PsiManager.getInstance(project)

            for (file in files) {
                val psiFile = psiManager.findFile(file) ?: continue
                // Find the fragment definition line in the file
                val element = findFragmentDefinitionInFile(psiFile, fragmentName)
                if (element != null) return element
            }
            return null
        }

        private fun findFragmentDefinitionInFile(
            file: PsiFile,
            fragmentName: String,
        ): PsiElement? = PsiTreeUtil
            .findChildrenOfType(file, BerryCrushFragmentElement::class.java)
            .find { fragment -> fragment.fragmentName == fragmentName }
    }
}

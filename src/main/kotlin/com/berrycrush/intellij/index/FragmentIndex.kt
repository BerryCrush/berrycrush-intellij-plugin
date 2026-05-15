package com.berrycrush.intellij.index

import com.berrycrush.intellij.language.FragmentFileType
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.indexing.ScalarIndexExtension
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor

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
        val text = fileContent.contentAsText.toString()

        // Process line by line to skip commented lines
        text.lineSequence().forEach { line ->
            val trimmedLine = line.trimStart()
            // Skip comment lines
            if (trimmedLine.startsWith("#")) return@forEach

            // Find fragment definition in this line
            val match = FRAGMENT_PATTERN.find(line) ?: return@forEach
            val fragmentName = match.groupValues[1].trim()
            if (fragmentName.isNotEmpty()) {
                result[fragmentName] = null
            }
        }

        result
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getInputFilter(): FileBasedIndex.InputFilter =
        DefaultFileTypeSpecificInputFilter(FragmentFileType)

    companion object {
        @JvmField
        val KEY: ID<String, Void> = ID.create("berrycrush.fragment.index")

        private const val VERSION = 2

        private val FRAGMENT_PATTERN = Regex("""[Ff]ragment:\s*(\S+)""")

        /**
         * Gets all fragment names in the project.
         * Returns empty collection if indexing is in progress (dumb mode).
         */
        fun getAllFragmentNames(project: Project): Collection<String> {
            if (DumbService.isDumb(project)) {
                return emptyList()
            }
            return FileBasedIndex.getInstance().getAllKeys(KEY, project)
        }

        /**
         * Gets all files containing a fragment with the given name.
         * Returns empty collection if indexing is in progress (dumb mode).
         */
        fun getFragmentFiles(project: Project, fragmentName: String): Collection<VirtualFile> {
            if (DumbService.isDumb(project)) {
                return emptyList()
            }
            return FileBasedIndex.getInstance().getContainingFiles(
                KEY,
                fragmentName,
                GlobalSearchScope.projectScope(project)
            )
        }

        /**
         * Finds the PSI element for a fragment definition.
         */
        fun findFragmentElement(project: Project, fragmentName: String): PsiElement? {
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

        private fun findFragmentDefinitionInFile(file: PsiFile, fragmentName: String): PsiElement? {
            val text = file.text
            val pattern = Regex("""[Ff]ragment:\s*${Regex.escape(fragmentName)}""")

            // Process line by line to skip commented lines
            var offset = 0
            text.lineSequence().forEach { line ->
                val trimmedLine = line.trimStart()
                // Skip comment lines
                if (!trimmedLine.startsWith("#")) {
                    val match = pattern.find(line)
                    if (match != null) {
                        // Find the element at this position
                        return file.findElementAt(offset + match.range.first)
                    }
                }
                offset += line.length + 1 // +1 for newline
            }
            return null
        }
    }
}

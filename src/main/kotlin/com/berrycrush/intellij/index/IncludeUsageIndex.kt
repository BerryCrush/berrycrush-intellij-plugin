package com.berrycrush.intellij.index

import com.berrycrush.intellij.language.FragmentFileType
import com.berrycrush.intellij.language.ScenarioFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.indexing.ScalarIndexExtension
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import com.intellij.psi.search.GlobalSearchScope

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
        val text = fileContent.contentAsText.toString()

        // Find all include directives (include fragmentName)
        INCLUDE_PATTERN.findAll(text).forEach { match ->
            val fragmentName = match.groupValues[1].removePrefix("^")
            if (fragmentName.isNotEmpty()) {
                result[fragmentName] = null
            }
        }

        result
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getInputFilter(): FileBasedIndex.InputFilter =
        DefaultFileTypeSpecificInputFilter(ScenarioFileType, FragmentFileType)

    companion object {
        @JvmField
        val KEY: ID<String, Void> = ID.create("berrycrush.include.usage.index")

        private const val VERSION = 2

        // Match "include fragmentName" at the start of a line (strict lowercase)
        private val INCLUDE_PATTERN = Regex(
            """^\s*include\s+(\^?[a-zA-Z_][a-zA-Z0-9_.\-]*)""",
            RegexOption.MULTILINE
        )

        /**
         * Gets all included fragment names in the project.
         */
        fun getAllIncludedFragments(project: Project): Collection<String> {
            return FileBasedIndex.getInstance().getAllKeys(KEY, project)
        }

        /**
         * Gets all files that include the given fragment.
         */
        fun getFilesIncludingFragment(project: Project, fragmentName: String): Collection<VirtualFile> {
            return FileBasedIndex.getInstance().getContainingFiles(
                KEY,
                fragmentName,
                GlobalSearchScope.projectScope(project)
            )
        }

        /**
         * Finds all PSI elements that include the given fragment.
         */
        fun findIncludeUsages(project: Project, fragmentName: String): List<PsiElement> {
            val files = getFilesIncludingFragment(project, fragmentName)
            val psiManager = PsiManager.getInstance(project)
            val results = mutableListOf<PsiElement>()

            for (file in files) {
                val psiFile = psiManager.findFile(file) ?: continue
                results.addAll(findIncludeDirectivesInFile(psiFile, fragmentName))
            }

            return results
        }

        private fun findIncludeDirectivesInFile(file: com.intellij.psi.PsiFile, fragmentName: String): List<PsiElement> {
            val results = mutableListOf<PsiElement>()
            val text = file.text
            val escapedName = Regex.escape(fragmentName)
            // Match "include fragmentName" at the start of a line (strict lowercase)
            val pattern = Regex(
                """^\s*(include\s+\^?$escapedName)(?!\w)""",
                RegexOption.MULTILINE
            )

            pattern.findAll(text).forEach { match ->
                // Find element at the "include" keyword position (group 1 start)
                val includeStart = match.groups[1]?.range?.first ?: match.range.first
                val element = file.findElementAt(includeStart)
                if (element != null) {
                    results.add(element)
                }
            }

            return results
        }
    }
}

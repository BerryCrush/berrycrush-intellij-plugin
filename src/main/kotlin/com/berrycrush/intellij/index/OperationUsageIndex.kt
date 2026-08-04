package com.berrycrush.intellij.index

import com.berrycrush.intellij.language.FragmentFileType
import com.berrycrush.intellij.language.ScenarioFileType
import com.berrycrush.intellij.psi.BerryCrushOperationRefElement
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
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

/**
 * Index for BerryCrush operation references.
 *
 * Enables fast lookup of operation usages (^operationId) across the project.
 * This index stores operation IDs referenced in .scenario and .fragment files.
 */
class OperationUsageIndex : ScalarIndexExtension<String>() {

    override fun getName(): ID<String, Void> = KEY

    override fun getVersion(): Int = VERSION

    override fun dependsOnFileContent(): Boolean = true

    override fun getIndexer(): DataIndexer<String, Void, FileContent> = DataIndexer { fileContent ->
        val result = mutableMapOf<String, Void?>()
        PsiTreeUtil.findChildrenOfType(fileContent.psiFile, BerryCrushOperationRefElement::class.java).forEach { ref ->
            if (ref.operationId.isNotEmpty()) {
                result[ref.operationId] = null
            }
        }
        result
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getInputFilter(): FileBasedIndex.InputFilter =
        DefaultFileTypeSpecificInputFilter(ScenarioFileType, FragmentFileType)

    companion object {
        @JvmField
        val KEY: ID<String, Void> = ID.create("berrycrush.operation.usage.index")

        private const val VERSION = 1

        /**
         * Gets all referenced operation IDs in the project.
         */
        fun getAllOperationIds(project: Project): Collection<String> {
            return FileBasedIndex.getInstance().getAllKeys(KEY, project)
        }

        /**
         * Gets all files containing references to the given operation ID.
         */
        fun getFilesReferencingOperation(project: Project, operationId: String): Collection<VirtualFile> {
            return FileBasedIndex.getInstance().getContainingFiles(
                KEY,
                operationId,
                GlobalSearchScope.projectScope(project)
            )
        }

        /**
         * Finds all PSI elements that reference the given operation ID.
         */
        fun findOperationUsages(project: Project, operationId: String): List<PsiElement> {
            val files = getFilesReferencingOperation(project, operationId)
            val psiManager = PsiManager.getInstance(project)
            val results = mutableListOf<PsiElement>()

            for (file in files) {
                val psiFile = psiManager.findFile(file) ?: continue
                results.addAll(findOperationReferencesInFile(psiFile, operationId))
            }

            return results
        }

        private fun findOperationReferencesInFile(file: com.intellij.psi.PsiFile, operationId: String): List<PsiElement> {
            return PsiTreeUtil.findChildrenOfType(file, BerryCrushOperationRefElement::class.java)
                .filter { it.operationId == operationId }
        }
    }
}

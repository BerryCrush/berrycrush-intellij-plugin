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
import org.berrycrush.intellij.psi.BerryCrushParametersElement

class NamedParametersIndex : ScalarIndexExtension<String>() {
    override fun getName() = KEY

    override fun getVersion() = VERSION

    override fun dependsOnFileContent(): Boolean = true

    override fun getIndexer(): DataIndexer<String, Void, FileContent> = DataIndexer { fileContent ->
        val result = mutableMapOf<String, Void?>()
        PsiTreeUtil.findChildrenOfType(fileContent.psiFile, BerryCrushParametersElement::class.java).forEach { parametersElement ->
            parametersElement.name?.let { name ->
                if (name.isNotEmpty()) {
                    result[name] = null
                }
            }
        }
        result
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getInputFilter() = DefaultFileTypeSpecificInputFilter(FragmentFileType)

    companion object {
        @JvmField
        val KEY = ID.create<String, Void>("berrycrush.named.parameter.index")

        private const val VERSION = 1

        fun getNamedParametersFiles(project: Project, name: String): Collection<VirtualFile> = FileBasedIndex.getInstance()
            .getContainingFiles(KEY, name, GlobalSearchScope.projectScope(project))

        fun findNamedParametersElement(project: Project, name: String): PsiElement? {
            val files = getNamedParametersFiles(project, name)
            val psiManager = PsiManager.getInstance(project)

            return files.asSequence()
                .mapNotNull { file -> psiManager.findFile(file) }
                .firstNotNullOfOrNull { psiFile -> findNamedParametersInFile(psiFile, name) }
        }

        fun findNamedParametersInFile(file: PsiFile, name: String): PsiElement? = PsiTreeUtil
            .findChildrenOfType(file, BerryCrushParametersElement::class.java)
            .find { it.name == name }
    }
}

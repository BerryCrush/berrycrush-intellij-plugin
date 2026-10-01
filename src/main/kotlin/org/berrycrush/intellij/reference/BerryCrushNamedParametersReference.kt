package org.berrycrush.intellij.reference

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.berrycrush.intellij.index.NamedParametersIndex
import org.berrycrush.intellij.psi.BerryCrushElementFactory
import org.berrycrush.intellij.psi.BerryCrushParametersElement

/**
 * Reference from a parameters include entry (`<< name`) to a named parameters block declaration.
 */
class BerryCrushNamedParametersReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val includeName: String,
) : PsiReferenceBase<PsiElement>(element, rangeInElement, false) {

    override fun resolve(): PsiElement? = findNamedParametersByName(element.project, includeName)

    override fun getVariants(): Array<out Any?> = findAllNamedParameters(element.project).toTypedArray()

    override fun handleElementRename(newElementName: String): PsiElement = element.replace(
        BerryCrushElementFactory.createParameterIncludeRefElement(element.project, newElementName),
    )

    companion object {
        fun findNamedParametersByName(project: Project, name: String): PsiElement? = NamedParametersIndex.findNamedParametersElement(project, name)

        fun findAllNamedParameters(project: Project): List<BerryCrushParametersElement> {
            val scope = GlobalSearchScope.projectScope(project)
            return FilenameIndex.getAllFilesByExt(project, "fragment", scope).toList()
                .map { it.findPsiFile(project) }
                .flatMap { PsiTreeUtil.findChildrenOfType(it, BerryCrushParametersElement::class.java) }
        }
    }
}

package com.berrycrush.intellij.refactoring.safedelete

import com.berrycrush.intellij.index.IncludeUsageIndex
import com.berrycrush.intellij.language.FragmentFileType
import com.berrycrush.intellij.psi.BerryCrushFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.safeDelete.NonCodeUsageSearchInfo
import com.intellij.refactoring.safeDelete.SafeDeleteProcessorDelegate
import com.intellij.usageView.UsageInfo

/**
 * Safe delete processor for BerryCrush fragment files.
 *
 * Checks for usages before allowing deletion:
 * - Warns if fragment is included anywhere
 * - Shows usages in preview dialog
 * - Allows force delete or cancel
 */
class BerryCrushSafeDeleteProcessor : SafeDeleteProcessorDelegate {

    override fun handlesElement(element: PsiElement): Boolean =
        element is PsiFile && element.virtualFile?.extension == FragmentFileType.EXTENSION

    override fun findUsages(
        element: PsiElement,
        allElementsToDelete: Array<out PsiElement>,
        usages: MutableList<in UsageInfo>,
    ): NonCodeUsageSearchInfo? {
        if (element !is PsiFile) return null

        val fragmentNames = extractFragmentNames(element)
        val project = element.project

        fragmentNames.forEach { fragmentName ->
            IncludeUsageIndex.findIncludeUsages(project, fragmentName)
                .map { UsageInfo(it) }
                .forEach { usages.add(it) }
        }

        return null
    }

    override fun getElementsToSearch(
        element: PsiElement,
        allElementsToDelete: Collection<PsiElement>,
    ): Collection<PsiElement>? = null

    override fun getAdditionalElementsToDelete(
        element: PsiElement,
        allElementsToDelete: Collection<PsiElement>,
        askUser: Boolean,
    ): Collection<PsiElement>? = null

    override fun findConflicts(
        element: PsiElement,
        allElementsToDelete: Array<out PsiElement>,
    ): Collection<String>? = null

    @Suppress("DEPRECATION")
    override fun preprocessUsages(
        project: com.intellij.openapi.project.Project,
        usages: Array<out UsageInfo>,
    ): Array<UsageInfo>? = usages.toList().toTypedArray()

    override fun prepareForDeletion(element: PsiElement) {
        // No preparation needed
    }

    override fun isToSearchInComments(element: PsiElement): Boolean = false

    override fun setToSearchInComments(element: PsiElement, enabled: Boolean) {
        // Not supported
    }

    override fun isToSearchForTextOccurrences(element: PsiElement): Boolean = false

    override fun setToSearchForTextOccurrences(element: PsiElement, enabled: Boolean) {
        // Not supported
    }

    /**
     * Extracts all fragment names defined in a file.
     * A file can contain multiple fragment definitions.
     */
    private fun extractFragmentNames(file: PsiFile): List<String> =
        FRAGMENT_PATTERN
            .findAll(file.text)
            .filter { match ->
                // Skip commented lines
                val lineStart = file.text.lastIndexOf('\n', match.range.first) + 1
                val linePrefix = file.text.substring(lineStart, match.range.first).trim()
                !linePrefix.startsWith("#")
            }
            .mapNotNull { it.groupValues.getOrNull(1) }
            .toList()

    companion object {
        private val FRAGMENT_PATTERN = Regex("""[Ff]ragment:\s*(\S+)""")
    }
}

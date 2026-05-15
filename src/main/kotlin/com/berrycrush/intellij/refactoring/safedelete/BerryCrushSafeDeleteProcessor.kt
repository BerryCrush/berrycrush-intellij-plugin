package com.berrycrush.intellij.refactoring.safedelete

import com.berrycrush.intellij.index.IncludeUsageIndex
import com.berrycrush.intellij.language.FragmentFileType
import com.berrycrush.intellij.psi.BerryCrushFile
import com.berrycrush.intellij.psi.BerryCrushFragmentElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.safeDelete.NonCodeUsageSearchInfo
import com.intellij.refactoring.safeDelete.SafeDeleteProcessor
import com.intellij.refactoring.safeDelete.SafeDeleteProcessorDelegate
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.MultiMap

/**
 * Safe delete processor for BerryCrush fragment files and fragment elements.
 *
 * Supports two modes:
 * 1. File-level deletion: Deletes entire .fragment file
 * 2. Element-level deletion: Deletes individual fragment block within a file
 *
 * Checks for usages before allowing deletion:
 * - Warns if fragment is included anywhere
 * - Shows usages in preview dialog
 * - Allows force delete or cancel
 */
class BerryCrushSafeDeleteProcessor : SafeDeleteProcessorDelegate {

    override fun handlesElement(element: PsiElement): Boolean {
        // Handle individual fragment elements (for per-fragment deletion)
        if (element is BerryCrushFragmentElement) {
            return true
        }
        // Handle fragment files
        if (element is PsiFile) {
            return element.virtualFile?.extension == FragmentFileType.EXTENSION
        }
        // Handle BerryCrushFile
        return element is BerryCrushFile &&
            element.virtualFile?.extension == FragmentFileType.EXTENSION
    }

    override fun findUsages(
        element: PsiElement,
        allElementsToDelete: Array<out PsiElement>,
        usages: MutableList<in UsageInfo>,
    ): NonCodeUsageSearchInfo {
        val project = element.project

        // Get fragment names based on element type
        val fragmentNames = when (element) {
            is BerryCrushFragmentElement -> listOfNotNull(element.fragmentName)
            is PsiFile -> extractFragmentNames(element)
            else -> element.containingFile?.let { extractFragmentNames(it) } ?: emptyList()
        }

        // Find usages via our index
        fragmentNames.forEach { fragmentName ->
            IncludeUsageIndex.findIncludeUsages(project, fragmentName)
                .map { UsageInfo(it) }
                .forEach { usages.add(it) }
        }

        // For file-level deletion, also search for generic usages
        if (element is PsiFile || element is BerryCrushFile) {
            SafeDeleteProcessor.findGenericElementUsages(
                element,
                usages,
                allElementsToDelete,
                GlobalSearchScope.projectScope(project),
            )
        }

        return NonCodeUsageSearchInfo(
            SafeDeleteProcessor.getDefaultInsideDeletedCondition(allElementsToDelete),
            listOf(element),
        )
    }

    override fun getElementsToSearch(
        element: PsiElement,
        allElementsToDelete: Collection<PsiElement>,
    ): Collection<PsiElement> = listOf(element)

    override fun getAdditionalElementsToDelete(
        element: PsiElement,
        allElementsToDelete: Collection<PsiElement>,
        askUser: Boolean,
    ): Collection<PsiElement>? = null

    override fun findConflicts(
        element: PsiElement,
        allElementsToDelete: Array<out PsiElement>,
        usages: Array<out UsageInfo>,
        conflicts: MultiMap<PsiElement, String>,
    ) {
        val project = element.project

        // Get fragment names based on element type
        val fragmentNames = when (element) {
            is BerryCrushFragmentElement -> listOfNotNull(element.fragmentName)
            is PsiFile -> extractFragmentNames(element)
            else -> element.containingFile?.let { extractFragmentNames(it) } ?: emptyList()
        }

        // Find usages and report as conflicts
        fragmentNames.forEach { fragmentName ->
            val fragmentUsages = IncludeUsageIndex.findIncludeUsages(project, fragmentName)
            if (fragmentUsages.isNotEmpty()) {
                val usageFiles = fragmentUsages.mapNotNull { it.containingFile?.name }.distinct()
                val usageDescription = if (usageFiles.size <= 3) {
                    usageFiles.joinToString(", ")
                } else {
                    "${usageFiles.take(3).joinToString(", ")} and ${usageFiles.size - 3} more"
                }
                conflicts.putValue(element, "Fragment '$fragmentName' is included in: $usageDescription")
            }
        }
    }

    override fun preprocessUsages(
        project: com.intellij.openapi.project.Project,
        usages: Array<out UsageInfo>,
    ): Array<UsageInfo> = usages.toList().toTypedArray()

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

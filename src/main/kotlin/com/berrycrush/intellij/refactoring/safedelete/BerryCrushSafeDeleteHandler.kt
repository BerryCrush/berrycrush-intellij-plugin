package com.berrycrush.intellij.refactoring.safedelete

import com.berrycrush.intellij.index.IncludeUsageIndex
import com.berrycrush.intellij.language.FragmentFileType
import com.berrycrush.intellij.psi.BerryCrushFile
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.RefactoringActionHandler

/**
 * Safe delete handler for BerryCrush fragment files.
 *
 * Checks for usages before allowing deletion and shows a warning dialog
 * if the fragment is included anywhere.
 */
class BerryCrushSafeDeleteHandler : RefactoringActionHandler {

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?, dataContext: DataContext?) {
        if (file !is BerryCrushFile) return
        if (file.virtualFile?.extension != FragmentFileType.EXTENSION) return
        invokeForFile(project, file)
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        val file = elements.firstOrNull() as? PsiFile ?: return
        invokeForFile(project, file)
    }

    private fun invokeForFile(project: Project, file: PsiFile) {
        val fragmentNames = extractFragmentNames(file)
        val allUsages = fragmentNames.flatMap { name ->
            IncludeUsageIndex.findIncludeUsages(project, name)
        }

        if (allUsages.isNotEmpty()) {
            val message = buildString {
                appendLine("The following fragments are still referenced:")
                fragmentNames.forEach { name ->
                    val usages = IncludeUsageIndex.findIncludeUsages(project, name)
                    if (usages.isNotEmpty()) {
                        appendLine("\n• $name (${usages.size} usages)")
                        usages.take(5).forEach { usage ->
                            val fileName = usage.containingFile?.name ?: "unknown"
                            appendLine("  - $fileName")
                        }
                        if (usages.size > 5) {
                            appendLine("  - ... and ${usages.size - 5} more")
                        }
                    }
                }
                appendLine("\nDelete anyway?")
            }

            val result = Messages.showYesNoDialog(
                project,
                message,
                "Safe Delete",
                "Delete Anyway",
                "Cancel",
                Messages.getWarningIcon(),
            )

            if (result != Messages.YES) return
        }

        // Proceed with deletion
        WriteCommandAction.runWriteCommandAction(project, "Delete Fragment", null, {
            file.virtualFile?.delete(this)
        })
    }

    private fun extractFragmentNames(file: PsiFile): List<String> =
        FRAGMENT_PATTERN
            .findAll(file.text)
            .filter { match ->
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

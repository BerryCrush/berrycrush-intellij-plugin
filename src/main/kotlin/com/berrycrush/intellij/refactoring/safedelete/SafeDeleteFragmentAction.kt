package com.berrycrush.intellij.refactoring.safedelete

import com.berrycrush.intellij.index.IncludeUsageIndex
import com.berrycrush.intellij.language.FragmentFileType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiFile

/**
 * Action for safe deleting BerryCrush fragment files.
 *
 * Checks for usages before allowing deletion and shows a warning dialog
 * if the fragment is included anywhere.
 */
class SafeDeleteFragmentAction : AnAction("Safe Delete Fragment...") {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.PSI_FILE)
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)

        e.presentation.isEnabledAndVisible =
            file != null && virtualFile?.extension == FragmentFileType.EXTENSION
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        if (virtualFile.extension != FragmentFileType.EXTENSION) return

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
                        appendLine("\n• $name (${usages.size} usage${if (usages.size > 1) "s" else ""})")
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
        } else {
            // No usages, confirm deletion
            val result = Messages.showYesNoDialog(
                project,
                "Delete '${virtualFile.name}'?",
                "Delete Fragment",
                Messages.getQuestionIcon(),
            )
            if (result != Messages.YES) return
        }

        // Proceed with deletion
        WriteCommandAction.runWriteCommandAction(project, "Delete Fragment", null, {
            virtualFile.delete(this)
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

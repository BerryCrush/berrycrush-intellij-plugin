package com.berrycrush.intellij.refactoring.safedelete

import com.berrycrush.intellij.index.IncludeUsageIndex
import com.berrycrush.intellij.language.FragmentFileType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

/**
 * Action for deleting individual fragments from a multi-fragment file.
 *
 * Only available when cursor is on a fragment definition line.
 * Checks for usages before deletion and warns if fragment is included anywhere.
 */
class SafeDeleteFragmentAction : AnAction("Delete Fragment...") {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.PSI_FILE)
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)

        // Only available for fragment files with editor open
        if (file == null || virtualFile?.extension != FragmentFileType.EXTENSION || editor == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        // Only available when cursor is on a fragment definition line
        val fragment = findFragmentAtCursor(file, editor)
        e.presentation.isEnabledAndVisible = fragment != null
        if (fragment != null) {
            e.presentation.text = "Delete Fragment '${fragment.name}'..."
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.PSI_FILE) ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return

        val targetFragment = findFragmentAtCursor(file, editor) ?: return
        deleteFragment(project, editor, targetFragment)
    }

    private fun findFragmentAtCursor(file: PsiFile, editor: Editor): FragmentInfo? {
        val offset = editor.caretModel.offset
        val document = editor.document
        val lineNumber = document.getLineNumber(offset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineEnd = document.getLineEndOffset(lineNumber)
        val lineText = document.getText(TextRange(lineStart, lineEnd))

        // Check if cursor is on a fragment definition line
        val match = FRAGMENT_PATTERN.find(lineText) ?: return null
        val fragmentName = match.groupValues[1]

        // Calculate fragment boundaries
        val fragmentBounds = findFragmentBounds(file.text, lineStart)

        return FragmentInfo(fragmentName, fragmentBounds)
    }

    private fun findFragmentBounds(text: String, fragmentStart: Int): TextRange {
        // Find the end of this fragment (next fragment definition or end of file)
        val afterFragment = text.substring(fragmentStart)
        val nextFragmentMatch = FRAGMENT_PATTERN.find(afterFragment, 1)

        val fragmentEnd = if (nextFragmentMatch != null) {
            // Find the line start of the next fragment
            val nextFragmentOffset = fragmentStart + nextFragmentMatch.range.first
            val lastNewline = text.lastIndexOf('\n', nextFragmentOffset - 1)
            if (lastNewline > fragmentStart) lastNewline else nextFragmentOffset
        } else {
            text.length
        }

        return TextRange(fragmentStart, fragmentEnd)
    }

    private fun deleteFragment(project: Project, editor: Editor, fragment: FragmentInfo) {
        val usages = IncludeUsageIndex.findIncludeUsages(project, fragment.name)

        if (usages.isNotEmpty()) {
            val message = buildString {
                appendLine("Fragment '${fragment.name}' is still referenced:")
                usages.take(5).forEach { usage ->
                    val fileName = usage.containingFile?.name ?: "unknown"
                    appendLine("  - $fileName")
                }
                if (usages.size > 5) {
                    appendLine("  - ... and ${usages.size - 5} more")
                }
                appendLine("\nDelete anyway?")
            }

            val result = Messages.showYesNoDialog(
                project,
                message,
                "Delete Fragment",
                "Delete Anyway",
                "Cancel",
                Messages.getWarningIcon(),
            )

            if (result != Messages.YES) return
        } else {
            val result = Messages.showYesNoDialog(
                project,
                "Delete fragment '${fragment.name}'?",
                "Delete Fragment",
                Messages.getQuestionIcon(),
            )
            if (result != Messages.YES) return
        }

        // Remove the fragment from the file
        WriteCommandAction.runWriteCommandAction(project, "Delete Fragment", null, {
            val document = editor.document
            document.deleteString(fragment.bounds.startOffset, fragment.bounds.endOffset)
            PsiDocumentManager.getInstance(project).commitDocument(document)
        })
    }

    private data class FragmentInfo(
        val name: String,
        val bounds: TextRange,
    )

    companion object {
        private val FRAGMENT_PATTERN = Regex("""[Ff]ragment:\s*(\S+)""")
    }
}

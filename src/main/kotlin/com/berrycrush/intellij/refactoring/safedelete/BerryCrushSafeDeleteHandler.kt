package com.berrycrush.intellij.refactoring.safedelete

import com.berrycrush.intellij.language.FragmentFileType
import com.berrycrush.intellij.psi.BerryCrushFile
import com.berrycrush.intellij.psi.BerryCrushFragmentElement
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.RefactoringActionHandler
import com.intellij.refactoring.safeDelete.SafeDeleteHandler

/**
 * Safe delete handler for BerryCrush fragment elements and files.
 *
 * When invoked from the editor, this handler finds the fragment element at the
 * caret position and delegates to IntelliJ's native SafeDeleteHandler.
 *
 * Supports:
 * - Individual fragment elements within a file
 * - Entire fragment files
 */
class BerryCrushSafeDeleteHandler : RefactoringActionHandler {

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?, dataContext: DataContext?) {
        if (editor == null || file !is BerryCrushFile) return

        // Find fragment element at caret position
        val offset = editor.caretModel.offset
        val element = file.findElementAt(offset) ?: return
        val fragmentElement = PsiTreeUtil.getParentOfType(element, BerryCrushFragmentElement::class.java)

        if (fragmentElement != null) {
            // Delegate to IntelliJ's native SafeDeleteHandler for the fragment element
            // Pass true to enable safe delete checking (shows usage preview)
            SafeDeleteHandler.invoke(project, arrayOf(fragmentElement), true)
        } else if (file.virtualFile?.extension == FragmentFileType.EXTENSION) {
            // If not on a fragment element, try to delete the entire file
            SafeDeleteHandler.invoke(project, arrayOf(file), true)
        }
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext?) {
        if (elements.isEmpty()) return

        // Filter to supported elements
        val supportedElements = elements.filter { element ->
            element is BerryCrushFragmentElement ||
                (element is PsiFile && element.virtualFile?.extension == FragmentFileType.EXTENSION)
        }.toTypedArray()

        if (supportedElements.isNotEmpty()) {
            // Pass true to enable safe delete checking (shows usage preview)
            SafeDeleteHandler.invoke(elements[0].project, supportedElements, true)
        }
    }
}

package org.berrycrush.intellij.refactoring

import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil
import org.berrycrush.intellij.language.FragmentFileType
import org.berrycrush.intellij.psi.BerryCrushFile
import org.berrycrush.intellij.psi.BerryCrushFragmentElement
import org.berrycrush.intellij.psi.BerryCrushFragmentRefElement
import org.berrycrush.intellij.psi.BerryCrushReferenceElement
import org.berrycrush.intellij.psi.BerryCrushVariableRefElement

/**
 * Enables refactoring support for BerryCrush scenario and fragment files.
 *
 * Supports:
 * - In-place rename for fragment definitions
 * - In-place rename for variable placeholders
 * - Safe delete for fragment definitions and files
 */
class BerryCrushRefactoringSupportProvider : RefactoringSupportProvider() {
    override fun isMemberInplaceRenameAvailable(
        element: PsiElement,
        context: PsiElement?,
    ): Boolean = element.containingFile is BerryCrushFile && isRenameableElement(element)

    override fun isInplaceRenameAvailable(
        element: PsiElement,
        context: PsiElement?,
    ): Boolean = element.containingFile is BerryCrushFile && isRenameableElement(element)

    override fun isSafeDeleteAvailable(element: PsiElement): Boolean {
        // Safe delete available for individual fragment elements
        if (element is BerryCrushFragmentElement) return true

        // Safe delete available for fragment files
        val file = element.containingFile ?: return false
        return file.virtualFile?.extension == FragmentFileType.EXTENSION
    }

    private fun isRenameableElement(element: PsiElement): Boolean {
        if (element is PsiNameIdentifierOwner) {
            return true
        }

        if (element.reference?.resolve() is PsiNameIdentifierOwner) {
            return true
        }

        val namedParent = PsiTreeUtil.getParentOfType(element, PsiNameIdentifierOwner::class.java)
        if (namedParent != null) {
            return true
        }

        val referenceParent: BerryCrushReferenceElement? = PsiTreeUtil.getParentOfType(
            element,
            BerryCrushVariableRefElement::class.java,
            BerryCrushFragmentRefElement::class.java,
        )

        return referenceParent?.reference?.resolve() is PsiNameIdentifierOwner
    }
}

package com.berrycrush.intellij.navigation

import com.berrycrush.intellij.psi.BerryCrushFile
import com.berrycrush.intellij.psi.BerryCrushFragmentElement
import com.berrycrush.intellij.psi.BerryCrushFragmentRefElement
import com.berrycrush.intellij.psi.BerryCrushVariableRefElement
import com.berrycrush.intellij.reference.BerryCrushParameterReference
import com.berrycrush.intellij.reference.BerryCrushVariableReference
import com.intellij.codeInsight.TargetElementEvaluatorEx2
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.util.PsiTreeUtil

/**
 * Target element evaluator for BerryCrush language.
 *
 * This helps IntelliJ find the correct element at the caret position for
 * refactoring operations like Safe Delete and Rename.
 */
class BerryCrushTargetElementEvaluator : TargetElementEvaluatorEx2() {
    override fun isAcceptableNamedParent(parent: PsiElement): Boolean = parent.containingFile is BerryCrushFile && parent is PsiNameIdentifierOwner

    override fun getNamedElement(element: PsiElement): PsiElement? {
        if (element.containingFile !is BerryCrushFile) {
            return null
        }

        val resolved = element.reference?.resolve()
        if (resolved is PsiNameIdentifierOwner && resolved.containingFile is BerryCrushFile) {
            return resolved
        }

        val referenceOwner = PsiTreeUtil.getParentOfType(
            element,
            BerryCrushVariableRefElement::class.java,
            BerryCrushFragmentRefElement::class.java,
        )
        val resolvedFromReferenceOwner = referenceOwner?.reference?.resolve()
        if (resolvedFromReferenceOwner is PsiNameIdentifierOwner && resolvedFromReferenceOwner.containingFile is BerryCrushFile) {
            return resolvedFromReferenceOwner
        }

        if (referenceOwner is BerryCrushVariableRefElement) {
            val fallbackResolved = if (referenceOwner.isParameterReference) {
                BerryCrushParameterReference.resolve(
                    referenceOwner,
                    referenceOwner.variableName.removePrefix("param.").split('.'),
                )
            } else {
                BerryCrushVariableReference.resolve(referenceOwner, referenceOwner.variableName)
            }

            if (fallbackResolved is PsiNameIdentifierOwner && fallbackResolved.containingFile is BerryCrushFile) {
                return fallbackResolved
            }
        }

        val owner = PsiTreeUtil.getParentOfType(element, PsiNameIdentifierOwner::class.java)
        if (owner != null && owner.containingFile is BerryCrushFile) {
            return owner
        }

        val fragmentElement = PsiTreeUtil.getParentOfType(element, BerryCrushFragmentElement::class.java)
        return fragmentElement
    }
}

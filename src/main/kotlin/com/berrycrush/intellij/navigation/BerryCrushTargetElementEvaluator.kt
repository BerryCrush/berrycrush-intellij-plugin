package com.berrycrush.intellij.navigation

import com.berrycrush.intellij.psi.BerryCrushFragmentElement
import com.intellij.codeInsight.TargetElementEvaluatorEx2
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

/**
 * Target element evaluator for BerryCrush language.
 *
 * This helps IntelliJ find the correct element at the caret position for
 * refactoring operations like Safe Delete and Rename.
 */
class BerryCrushTargetElementEvaluator : TargetElementEvaluatorEx2() {

    override fun isAcceptableNamedParent(parent: PsiElement): Boolean {
        // Accept fragment elements as named parents
        return parent is BerryCrushFragmentElement
    }

    override fun getNamedElement(element: PsiElement): PsiElement? {
        // If we're on a leaf element inside a fragment, return the fragment element
        val fragmentElement = PsiTreeUtil.getParentOfType(element, BerryCrushFragmentElement::class.java)
        if (fragmentElement != null) {
            return fragmentElement
        }
        return null
    }
}

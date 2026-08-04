package com.berrycrush.intellij.inspection

import com.berrycrush.intellij.psi.BerryCrushDirectiveElement
import com.berrycrush.intellij.psi.BerryCrushStepElement
import com.berrycrush.intellij.reference.BerryCrushStepReference
import com.berrycrush.intellij.util.ModuleScopeResolver
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

/**
 * Inspection that detects undefined custom step references.
 *
 * Highlights steps that don't have directives (call/assert/extract/include)
 * and don't match any @Step annotated method in the project.
 */
class UndefinedStepInspection : BerryCrushInspection() {

    override fun getDisplayName(): String = "Undefined custom step"
    override fun getShortName(): String = "BerryCrushUndefinedStep"
    override fun getGroupDisplayName(): String = "BerryCrush"
    override fun isEnabledByDefault(): Boolean = true

    override fun checkFile(file: PsiFile, holder: ProblemsHolder) {
        PsiTreeUtil.findChildrenOfType(file, BerryCrushStepElement::class.java)
            .filter { PsiTreeUtil.findChildrenOfType(it, BerryCrushDirectiveElement::class.java).isEmpty() }
            .filter { step -> !step.hasCustomStep(file) }
            .forEach { step -> step.addWarning(holder)}
    }

    private fun BerryCrushStepElement.hasCustomStep(file: PsiFile): Boolean {
        val stepText = stepText ?: return false
        return BerryCrushStepReference.findMatchingStepMethodsInScope(
            file.project,
            stepText,
            ModuleScopeResolver.getModuleDependencyScope(file)
        ).isNotEmpty()
    }

    private fun BerryCrushStepElement.addWarning(holder: ProblemsHolder) {
        val quickFix = stepText?.let { arrayOf(CreateStepQuickFix(it)) } ?: arrayOf()
        holder.registerProblem(
            this,
            "Step '$stepText' has no matching @Step definition",
            ProblemHighlightType.WEAK_WARNING,
            *quickFix
        )
    }
}

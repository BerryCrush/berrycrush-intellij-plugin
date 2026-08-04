package com.berrycrush.intellij.inspection

import com.berrycrush.intellij.index.FragmentIndex
import com.berrycrush.intellij.psi.BerryCrushIncludeElement
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

/**
 * Inspection that detects missing fragment references in BerryCrush files.
 *
 * Highlights `include fragmentName` directives where the referenced
 * fragment doesn't exist in the project.
 */
class MissingFragmentInspection : BerryCrushInspection() {

    override fun getDisplayName(): String = "Missing fragment reference"
    override fun getShortName(): String = "BerryCrushMissingFragment"
    override fun getGroupDisplayName(): String = "BerryCrush"
    override fun isEnabledByDefault(): Boolean = true

    override fun checkFile(file: PsiFile, holder: ProblemsHolder) {
        val knownFragments = FragmentIndex.getAllFragmentNames(file.project)
        PsiTreeUtil.findChildrenOfType(file, BerryCrushIncludeElement::class.java).forEach { include ->
            val fragmentName = include.fragmentName ?: return@forEach
            if (fragmentName !in knownFragments) {
                    holder.registerProblem(
                        include,
                        "Fragment '$fragmentName' not found",
                        ProblemHighlightType.ERROR,
                        CreateFragmentQuickFix(fragmentName)
                    )
            }
        }
    }
}

package org.berrycrush.intellij.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile

/**
 * Base class for BerryCrush file inspections.
 *
 * Filters to only process .scenario and .fragment files.
 */
abstract class BerryCrushInspection : LocalInspectionTool() {
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
    ): PsiElementVisitor {
        val file = holder.file
        val fileName = file.name

        // Only process BerryCrush files
        if (!fileName.endsWith(".scenario") && !fileName.endsWith(".fragment")) {
            return PsiElementVisitor.EMPTY_VISITOR
        }

        return object : PsiElementVisitor() {
            override fun visitFile(file: PsiFile) {
                checkFile(file, holder)
            }
        }
    }

    /**
     * Check the BerryCrush file for problems.
     * Called once per file during inspection.
     */
    protected abstract fun checkFile(
        file: PsiFile,
        holder: ProblemsHolder,
    )
}

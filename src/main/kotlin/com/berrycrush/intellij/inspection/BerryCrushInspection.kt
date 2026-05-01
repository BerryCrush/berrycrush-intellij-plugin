package com.berrycrush.intellij.inspection

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile

/**
 * Base class for BerryCrush file inspections.
 *
 * Filters to only process .scenario and .fragment files.
 */
abstract class BerryCrushInspection : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
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
    protected abstract fun checkFile(file: PsiFile, holder: ProblemsHolder)

    /**
     * Find a PSI element at a specific line and column.
     */
    protected fun findElementAtLine(file: PsiFile, lineNumber: Int, column: Int = 0): PsiElement? {
        val document = com.intellij.psi.PsiDocumentManager.getInstance(file.project).getDocument(file)
            ?: return null
        
        if (lineNumber >= document.lineCount) return null
        
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val offset = lineStartOffset + column
        
        return file.findElementAt(offset)
    }
}

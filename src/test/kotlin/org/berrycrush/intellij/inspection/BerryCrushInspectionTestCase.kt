package org.berrycrush.intellij.inspection

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiFile
import org.berrycrush.intellij.BerryCrushTestCase

abstract class BerryCrushInspectionTestCase(
    val inspection: BerryCrushInspection,
) : BerryCrushTestCase() {
    fun runInspection(file: PsiFile): List<ProblemDescriptor> {
        val manager = InspectionManager.getInstance(project)
        val holder = ProblemsHolder(manager, file, false)
        val visitor = inspection.buildVisitor(holder, false)
        visitor.visitFile(file)
        return holder.results
    }
}

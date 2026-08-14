package org.berrycrush.intellij.inspection

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.berrycrush.intellij.psi.BerryCrushOperationRefElement
import org.berrycrush.intellij.reference.BerryCrushOperationReference

/**
 * Inspection that detects undefined OpenAPI operation references.
 *
 * Highlights `call ^operationId` directives where the operation ID
 * doesn't exist in any OpenAPI specification in the project.
 */
class UndefinedOperationInspection : BerryCrushInspection() {
    override fun getDisplayName(): String = "Undefined OpenAPI operation"

    override fun getShortName(): String = "BerryCrushUndefinedOperation"

    override fun getGroupDisplayName(): String = "BerryCrush"

    override fun isEnabledByDefault(): Boolean = true

    override fun checkFile(
        file: PsiFile,
        holder: ProblemsHolder,
    ) {
        val project = file.project
        val knownOperations = BerryCrushOperationReference.findAllOperationIds(project).toSet()

        // Skip if no OpenAPI specs found
        if (knownOperations.isEmpty() && BerryCrushOperationReference.findOpenAPIFiles(project).isEmpty()) {
            return
        }
        val refs = PsiTreeUtil.findChildrenOfType(file, BerryCrushOperationRefElement::class.java)
        refs.forEach { ref ->
            val operationId = ref.operationId
            if (operationId !in knownOperations) {
                holder.registerProblem(
                    ref,
                    "Operation '$operationId' not found in OpenAPI specs",
                    ProblemHighlightType.WARNING,
                )
            }
        }
    }
}

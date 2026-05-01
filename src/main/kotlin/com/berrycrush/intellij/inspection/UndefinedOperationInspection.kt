package com.berrycrush.intellij.inspection

import com.berrycrush.intellij.reference.BerryCrushOperationReference
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiFile

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

    override fun checkFile(file: PsiFile, holder: ProblemsHolder) {
        val project = file.project
        val knownOperations = BerryCrushOperationReference.findAllOperationIds(project).toSet()
        
        // Skip if no OpenAPI specs found
        if (knownOperations.isEmpty() && BerryCrushOperationReference.findOpenAPIFiles(project).isEmpty()) {
            return
        }
        
        val lines = file.text.lines()
        
        lines.forEachIndexed { lineIndex, line ->
            CALL_PATTERN.find(line)?.let { match ->
                val operationId = match.groupValues[1]
                if (operationId !in knownOperations) {
                    findElementAtLine(file, lineIndex, match.range.first)?.let { element ->
                        holder.registerProblem(
                            element,
                            "Operation '$operationId' not found in OpenAPI specs",
                            ProblemHighlightType.WARNING
                        )
                    }
                }
            }
        }
    }

    companion object {
        private val CALL_PATTERN = Regex("""call\s+\^(\w+)""")
    }
}

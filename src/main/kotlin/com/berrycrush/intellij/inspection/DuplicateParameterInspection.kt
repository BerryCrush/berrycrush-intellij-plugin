package com.berrycrush.intellij.inspection

import com.berrycrush.intellij.psi.BerryCrushIncludeLikeElement
import com.berrycrush.intellij.psi.BerryCrushParameterEntryElement
import com.berrycrush.intellij.util.lineNumber
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

/**
 * Inspection that detects duplicate parameters in parameterized fragment includes.
 *
 * Reports an error when the same parameter name is specified multiple times
 * in a single include directive.
 */
class DuplicateParameterInspection : BerryCrushInspection() {

    override fun getDisplayName(): String = "Duplicate fragment parameter"
    override fun getShortName(): String = "BerryCrushDuplicateParameter"
    override fun getGroupDisplayName(): String = "BerryCrush"
    override fun isEnabledByDefault(): Boolean = true

    override fun checkFile(file: PsiFile, holder: ProblemsHolder) {
        PsiTreeUtil.findChildrenOfType(file, BerryCrushIncludeLikeElement::class.java).forEach { includelike ->
            includelike.parameters?.let { parameters ->
                val seen = mutableMapOf<String, BerryCrushParameterEntryElement>()
                parameters.entries.forEach { entry ->
                    entry.parameterName?.let { name ->
                        val prev = seen.compute(name) { _, v ->
                            v ?: entry
                        }
                        if (prev != entry) {
                            val lineNumber = prev?.textRange?.let { file.lineNumber(it) }
                            holder.registerProblem(
                                entry,
                                "Duplicate parameter '$name' (first defined on line ${lineNumber?.first ?: "n/a"})",
                                ProblemHighlightType.ERROR
                            )
                        }
                    }
                }
            }
        }
    }
}

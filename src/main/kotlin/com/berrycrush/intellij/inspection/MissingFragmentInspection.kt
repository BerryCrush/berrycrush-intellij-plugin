package com.berrycrush.intellij.inspection

import com.berrycrush.intellij.index.FragmentIndex
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiFile

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
        val lines = file.text.lines()
        
        lines.forEachIndexed { lineIndex, line ->
            INCLUDE_PATTERN.find(line)?.let { match ->
                val fragmentName = match.groupValues[1]
                if (fragmentName !in knownFragments) {
                    findElementAtLine(file, lineIndex, match.range.first)?.let { element ->
                        holder.registerProblem(
                            element,
                            "Fragment '$fragmentName' not found",
                            ProblemHighlightType.ERROR,
                            CreateFragmentQuickFix(fragmentName)
                        )
                    }
                }
            }
        }
    }

    companion object {
        private val INCLUDE_PATTERN = Regex("""^\s*include\s+(\S+)""")
    }
}

package com.berrycrush.intellij.navigation

import com.berrycrush.intellij.reference.BerryCrushFragmentReference
import com.berrycrush.intellij.reference.BerryCrushOperationReference
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement

/**
 * Handles Go to Declaration (Cmd+Click) for BerryCrush elements.
 */
class BerryCrushGotoDeclarationHandler : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor?
    ): Array<PsiElement>? {
        if (sourceElement == null) return null

        // Check file type
        val file = sourceElement.containingFile ?: return null
        val fileName = file.name
        if (!fileName.endsWith(".scenario") && !fileName.endsWith(".fragment")) {
            return null
        }

        val text = sourceElement.text

        // Handle operation references (^operationId)
        if (text.startsWith("^") && text.length > 1) {
            val operationId = text.removePrefix("^")
            if (operationId.matches(Regex("[a-zA-Z_]\\w*"))) {
                val target = BerryCrushOperationReference.findOperationInOpenAPI(
                    sourceElement.project,
                    operationId
                )
                if (target != null) {
                    return arrayOf(target)
                }
            }
        }

        // Check if we're in an include directive context
        if (isInIncludeContext(sourceElement)) {
            val fragmentName = text.removePrefix("^")
            if (fragmentName.matches(Regex("[a-zA-Z_][a-zA-Z0-9_.\\-]*"))) {
                val target = BerryCrushFragmentReference.findFragmentByName(
                    sourceElement.project,
                    fragmentName
                )
                if (target != null) {
                    return arrayOf(target)
                }
            }
        }

        // Check if the text itself looks like a fragment name (for direct include clicks)
        if (text.matches(Regex("[a-zA-Z_][a-zA-Z0-9_.\\-]*"))) {
            // Look for this as a potential fragment name
            val target = BerryCrushFragmentReference.findFragmentByName(
                sourceElement.project,
                text
            )
            if (target != null) {
                return arrayOf(target)
            }
        }

        return null
    }

    /**
     * Check if the element is inside or near an include directive.
     */
    private fun isInIncludeContext(element: PsiElement): Boolean {
        // Check current element
        if (element.text.trim().lowercase() == "include") {
            return false // The word "include" itself, not the target
        }

        // Check parent chain for "include" pattern
        var current: PsiElement? = element.parent
        var depth = 0
        while (current != null && depth < 5) {
            val text = current.text.trim()
            if (text.lowercase().startsWith("include ") || text.lowercase().startsWith("include\t")) {
                return true
            }
            // Check siblings
            var prevSibling = element.prevSibling
            while (prevSibling != null) {
                val sibText = prevSibling.text.trim().lowercase()
                if (sibText == "include") {
                    return true
                }
                prevSibling = prevSibling.prevSibling
            }
            current = current.parent
            depth++
        }
        return false
    }

    override fun getActionText(context: DataContext): String? = null
}

package com.berrycrush.intellij.reference

import com.berrycrush.intellij.psi.BerryCrushElementTypes
import com.berrycrush.intellij.psi.BerryCrushParameterEntryElement
import com.berrycrush.intellij.psi.BerryCrushVariableInterpolationElement
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.util.PsiTreeUtil

/**
 * Reference from variable interpolation (${env.VAR}, ${context.var}, ${param.name})
 * to the variable definition.
 *
 * - ${env.VAR} - environment variables (not resolvable in IDE)
 * - ${context.var} or ${var} - context variables from extract directives
 * - ${param.name} - parameter references from parameters blocks
 */
class BerryCrushVariableInterpolationReference(
    element: PsiElement,
    textRange: TextRange,
    private val variableName: String,
    private val refType: BerryCrushVariableInterpolationElement.RefType
) : PsiReferenceBase<PsiElement>(element, textRange, true) {

    override fun resolve(): PsiElement? {
        return when (refType) {
            BerryCrushVariableInterpolationElement.RefType.PARAM -> {
                findParameterDefinition(element.containingFile, variableName)
            }
            BerryCrushVariableInterpolationElement.RefType.CONTEXT,
            BerryCrushVariableInterpolationElement.RefType.SHORTHAND -> {
                findExtractDefinition(element.containingFile, variableName)
            }
            BerryCrushVariableInterpolationElement.RefType.ENV -> {
                // Environment variables can't be resolved in IDE
                null
            }
        }
    }

    override fun getVariants(): Array<Any> {
        val file = element.containingFile ?: return emptyArray()

        return when (refType) {
            BerryCrushVariableInterpolationElement.RefType.PARAM -> {
                findAllParameterNames(file).toTypedArray()
            }
            BerryCrushVariableInterpolationElement.RefType.CONTEXT,
            BerryCrushVariableInterpolationElement.RefType.SHORTHAND -> {
                findAllExtractedVariables(file).toTypedArray()
            }
            BerryCrushVariableInterpolationElement.RefType.ENV -> {
                // Environment variables - no variants
                emptyArray()
            }
        }
    }

    companion object {
        /**
         * Find a parameter definition by name in the file.
         */
        fun findParameterDefinition(file: PsiFile, name: String): PsiElement? {
            return PsiTreeUtil.findChildrenOfType(file, BerryCrushParameterEntryElement::class.java)
                .firstOrNull { it.parameterName == name }
        }

        /**
         * Find an extract directive that defines the given variable.
         */
        fun findExtractDefinition(file: PsiFile, name: String): PsiElement? {
            // Look for "extract varName = ..." pattern
            val text = file.text
            val pattern = Regex("""extract\s+($name)\s*=""")
            val match = pattern.find(text) ?: return null

            // Find the PSI element at that position
            val offset = match.range.first
            return file.findElementAt(offset)
        }

        /**
         * Find all parameter names in the file.
         */
        fun findAllParameterNames(file: PsiFile): Set<String> {
            return PsiTreeUtil.findChildrenOfType(file, BerryCrushParameterEntryElement::class.java)
                .mapNotNull { it.parameterName }
                .toSet()
        }

        /**
         * Find all extracted variable names in the file.
         */
        fun findAllExtractedVariables(file: PsiFile): Set<String> {
            val text = file.text
            val pattern = Regex("""extract\s+(\w+)\s*=""")
            return pattern.findAll(text)
                .map { it.groupValues[1] }
                .toSet()
        }
    }
}

package com.berrycrush.intellij.refactoring.variable

import com.berrycrush.intellij.psi.BerryCrushExampleHeaderElement
import com.berrycrush.intellij.psi.BerryCrushExtractElement
import com.berrycrush.intellij.psi.BerryCrushFile
import com.berrycrush.intellij.psi.BerryCrushNamedBlockElement
import com.berrycrush.intellij.psi.BerryCrushParameterEntryElement
import com.berrycrush.intellij.psi.BerryCrushScenarioLikeElement
import com.berrycrush.intellij.psi.BerryCrushVariableRefElement
import com.berrycrush.intellij.reference.BerryCrushParameterReference
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.usageView.UsageInfo

/**
 * Handles PSI-based rename for variable-like declarations and references:
 * - extract declarations (`extract ... => name`) and `{{name}}` usages
 * - parameter entries and `{{param.name}}` usages
 * - example headers and linked variable usages in outlines
 */
class VariableRenameProcessor : RenamePsiElementProcessor() {
    override fun substituteElementToRename(element: PsiElement, editor: Editor?): PsiElement? = resolveRenameTarget(element) ?: element

    override fun canProcessElement(element: PsiElement): Boolean {
        if (element.containingFile !is BerryCrushFile) return false

        return resolveRenameTarget(element) != null ||
            element is BerryCrushVariableRefElement ||
            PsiTreeUtil.getParentOfType(element, BerryCrushVariableRefElement::class.java) != null
    }

    override fun prepareRenaming(
        element: PsiElement,
        newName: String,
        allRenames: MutableMap<PsiElement, String>,
        scope: SearchScope,
    ) {
        val target = resolveRenameTarget(element) ?: return
        if (target != element) {
            allRenames[target] = newName
        }
    }

    override fun renameElement(
        element: PsiElement,
        newName: String,
        usages: Array<out UsageInfo>,
        listener: RefactoringElementListener?,
    ) {
        val parameterEntry = element as? BerryCrushParameterEntryElement
        val oldParameterName = parameterEntry?.parameterName
        val parameterReferences = if (!oldParameterName.isNullOrBlank()) {
            collectParameterReferencePointers(parameterEntry, oldParameterName)
        } else {
            emptyList()
        }

        super.renameElement(element, newName, usages, listener)

        if (parameterReferences.isNotEmpty()) {
            parameterReferences.forEach { pointer ->
                val referenceElement = pointer.element ?: return@forEach
                if (!referenceElement.isValid) return@forEach

                val currentName = referenceElement.variableName.removePrefix("param.").substringBefore('.')
                if (currentName == newName) return@forEach

                (referenceElement.reference as? BerryCrushParameterReference)?.handleElementRename(newName)
            }
        }
    }

    private fun collectParameterReferencePointers(
        parameterEntry: BerryCrushParameterEntryElement,
        parameterName: String,
    ): List<SmartPsiElementPointer<BerryCrushVariableRefElement>> {
        val namedBlock = PsiTreeUtil.getParentOfType(parameterEntry, BerryCrushNamedBlockElement::class.java)
            ?: return emptyList()

        val pointerManager = SmartPointerManager.getInstance(parameterEntry.project)
        return PsiTreeUtil.findChildrenOfType(namedBlock, BerryCrushVariableRefElement::class.java)
            .asSequence()
            .filter { it.isParameterReference }
            .filter { it.variableName.removePrefix("param.").substringBefore('.') == parameterName }
            .map { pointerManager.createSmartPsiElementPointer(it) }
            .toList()
    }

    private fun resolveRenameTarget(element: PsiElement): PsiElement? {
        val declarationTarget = resolveDeclarationTarget(element)
        if (declarationTarget != null) {
            return declarationTarget
        }

        val variableRef = PsiTreeUtil.getParentOfType(element, BerryCrushVariableRefElement::class.java)
            ?: return null

        val resolvedReferenceTarget = variableRef.reference?.resolve()
        if (resolvedReferenceTarget is BerryCrushExtractElement ||
            resolvedReferenceTarget is BerryCrushParameterEntryElement ||
            resolvedReferenceTarget is BerryCrushExampleHeaderElement
        ) {
            return resolvedReferenceTarget
        }

        val variableName = variableRef.variableName

        if (variableRef.isParameterReference) {
            val parameterName = variableName.removePrefix("param.").substringBefore('.')
            val scope = PsiTreeUtil.getParentOfType(variableRef, BerryCrushNamedBlockElement::class.java)
            return scope?.parameter?.entries?.firstOrNull { it.parameterName == parameterName }
        }

        val scenario = PsiTreeUtil.getParentOfType(variableRef, BerryCrushScenarioLikeElement::class.java)
            ?: return null

        val extractDeclaration = PsiTreeUtil.findChildrenOfType(scenario, BerryCrushExtractElement::class.java)
            .firstOrNull { it.extractName == variableName }
        if (extractDeclaration != null) {
            return extractDeclaration
        }

        val exampleHeader = PsiTreeUtil.findChildrenOfType(scenario, BerryCrushExampleHeaderElement::class.java)
            .firstOrNull { it.name == variableName }

        if (exampleHeader != null) {
            return exampleHeader
        }

        return variableRef
    }

    private fun resolveDeclarationTarget(element: PsiElement): PsiElement? {
        if (element is BerryCrushExtractElement ||
            element is BerryCrushParameterEntryElement ||
            element is BerryCrushExampleHeaderElement
        ) {
            return element
        }

        val extract = PsiTreeUtil.getParentOfType(element, BerryCrushExtractElement::class.java)
        if (extract != null) {
            return extract
        }

        val parameter = PsiTreeUtil.getParentOfType(element, BerryCrushParameterEntryElement::class.java)
        if (parameter != null) {
            return parameter
        }

        return PsiTreeUtil.getParentOfType(element, BerryCrushExampleHeaderElement::class.java)
    }
}

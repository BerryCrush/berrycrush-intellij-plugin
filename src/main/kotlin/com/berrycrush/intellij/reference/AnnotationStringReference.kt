package com.berrycrush.intellij.reference

import com.berrycrush.intellij.index.StepUsageIndex
import com.berrycrush.intellij.util.ModuleScopeResolver
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult

/**
 * Reference from @Step/@Assertion annotation string parameter to usages in scenario files.
 *
 * Enables Cmd+Click navigation from the annotation string to the scenario files
 * where the step/assertion is used.
 *
 * Example:
 * ```kotlin
 * @Step("get pet by {name}")  // Cmd+Click navigates to scenario files using this step
 * fun getPetByName(name: String) { ... }
 * ```
 */
class AnnotationStringReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val pattern: String,
    private val isAssertion: Boolean
) : PsiReferenceBase<PsiElement>(element, rangeInElement), PsiPolyVariantReference {

    override fun resolve(): PsiElement? {
        val results = multiResolve(false)
        return results.firstOrNull()?.element
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val project = element.project
        // Use dependent modules scope: find usages in modules that depend on this one
        val scope = ModuleScopeResolver.getDependentModulesScope(element)
        
        val usages = if (isAssertion) {
            StepUsageIndex.findAssertionUsagesInScope(project, pattern, scope)
        } else {
            StepUsageIndex.findStepUsagesInScope(project, pattern, scope)
        }
        
        return usages.map { PsiElementResolveResult(it) }.toTypedArray()
    }

    override fun getVariants(): Array<Any> {
        // No completion variants for annotation strings
        return emptyArray()
    }
}

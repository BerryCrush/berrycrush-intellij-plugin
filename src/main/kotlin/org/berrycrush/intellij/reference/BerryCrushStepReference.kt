package org.berrycrush.intellij.reference

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import org.berrycrush.intellij.util.AnnotationReference
import org.berrycrush.intellij.util.ModuleScopeResolver

/**
 * Reference to @Step annotated methods in Java/Kotlin classes.
 *
 * Enables navigation from step text in .scenario/.fragment files to
 * the corresponding @Step annotated method definitions.
 *
 * Uses module-scoped search to only show @Step methods that are in the
 * scenario file's classpath (compile-time dependencies).
 */
class BerryCrushStepReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val stepText: String,
) : PsiReferenceBase<PsiElement>(element, rangeInElement),
    PsiPolyVariantReference {
    override fun resolve(): PsiElement? {
        val results = multiResolve(false)
        return results.firstOrNull()?.element
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val project = element.project
        // Use module-scoped search: only find @Step methods in this scenario's classpath
        val scope = ModuleScopeResolver.getModuleDependencyScope(element)
        val matchingMethods = findMatchingStepMethodsInScope(project, stepText, scope)
        return matchingMethods.map { PsiElementResolveResult(it) }.toTypedArray()
    }

    override fun getVariants(): Array<Any> {
        // Return all step patterns for completion (use module scope for better suggestions)
        val project = element.project
        val scope = ModuleScopeResolver.getModuleDependencyScope(element)
        return getAllStepPatternsInScope(project, scope).toTypedArray()
    }

    companion object {
        private const val STEP_ANNOTATION_FQN = "org.berrycrush.step.Step"

        /**
         * Finds all @Step annotated methods that match the given step text within the given scope.
         *
         * @param project The project
         * @param stepText The step text to match
         * @param scope The search scope (typically module dependencies)
         * @return List of matching @Step methods
         */
        fun findMatchingStepMethodsInScope(
            project: Project,
            stepText: String,
            scope: GlobalSearchScope,
        ): List<PsiMethod> {
            val stepAnnotationClass = findStepAnnotationClass(project) ?: return emptyList()
            val methods = AnnotatedElementsSearch.searchPsiMethods(stepAnnotationClass, scope)

            return methods
                .filter { method ->
                    val pattern = getStepPattern(method)
                    pattern != null && AnnotationReference.matchesPattern(stepText, pattern)
                }.toList()
        }

        /**
         * Finds all @Step annotated methods that match the given step text.
         * Searches the entire project (backward compatibility).
         */
        fun findMatchingStepMethods(
            project: Project,
            stepText: String,
        ): List<PsiMethod> {
            val scope = GlobalSearchScope.allScope(project)
            return findMatchingStepMethodsInScope(project, stepText, scope)
        }

        /**
         * Gets all @Step annotated methods within the given scope.
         */
        fun getAllStepMethodsInScope(
            project: Project,
            scope: GlobalSearchScope,
        ): List<PsiMethod> {
            val stepAnnotationClass = findStepAnnotationClass(project) ?: return emptyList()
            return AnnotatedElementsSearch.searchPsiMethods(stepAnnotationClass, scope).toList()
        }

        /**
         * Gets all step patterns within the given scope.
         */
        fun getAllStepPatternsInScope(
            project: Project,
            scope: GlobalSearchScope,
        ): List<String> = getAllStepMethodsInScope(project, scope).mapNotNull { getStepPattern(it) }

        /**
         * Finds the Step annotation class in the project.
         */
        private fun findStepAnnotationClass(project: Project): PsiClass? = AnnotationReference.findAnnotationClass(project, STEP_ANNOTATION_FQN)

        /**
         * Gets the pattern value from a @Step annotation on a method.
         */
        private fun getStepPattern(method: PsiMethod): String? = AnnotationReference.getAnnotationPattern(method, STEP_ANNOTATION_FQN)
    }
}

package com.berrycrush.intellij.reference

import com.berrycrush.intellij.util.AnnotationReference
import com.berrycrush.intellij.util.ModuleScopeResolver
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

/**
 * Reference to @Assertion annotated methods in Java/Kotlin classes.
 *
 * Enables navigation from assertion text in .scenario/.fragment files to
 * the corresponding @Assertion annotated method definitions.
 *
 * Uses module-scoped search to only show @Assertion methods that are in the
 * scenario file's classpath (compile-time dependencies).
 */
class BerryCrushAssertionReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val assertionText: String,
) : PsiReferenceBase<PsiElement>(element, rangeInElement),
    PsiPolyVariantReference {
    override fun resolve(): PsiElement? {
        val results = multiResolve(false)
        return results.firstOrNull()?.element
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val project = element.project
        // Use module-scoped search: only find @Assertion methods in this scenario's classpath
        val scope = ModuleScopeResolver.getModuleDependencyScope(element)
        val matchingMethods = findMatchingAssertionMethodsInScope(project, assertionText, scope)
        return matchingMethods.map { PsiElementResolveResult(it) }.toTypedArray()
    }

    override fun getVariants(): Array<Any> {
        // Return all assertion patterns for completion (use module scope for better suggestions)
        val project = element.project
        val scope = ModuleScopeResolver.getModuleDependencyScope(element)
        return getAllAssertionPatternsInScope(project, scope).toTypedArray()
    }

    companion object {
        private const val ASSERTION_ANNOTATION_FQN = "org.berrycrush.assertion.Assertion"

        /**
         * Finds all @Assertion annotated methods that match the given text within the given scope.
         *
         * @param project The project
         * @param assertionText The assertion text to match
         * @param scope The search scope (typically module dependencies)
         * @return List of matching @Assertion methods
         */
        fun findMatchingAssertionMethodsInScope(
            project: Project,
            assertionText: String,
            scope: GlobalSearchScope,
        ): List<PsiMethod> {
            val assertionAnnotationClass = findAssertionAnnotationClass(project) ?: return emptyList()
            val methods = AnnotatedElementsSearch.searchPsiMethods(assertionAnnotationClass, scope)

            return methods
                .filter { method ->
                    val pattern = getAssertionPattern(method)
                    pattern != null && AnnotationReference.matchesPattern(assertionText, pattern)
                }.toList()
        }

        /**
         * Finds all @Assertion annotated methods that match the given text.
         * Searches the entire project (backward compatibility).
         */
        fun findMatchingAssertionMethods(
            project: Project,
            assertionText: String,
        ): List<PsiMethod> {
            val scope = GlobalSearchScope.allScope(project)
            return findMatchingAssertionMethodsInScope(project, assertionText, scope)
        }

        /**
         * Gets all @Assertion annotated methods within the given scope.
         */
        fun getAllAssertionMethodsInScope(
            project: Project,
            scope: GlobalSearchScope,
        ): List<PsiMethod> {
            val assertionAnnotationClass = findAssertionAnnotationClass(project) ?: return emptyList()
            return AnnotatedElementsSearch.searchPsiMethods(assertionAnnotationClass, scope).toList()
        }

        /**
         * Gets all @Assertion annotated methods in the project.
         */
        fun getAllAssertionMethods(project: Project): List<PsiMethod> {
            val scope = GlobalSearchScope.allScope(project)
            return getAllAssertionMethodsInScope(project, scope)
        }

        /**
         * Gets all assertion patterns within the given scope.
         */
        fun getAllAssertionPatternsInScope(
            project: Project,
            scope: GlobalSearchScope,
        ): List<String> = getAllAssertionMethodsInScope(project, scope).mapNotNull { getAssertionPattern(it) }

        /**
         * Finds the Assertion annotation class in the project.
         */
        private fun findAssertionAnnotationClass(project: Project): PsiClass? = AnnotationReference.findAnnotationClass(project, ASSERTION_ANNOTATION_FQN)

        /**
         * Gets the pattern value from an @Assertion annotation on a method.
         */
        private fun getAssertionPattern(method: PsiMethod): String? = AnnotationReference.getAnnotationPattern(method, ASSERTION_ANNOTATION_FQN)
    }
}

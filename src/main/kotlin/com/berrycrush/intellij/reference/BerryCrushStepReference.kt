package com.berrycrush.intellij.reference

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch

/**
 * Reference to @Step annotated methods in Java/Kotlin classes.
 *
 * Enables navigation from step text in .scenario/.fragment files to
 * the corresponding @Step annotated method definitions.
 */
class BerryCrushStepReference(
    element: PsiElement,
    rangeInElement: TextRange,
    private val stepText: String
) : PsiReferenceBase<PsiElement>(element, rangeInElement), PsiPolyVariantReference {

    override fun resolve(): PsiElement? {
        val results = multiResolve(false)
        return results.firstOrNull()?.element
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val project = element.project
        val matchingMethods = findMatchingStepMethods(project, stepText)
        return matchingMethods.map { PsiElementResolveResult(it) }.toTypedArray()
    }

    override fun getVariants(): Array<Any> {
        // Return all step patterns for completion
        val project = element.project
        return getAllStepPatterns(project).toTypedArray()
    }

    companion object {
        private const val STEP_ANNOTATION_FQN = "org.berrycrush.step.Step"

        /**
         * Finds all @Step annotated methods that match the given step text.
         */
        fun findMatchingStepMethods(project: Project, stepText: String): List<PsiMethod> {
            val stepAnnotationClass = findStepAnnotationClass(project) ?: return emptyList()
            val scope = GlobalSearchScope.allScope(project)
            val methods = AnnotatedElementsSearch.searchPsiMethods(stepAnnotationClass, scope)

            return methods.filter { method ->
                val pattern = getStepPattern(method)
                pattern != null && matchesPattern(stepText, pattern)
            }.toList()
        }

        /**
         * Gets all @Step annotated methods in the project.
         */
        fun getAllStepMethods(project: Project): List<PsiMethod> {
            val stepAnnotationClass = findStepAnnotationClass(project) ?: return emptyList()
            val scope = GlobalSearchScope.allScope(project)
            return AnnotatedElementsSearch.searchPsiMethods(stepAnnotationClass, scope).toList()
        }

        /**
         * Gets all step patterns defined in the project.
         */
        fun getAllStepPatterns(project: Project): List<String> {
            return getAllStepMethods(project).mapNotNull { getStepPattern(it) }
        }

        /**
         * Finds the Step annotation class in the project.
         */
        private fun findStepAnnotationClass(project: Project): com.intellij.psi.PsiClass? {
            val javaPsiFacade = JavaPsiFacade.getInstance(project)
            val scope = GlobalSearchScope.allScope(project)
            return javaPsiFacade.findClass(STEP_ANNOTATION_FQN, scope)
        }

        /**
         * Gets the pattern value from a @Step annotation on a method.
         */
        private fun getStepPattern(method: PsiMethod): String? {
            val annotation = method.getAnnotation(STEP_ANNOTATION_FQN) ?: return null
            return getAnnotationStringValue(annotation, "pattern")
                ?: getAnnotationStringValue(annotation, "value")
        }

        /**
         * Extracts a string attribute value from an annotation.
         */
        private fun getAnnotationStringValue(annotation: PsiAnnotation, attributeName: String): String? {
            val attributeValue = annotation.findAttributeValue(attributeName) ?: return null
            val text = attributeValue.text
            // Remove surrounding quotes if present
            return if (text.startsWith("\"") && text.endsWith("\"") && text.length >= 2) {
                text.substring(1, text.length - 1)
            } else {
                text
            }
        }

        /**
         * Checks if the step text matches the pattern.
         *
         * Patterns support placeholders like {int}, {string}, {word}, etc.
         */
        private fun matchesPattern(stepText: String, pattern: String): Boolean {
            // Convert pattern placeholders to regex
            val regexPattern = pattern
                .replace(Regex("""\{int\}"""), """(-?\d+)""")
                .replace(Regex("""\{string\}"""), """("[^"]*"|'[^']*')""")
                .replace(Regex("""\{word\}"""), """(\w+)""")
                .replace(Regex("""\{float\}"""), """(-?\d+\.?\d*)""")
                .replace(Regex("""\{any\}"""), """(.+?)""")
                .let { "^$it$" }

            return try {
                Regex(regexPattern, RegexOption.IGNORE_CASE).matches(stepText)
            } catch (e: Exception) {
                // If regex compilation fails, fall back to simple contains check
                stepText.contains(pattern, ignoreCase = true)
            }
        }
    }
}
